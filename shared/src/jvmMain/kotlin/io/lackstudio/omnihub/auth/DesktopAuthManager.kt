package io.lackstudio.omnihub.auth

import co.touchlab.kermit.Logger
import io.lackstudio.omnihub.shared.BuildKonfig
import kotlinx.coroutines.CompletableDeferred
import java.awt.Desktop
import java.awt.Taskbar
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URI
import java.net.URLEncoder
import java.util.UUID
import kotlin.concurrent.thread

class DesktopAuthManager : AuthManager {

    private val logger = Logger.withTag("DesktopAuthManager")
    private val callbackPort = 54321
    private var resultDeferred: CompletableDeferred<GoogleAuthTokens?>? = null

    // Desktop uses a fixed URL: http://localhost:54321/callback
    override fun getRedirectUrl(): String {
        return "http://localhost:$callbackPort/callback"
    }

    override fun startLogin(authUrl: String) {
        // Start a thread to run the Server (to avoid blocking the UI)
        thread {
            try {
                // Simple HTTP Server
                ServerSocket(callbackPort).use { serverSocket ->
                    logger.d { "Desktop Auth Server listening on port $callbackPort..." }

                    // Open the system browser and go to the OAuth2 login page
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI(authUrl))
                    }

                    var authenticated = false
                    // Loop to handle potential redirects (e.g., fragment to query for Google)
                    while (!authenticated) {
                        // Wait for browser redirect (Blocking)
                        serverSocket.accept().use { clientSocket ->
                            // Read Request
                            val reader = clientSocket.getInputStream().bufferedReader()
                            val requestLine = reader.readLine() // e.g., "GET /callback?code=XYZ... HTTP/1.1"

                            if (requestLine != null) {
                                if (requestLine.contains("code=")) {
                                    val code = requestLine.substringAfter("code=").substringBefore("&").substringBefore(" ")
                                    logger.i { "Desktop received Auth Code (masked): ${code.take(4)}***" }

                                    // Push into DeepLinkBuffer
                                    // To bypass the ViewModel's parsing logic, we disguise it as a Deep Link format
                                    DeepLinkBuffer.setDeepLink("omnihub://auth/callback?code=$code")
                                    authenticated = true
                                } else if (requestLine.contains("id_token=")) {
                                    // Precisely extract id_token, excluding subsequent & parameters or spaces
                                    val idToken = requestLine.substringAfter("id_token=").substringBefore("&").substringBefore(" ")
                                    logger.i { "Desktop received Google ID Token" }
                                    
                                    val tokens = GoogleAuthTokens(idToken = idToken)
                                    resultDeferred?.complete(tokens)

                                    // Push into DeepLinkBuffer
                                    // To bypass the ViewModel's parsing logic, we disguise it as a Deep Link format
                                    DeepLinkBuffer.setDeepLink("omnihub://auth/callback?idToken=$idToken")
                                    authenticated = true
                                }
                            }

                            // Return HTML to the browser (to reassure the user)
                            val writer = PrintWriter(clientSocket.getOutputStream())
                            writer.println("HTTP/1.1 200 OK")
                            writer.println("Content-Type: text/html; charset=UTF-8")
                            writer.println("\r\n")

                            if (authenticated) {
                                writer.print(loadSuccessHtml())
                            } else {
                                // If token not found in query, try extracting from URL fragment (Google case)
                                writer.print("<html><script>if(window.location.hash){window.location.search=window.location.hash.substring(1);}else{document.body.innerHTML='<h1>Login Failed</h1><p>No authorization code or token found.</p>';}</script><body><p>Processing login...</p></body></html>")
                            }
                            writer.flush()
                        }
                    }
                }

                try {
                    if (Taskbar.isTaskbarSupported() &&
                        Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION)) {
                        // This will make the Dock icon bounce until the user clicks the App
                        Taskbar.getTaskbar().requestUserAttention(true, true)
                    }
                } catch (e: Exception) {
                    // Ignore unsupported platform errors
                    logger.e { "Exception error message: ${e.message}" }
                }

            } catch (e: Exception) {
                logger.e(throwable = e) { "Error in DesktopAuthManager" }
                // Port occupancy errors can be handled here
            }
        }
    }

    override suspend fun signInWithGoogle(context: Any?): GoogleAuthTokens? {
        val deferred = CompletableDeferred<GoogleAuthTokens?>()
        resultDeferred = deferred
        
        val scope = "email profile openid"
        val encodedRedirect = URLEncoder.encode(getRedirectUrl(), "UTF-8")
        val encodedScope = URLEncoder.encode(scope, "UTF-8")
        val nonce = UUID.randomUUID().toString()
        
        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=${BuildKonfig.GOOGLE_SERVER_CLIENT_ID}&" +
                "redirect_uri=$encodedRedirect&" +
                "response_type=id_token&" +
                "scope=$encodedScope&" +
                "nonce=$nonce&" +
                "prompt=select_account"

        startLogin(authUrl)
        
        return resultDeferred?.await()
    }

    private fun loadSuccessHtml(): String {
        return try {
            val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("auth_success.html")
            if (resourceStream != null) {
                InputStreamReader(resourceStream, Charsets.UTF_8).use { it.readText() }
            } else {
                "<html><body><h1>Login Successful</h1></body></html>"
            }
        } catch (e: Exception) {
            "<html><body><h1>Login Successful</h1></body></html>"
        }
    }
}

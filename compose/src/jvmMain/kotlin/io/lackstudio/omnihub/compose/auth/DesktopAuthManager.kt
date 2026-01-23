package io.lackstudio.omnihub.compose.auth

import java.awt.Desktop
import java.awt.Taskbar
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URI
import kotlin.concurrent.thread

class DesktopAuthManager : AuthManager {
    private val callbackPort = 54321

    // Desktop uses a fixed URL: http://localhost:54321/callback
    override fun getRedirectUrl(): String {
        return "http://localhost:$callbackPort/callback"
    }

    override fun startLogin(authUrl: String) {
        // Start a thread to run the Server (to avoid blocking the UI)
        thread {
            try {
                // Simple HTTP Server
                val serverSocket = ServerSocket(callbackPort)
                println("🖥️ Desktop Auth Server listening on port $callbackPort...")

                // Open the system browser and go to the OAuth2 login page
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(authUrl))
                }

                // Wait for browser redirect (Blocking)
                val clientSocket = serverSocket.accept()

                // Read Request
                val reader = clientSocket.getInputStream().bufferedReader()
                val requestLine = reader.readLine() // e.g., "GET /callback?code=XYZ... HTTP/1.1"

                if (requestLine != null && requestLine.contains("code=")) {
                    val code = requestLine.substringAfter("code=").substringBefore(" ")
                    println("✅ Desktop received code: $code")

                    // Push into DeepLinkBuffer
                    // To bypass the ViewModel's parsing logic, we disguise it as a Deep Link format
                    DeepLinkBuffer.setDeepLink("omnihub://auth/callback?code=$code")
                }

                val htmlContent = try {
                    val resourceStream = Thread.currentThread().contextClassLoader
                        .getResourceAsStream("auth_success.html")

                    if (resourceStream != null) {
                        // Read Stream and convert to String
                        InputStreamReader(resourceStream, Charsets.UTF_8).use { it.readText() }
                    } else {
                        // If the file is not found (Fallback), return simple default text
                        "<html><body><h1>Login Successful (File not found)</h1></body></html>"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    "<html><body><h1>Login Successful (Error loading file)</h1></body></html>"
                }

                // Return HTML to the browser (to reassure the user)
                val writer = PrintWriter(clientSocket.getOutputStream())
                writer.println("HTTP/1.1 200 OK")
                writer.println("Content-Type: text/html; charset=UTF-8")
                writer.println("\r\n")
                writer.print(htmlContent)
                writer.flush()

                // close socket
                clientSocket.close()
                serverSocket.close()

                try {
                    if (Taskbar.isTaskbarSupported() &&
                        Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION)) {
                        // This will make the Dock icon bounce until the user clicks the App
                        Taskbar.getTaskbar().requestUserAttention(true, true)
                    }
                } catch (e: Exception) {
                    // Ignore unsupported platform errors
                    println("exception error message ${e.message}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Port occupancy errors can be handled here
            }
        }
    }
}

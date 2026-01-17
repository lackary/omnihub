window.onload = function() {
    // Get the 'code' parameter from the URL (?code=...)
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const statusText = document.getElementById('status');

    if (code) {
        statusText.innerText = "Log in successful! Redirecting...";

        // --- For Desktop App (Deep Link) ---
        // Try to wake up the omnihub:// protocol
        // The browser will usually show a prompt: "Do you want to open OmniHub?"
        // Note: window.location.href is used here to trigger the custom protocol
        window.location.href = "omnihub://auth/callback?code=" + code;

        // --- For Web App (KMP Wasm) ---
        // Web version users will not have the omnihub:// protocol
        // We store the code and then redirect back to the Web App homepage

        // Store the Code in LocalStorage for the main program (Wasm) to read during initialization
        localStorage.setItem('unsplash_auth_code', code);

        // Delayed redirect to the parent directory (back to the /omnihub/ root)
        // Use '../..' because we are currently in /auth/callback/ and need to return to the root
        setTimeout(() => {
            // If the page is deployed at https://lackary.github.io/omnihub/
            // This will redirect back to that location
            window.location.href = "../../";
        }, 1500);

    } else {
        statusText.innerText = "Error: No authorization code found.";
        statusText.style.color = "red";
    }
}

window.onload = function() {
    console.log("OmniHub Auth Callback Loaded");

    // Get URL parameters (from Query or Fragment)
    const params = new URLSearchParams(window.location.search || window.location.hash.substring(1));
    const code = params.get('code');
    const idToken = params.get('id_token') || params.get('idToken');
    const state = params.get('state');
    const platform = params.get('platform');
    const statusText = document.getElementById('status');

    console.log("Params:", { code: !!code, idToken: !!idToken, state, platform });
    console.log("Window Info:", { opener: !!window.opener, name: window.name });

    if (code || idToken) {
        // --- 1. Storage (for Web App use) ---
        if (code) localStorage.setItem('unsplash_auth_code', code);
        if (idToken) localStorage.setItem('google_id_token', idToken);

        if (platform === 'desktop') {
            // --- 2. Desktop Flow (Deep Link) ---

            // Display a nice success interface (Matching Material 3 style)
            console.log("Executing Desktop (Deep Link) Flow");
            document.body.innerHTML = `
                <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 80vh; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; padding: 20px;">
                    <div style="font-size: 64px; color: #4CAF50; margin-bottom: 20px;">✓</div>
                    <h1 style="margin: 0; color: #333;">Authentication Successful!</h1>
                    <p style="color: #666; font-size: 18px; margin-top: 10px;">Please return to the <b>OmniHub</b> app to continue.</p>
                    <p style="color: #999; font-size: 14px; margin-top: 30px;">You can now safely close this browser tab.</p>
                </div>
            `;

            let deepLink = "omnihub://auth/callback?";
            if (code) deepLink += "code=" + code;
            if (idToken) {
                const separator = code ? "&" : "";
                deepLink += separator + "idToken=" + idToken;
            }
            window.location.href = deepLink;

        } else {
            // --- 3. Web App Flow ---

            // Check if this is a popup flow (using state or window.opener)
            const isPopup = (state === 'web_popup') || (window.opener && window.opener !== window);

            if (isPopup) {
                console.log("Executing Popup Flow");
                if (window.opener) {
                    console.log("Sending message to opener:", window.location.origin);
                    window.opener.postMessage({
                        type: 'auth_success',
                        code: code,
                        idToken: idToken
                    }, "*"); // Use * for debugging origin issues
                } else {
                    console.warn("Popup flow detected but window.opener is missing!");
                }

                document.body.innerHTML = `
                    <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 80vh; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; padding: 20px;">
                        <div style="font-size: 64px; color: #4CAF50; margin-bottom: 20px;">✓</div>
                        <h1 style="margin: 0; color: #333;">Authentication Successful!</h1>
                        <p style="color: #666; font-size: 18px; margin-top: 10px;">You can now safely close this window.</p>
                    </div>
                `;
                setTimeout(() => {
                    console.log("Closing popup...");
                    window.close();
                }, 1500);

                // CRITICAL: Stop here for popup flow
                return;
            }

            console.log("Executing Standard Redirect Flow");
            // Redirect back to home for standard full-page flow
            statusText.innerText = "Login successful! Redirecting to home...";
            statusText.style.color = "#4CAF50";

            setTimeout(() => {
                console.log("Redirecting to app root...");
                window.location.href = "../../";
            }, 1500);
        }

    } else {
        // Error Handling
        console.error("No authorization code or token found in URL");
        statusText.innerText = "Error: No authorization code found.";
        statusText.style.color = "#FF3B30";
        statusText.style.fontWeight = "bold";
    }
}

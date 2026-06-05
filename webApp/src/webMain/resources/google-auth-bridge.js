/**
 * Google Identity Services Bridge for Kotlin/JS and Kotlin/Wasm
 */
window.initAndPromptGoogleSignIn = function(clientId, callback) {
    console.log("JS Bridge: Initializing Google Auth with Client ID: " + clientId);
    if (typeof google === 'undefined' || !google.accounts || !google.accounts.id) {
        console.error("JS Bridge: Google Identity Services SDK not loaded yet.");
        return;
    }

    google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
            console.log("JS Bridge: Received response from Google");
            // Check if we are in Wasm environment (callback might be a function)
            // or JS environment (callback might be a wrapper)
            callback(response.credential);
        }
    });
    google.accounts.id.prompt();
};

/**
 * Google Identity Services Bridge for Kotlin/JS and Kotlin/Wasm
 */
window.initAndPromptGoogleSignIn = function(clientId, callback) {
    console.log("JS Bridge: Initializing Google Auth with Client ID: " + clientId);
    if (typeof google === 'undefined' || !google.accounts || !google.accounts.id) {
        console.error("JS Bridge: Google Identity Services SDK not loaded yet.");
        callback(null)
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
    google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed()) {
            console.warn("JS Bridge: Prompt not displayed:", notification.getNotDisplayedReason());
            callback(null);
        } else if (notification.isSkippedMoment()) {
            console.warn("JS Bridge: Prompt skipped:", notification.getSkippedReason());
            callback(null);
        } else if (notification.isDismissedMoment()) {
            console.warn("JS Bridge: Prompt dismissed:", notification.getDismissedReason());
            callback(null); // Notify Kotlin to end the process when the user dismisses the prompt
        }
    });
};

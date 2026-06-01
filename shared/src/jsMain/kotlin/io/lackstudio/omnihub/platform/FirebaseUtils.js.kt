package io.lackstudio.omnihub.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.lackstudio.omnihub.utils.getFirebaseWebConfig
import io.lackstudio.omnihub.utils.logging.AppLog

actual fun initializeFirebase() {
    val config = getFirebaseWebConfig()
    if (config != null) {
        Firebase.initialize(
            options = FirebaseOptions(
                apiKey = config.apiKey,
                authDomain = config.authDomain,
                projectId = config.projectId,
                storageBucket = config.storageBucket,
                applicationId = config.appId
            )
        )
        AppLog.d {"Firebase JS initialized successfully"}
    } else {
        AppLog.w { "Firebase JS config not found" }
    }
}

package io.lackstudio.omnihub.platform

import android.app.Application
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.lackstudio.omnihub.utils.getFirebaseWebConfig
import io.lackstudio.omnihub.utils.logging.AppLog

actual fun initializeFirebase() {
    // Desktop initialization can be added here if needed
    FirebasePlatform.initializeFirebasePlatform(
        object : FirebasePlatform() {
            val storage = mutableMapOf<String, String>()
            override fun store(key: String, value: String) = storage.set(key, value)
            override fun retrieve(key: String) = storage[key]
            override fun clear(key: String) { storage.remove(key) }
            override fun log(msg: String) = println(msg)
        }
    )

    val config = getFirebaseWebConfig()
    if (config != null) {
        Firebase.initialize(
            context = Application(),
            options = FirebaseOptions(
                apiKey = config.apiKey,
                authDomain = config.authDomain,
                projectId = config.projectId,
                storageBucket = config.storageBucket,
                applicationId = config.appId
            )
        )
        AppLog.i { "Firebase JVM initialized successfully" }
    } else {
        AppLog.w { "Firebase JVM config not found" }
    }
}

package io.lackstudio.omnihub.platform

import android.app.Application
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.lackstudio.omnihub.utils.getFirebaseWebConfig
import io.lackstudio.omnihub.utils.logging.AppLog
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

actual fun initializeFirebase() {
    // Desktop initialization can be added here if needed
    FirebasePlatform.initializeFirebasePlatform(
        object : FirebasePlatform() {
            private val prefs = Preferences.userRoot().node("io.lackstudio.omnihub.firebase")

            override fun store(key: String, value: String) {
                prefs.put(key, value)
                forceSyncToDisk()
            }

            override fun retrieve(key: String): String? = prefs.get(key, null)

            override fun clear(key: String) {
                prefs.remove(key)
                forceSyncToDisk()
            }

            override fun log(msg: String) = AppLog.d { "Firebase JVM: $msg" }

            private fun forceSyncToDisk() {
                try {
                    prefs.flush()
                } catch (e: BackingStoreException) {
                    AppLog.e(throwable = e) { "Failed to flush preferences to disk" }
                }
            }
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

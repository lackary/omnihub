package io.lackstudio.omnihub

import android.app.Application
import org.koin.android.ext.koin.androidContext
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnifeed.auth.platform.initializeFirebase
import io.lackstudio.omnihub.platform.firebaseWebBase64

class OmniHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Call manually here (maintained for consistency, although the Android plugin does it automatically)
        initializeFirebase(firebaseWebBase64)

        initKoin {
            androidContext(this@OmniHubApplication)
        }
    }
}

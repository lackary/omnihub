package io.lackstudio.omnihub

import io.lackstudio.omnihub.di.initKoin
import android.app.Application
import io.lackstudio.omnihub.platform.initializeFirebase
import org.koin.android.ext.koin.androidContext

class OmniHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Call manually here (maintained for consistency, although the Android plugin does it automatically)
        initializeFirebase()

        initKoin {
            androidContext(this@OmniHubApplication)
        }
    }
}

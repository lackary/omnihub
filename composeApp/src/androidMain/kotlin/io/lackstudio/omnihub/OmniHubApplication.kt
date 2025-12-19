package io.lackstudio.omnihub

import android.app.Application
import io.lackstudio.omnihub.di.initKoin
import org.koin.android.ext.koin.androidContext

class OmniHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@OmniHubApplication)
        }
    }
}

package io.lackstudio.omnihub.di

import io.lackstudio.omnifeed.core.common.util.appPlatformLogWriter
import io.lackstudio.omnifeed.core.di.appLoggerModule
import io.lackstudio.omnifeed.unsplash.di.unsplashModule
import io.lackstudio.omnifeed.unsplash.utils.Environment.AUTH_SCHEME_PUBLIC
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

// Define a shared init function here
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this) // Allow platforms to pass extra configuration (e.g., Android Context)
        modules(
            appModule,
            appLoggerModule(appPlatformLogWriter()),
            unsplashModule(
                tokenType = AUTH_SCHEME_PUBLIC,
                token = getUnsplashAccessKey()
            ),
        )
    }
}

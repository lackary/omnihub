package io.lackstudio.omnihub.di

import io.lackstudio.omnifeed.auth.di.omnifeedAuthModule
import io.lackstudio.omnifeed.core.OmniFeedConfig
import io.lackstudio.omnifeed.core.UnsplashConfig
import io.lackstudio.omnifeed.core.common.logging.createOmniFeedLogger
import io.lackstudio.omnifeed.core.di.coreModule
import io.lackstudio.omnifeed.unsplash.di.unsplashModule
import io.lackstudio.omnifeed.unsplash.utils.Environment as UnsplashEnvironment
import io.lackstudio.omnihub.platform.authModule
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import io.lackstudio.omnihub.utils.logging.AppLog
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

// Define a shared init function here
fun initKoin(config: KoinAppDeclaration? = null) {

    val appLogger = createOmniFeedLogger(
        isDebug = true,
        tag = "OmniFeedApp"
    )

    AppLog.init(appLogger)

    val omniFeedConfig = OmniFeedConfig(
        appLogger = appLogger,
        unsplash = UnsplashConfig(
            tokenType = UnsplashEnvironment.AUTH_SCHEME_PUBLIC,
            token = getUnsplashAccessKey(),
        )
    )
    startKoin {
        config?.invoke(this) // Allow platforms to pass extra configuration (e.g., Android Context)
        modules(
            coreModule(omniFeedConfig),
            unsplashModule(
                tokenType = omniFeedConfig.unsplash.tokenType,
                token = omniFeedConfig.unsplash.token
            ),
            omnifeedAuthModule,
            authModule,
            appModule
        )
    }
}

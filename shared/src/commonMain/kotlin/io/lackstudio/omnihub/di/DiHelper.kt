package io.lackstudio.omnihub.di

import io.lackstudio.omnifeed.auth.domain.repository.AuthRepository
import io.lackstudio.omnifeed.auth.di.omnifeedAuthModule
import io.lackstudio.omnifeed.core.CustomServiceConfig
import io.lackstudio.omnifeed.core.OmniFeedConfig
import io.lackstudio.omnifeed.core.UnsplashConfig
import io.lackstudio.omnifeed.core.common.logging.createOmniFeedLogger
import io.lackstudio.omnifeed.core.di.coreModule
import io.lackstudio.omnifeed.core.network.oauth.AccessTokenProvider
import io.lackstudio.omnifeed.core.network.oauth.AuthToken
import io.lackstudio.omnifeed.unsplash.di.unsplashModule
import io.lackstudio.omnifeed.unsplash.utils.Environment as UnsplashEnvironment
import io.lackstudio.omnihub.platform.authModule
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import io.lackstudio.omnihub.shared.BuildKonfig
import io.lackstudio.omnihub.utils.Environment
import io.lackstudio.omnihub.utils.logging.AppLog
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

// Define a shared init function here
fun initKoin(config: KoinAppDeclaration? = null) {

    val appLogger = createOmniFeedLogger(
        isDebug = true,
        tag = "OmniFeedApp"
    )

    AppLog.init(appLogger)

    // Helper to construct Cloud Function URLs
    fun getCloudFunctionUrl(path: String): String {
        return "https://${BuildKonfig.FIREBASE_REGION}-${BuildKonfig.FIREBASE_PROJECT_ID}.cloudfunctions.net/$path"
    }

    val omniFeedConfig = OmniFeedConfig(
        appLogger = appLogger,
        unsplash = UnsplashConfig(
            tokenType = UnsplashEnvironment.AUTH_SCHEME_PUBLIC,
            token = getUnsplashAccessKey(),
        ),
        customServices = mapOf(
            Environment.SERVICE_UNSPLASH to CustomServiceConfig(
                authEndpoint = getCloudFunctionUrl(BuildKonfig.FIREBASE_EXT_CUSTOM_AUTH_PATH),
                linkedField = "isUnsplashLinked"
            )
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
            module {
                // Override AccessTokenProvider for Unsplash to make it dynamic
                single<AccessTokenProvider>(createdAtStart = false) {
                    val authRepository = get<AuthRepository>()
                    AccessTokenProvider(
                        initialTokenType = omniFeedConfig.unsplash.tokenType,
                        initialToken = omniFeedConfig.unsplash.token,
                        dynamicTokenResolver = {
                            authRepository.getServiceToken(Environment.SERVICE_UNSPLASH)?.let {
                                AuthToken("Bearer", it)
                            }
                        }
                    )
                }
            },
            authModule,
            appModule
        )
    }
}

package io.lackstudio.omnihub.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

// Define a shared init function here
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this) // Allow platforms to pass extra configuration (e.g., Android Context)
        modules(appModule)   // Load the appModule defined earlier
    }
}

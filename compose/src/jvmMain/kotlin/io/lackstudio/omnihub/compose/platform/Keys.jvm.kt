package io.lackstudio.omnihub.compose.platform

import io.lackstudio.omnihub.compose.BuildKonfig

actual fun getUnsplashAccessKey(): String = BuildKonfig.UNSPLASH_ACCESS_KEY
actual fun getUnsplashSecretKey(): String = BuildKonfig.UNSPLASH_SECRET_KEY

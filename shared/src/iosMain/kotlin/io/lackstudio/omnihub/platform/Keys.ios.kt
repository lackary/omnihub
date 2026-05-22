package io.lackstudio.omnihub.platform

import io.lackstudio.omnihub.shared.BuildKonfig

actual fun getUnsplashAccessKey(): String = BuildKonfig.UNSPLASH_ACCESS_KEY
actual fun getUnsplashSecretKey(): String = BuildKonfig.UNSPLASH_SECRET_KEY

package io.lackstudio.omnihub

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
package io.lackstudio.omnihub.di

import io.lackstudio.omnihub.ui.gallery.GalleryViewModel
import org.koin.core.module.dsl.viewModelOf // ⭐ Key import
import org.koin.dsl.module

val appModule = module {
    // Use viewModelOf for automatic binding
    // Koin automatically detects GalleryViewModel constructor parameters
    // (currently empty, but convenient for adding Repositories in the future)
    viewModelOf(::GalleryViewModel)
}

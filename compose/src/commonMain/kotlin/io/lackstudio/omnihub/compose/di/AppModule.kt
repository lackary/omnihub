package io.lackstudio.omnihub.compose.di

import io.lackstudio.omnihub.compose.ui.gallery.CollectionViewModel
import io.lackstudio.omnihub.compose.ui.gallery.GalleryViewModel
import io.lackstudio.omnihub.compose.ui.gallery.PhotoViewModel
import io.lackstudio.omnihub.compose.ui.gallery.TopicViewModel
import io.lackstudio.omnihub.compose.ui.gallery.UserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Use viewModelOf for automatic binding
    // Koin automatically detects GalleryViewModel constructor parameters
    // (currently empty, but convenient for adding Repositories in the future)
    viewModelOf(::GalleryViewModel)
    viewModelOf(::PhotoViewModel)
    viewModelOf(::CollectionViewModel)
    viewModelOf(::TopicViewModel)
    viewModelOf(::UserViewModel)
}

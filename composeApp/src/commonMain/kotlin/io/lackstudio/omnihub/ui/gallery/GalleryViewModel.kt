package io.lackstudio.omnihub.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryViewModel : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state = _state.asStateFlow()

    init {
        // Automatically load all dummy data when ViewModel initializes
        loadDummyData()
    }

    fun handleIntent(intent: GalleryIntent) {
        when (intent) {
            is GalleryIntent.SelectTab -> {
                _state.update { it.copy(currentTabIndex = intent.index) }
            }
            is GalleryIntent.Refresh -> {
                loadDummyData()
            }
        }
    }

    private fun loadDummyData() {
        viewModelScope.launch {
            // 1. Set to Loading state first
            _state.update {
                it.copy(
                    photosState = it.photosState.copy(isLoading = true),
                    collectionsState = it.collectionsState.copy(isLoading = true),
                    topicsState = it.topicsState.copy(isLoading = true)
                )
            }

            // 2. Simulate network delay (to show loading spinner)
            delay(1500)

            // 3. Generate dummy data (using Picsum)
            val dummyPhotos = List(20) { i ->
                GalleryPhoto(
                    id = "$i",
                    // seed ensures images are the same after refresh, 300/400 represents width/height
                    url = "https://picsum.photos/seed/photo$i/300/400",
                    title = "Photo Item $i"
                )
            }

            val dummyCollections = List(10) { i ->
                GalleryCollection(
                    id = "$i",
                    coverUrl = "https://picsum.photos/seed/col$i/400/300",
                    title = "Collection $i",
                    totalPhotos = (10..100).random(),
                )
            }

            val dummyTopics = List(8) { i ->
                GalleryTopic(
                    id = "$i",
                    coverUrl = "https://picsum.photos/seed/topic$i/400/250",
                    title = "Topic $i",
                    description = "This is a description for topic $i"
                )
            }

            // 4. Update state to Success
            _state.update {
                it.copy(
                    photosState = PhotosState(isLoading = false, items = dummyPhotos),
                    collectionsState = CollectionsState(isLoading = false, items = dummyCollections),
                    topicsState = TopicsState(isLoading = false, items = dummyTopics)
                )
            }
        }
    }
}

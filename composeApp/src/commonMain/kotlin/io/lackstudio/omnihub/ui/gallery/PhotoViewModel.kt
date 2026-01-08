package io.lackstudio.omnihub.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PhotoViewModel(
    private val getPhotoUseCase: GetPhotoUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(PhotoDetailUiState())
    val state = _state.asStateFlow()

    private var currentId: String? = null

    fun handleIntent(intent: PhotoDetailIntent) {
        when (intent) {
            is PhotoDetailIntent.LoadDetail -> fetchPhoto(intent.id)
            is PhotoDetailIntent.Retry -> currentId?.let { fetchPhoto(it) }
        }
    }

    private fun fetchPhoto(id: String) {
        handleUseCaseCall(
            useCase = { getPhotoUseCase(id) },
            onLoading = {
                _state.update { it.copy(detailState = AppUiState.Loading) }
            },
            onSuccess = { photo ->
                val detail = Photo(
                    id = photo.id,
                    fullUrl = photo.urls.full, // use regular or full
                    username = photo.user.username,
                    userAvatar = photo.user.profileImage.medium,
                    description = photo.description ?: photo.altDescription,
                    views = photo.views,
                    downloads = photo.downloads,
                    likes = photo.likes,
                    createdAt = photo.createdAt,
                    exif = photo.exif.let {
                        PhotoExif(
                            make = it.make,
                            model = it.model,
                            aperture = it.aperture,
                            exposureTime = it.exposureTime,
                            iso = it.iso,
                            focalLength = it.focalLength
                        )
                    },
                    location = photo.location.let { loc ->
                        PhotoLocation(
                            city = loc.city,
                            country = loc.country,
                            latitude = loc.position.latitude,
                            longitude = loc.position.longitude
                        )
                    }
                )
                _state.update { it.copy(detailState = AppUiState.Success(detail)) }
            },
            onError = { msg ->
//                println("Photo error: $msg")
                _state.update { it.copy(detailState = AppUiState.Error(msg)) }
            }
        )
    }

}

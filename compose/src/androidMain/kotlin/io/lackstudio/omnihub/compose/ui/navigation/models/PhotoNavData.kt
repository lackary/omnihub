package io.lackstudio.omnihub.compose.ui.navigation.models

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoNavData(
    val photoId: String,
    val thumbUrl: String = "",
    val ratio: Float = 1f
) : Parcelable {
    companion object {
        const val EXTRA_ID = "PHOTO_ID"
        const val EXTRA_URL = "THUMB_URL"
        const val EXTRA_RATIO = "RATIO"

        fun Intent.putPhotoNavData(data: PhotoNavData): Intent {
            return this.apply {
                putExtra(EXTRA_ID, data.photoId)
                putExtra(EXTRA_URL, data.thumbUrl)
                putExtra(EXTRA_RATIO, data.ratio)
            }
        }

        fun fromIntent(intent: Intent): PhotoNavData? {
            val id = intent.getStringExtra(EXTRA_ID) ?: return null
            return PhotoNavData(
                photoId = id,
                thumbUrl = intent.getStringExtra(EXTRA_URL) ?: "",
                ratio = intent.getFloatExtra(EXTRA_RATIO, 1f)
            )
        }
    }
}

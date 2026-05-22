import io.lackstudio.omnihub.ui.navigation.models.PhotoNavData

/**
 * Atomic state for the Photo Stack UI.
 * 
 * WHY ATOMIC? 
 * By combining [photos] and [currentPhotoId] into a single data class, we ensure that updates 
 * to both are seen by the UI at the exact same time. This prevents inconsistent intermediate 
 * states (e.g., having a current ID but an empty list) that could cause Composables to be 
 * prematurely disposed and recreated, leading to "LayoutInstance" resets and flickering.
 */
data class PhotoStackState(
    val photos: List<PhotoNavData> = emptyList(),
    val currentPhotoId: String? = null
)

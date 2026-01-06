package io.lackstudio.omnihub.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.animation.SharedTransitionLayout
import coil3.compose.LocalPlatformContext
import coil3.size.Size
import io.lackstudio.omnifeed.ui.state.AppUiState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

// 1. Stateful Composable (Responsible for logic and data flow)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailScreen(
    id: String,
    thumbUrl: String,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: PhotoDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(id) {
        viewModel.handleIntent(PhotoDetailIntent.LoadDetail(id))
    }

    PhotoDetailContent(
        id = id,
        thumbUrl = thumbUrl,
        state = state,
        onBack = onBack,
        onRetry = { viewModel.handleIntent(PhotoDetailIntent.Retry) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

// 2. Stateless Composable (Pure UI, convenient for Preview and testing)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailContent(
    id: String,
    thumbUrl: String,
    state: PhotoDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Attempt to get high-resolution photo data
    val photoDetail = (state.detailState as? AppUiState.Success)?.data

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {

            // [Layer 1: Base Image / Transition Anchor] (Always visible)
            // Always use thumbUrl + Size.ORIGINAL to ensure transition doesn't flicker
            with(sharedTransitionScope) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(thumbUrl)
                        .size(Size.ORIGINAL) // Key: Force consistency with list page
                        .crossfade(false) // Disable crossfade
                        .build(),
                    contentDescription = null, // Base image doesn't need description, let the high-res image handle it
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "image-$id"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                )
            }

            // [Layer 2: High-Resolution Image] (Overlaid after data loads)
            if (photoDetail != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(photoDetail.fullUrl)
                        .crossfade(true) // Enable crossfade for high-res image to elegantly overlay thumbnail
                        .build(),
                    contentDescription = photoDetail.description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                    // Note: sharedElement is not needed here
                )
            }

            // [Layer 3: Status Indicator] (Loading / Error)
            // Use if instead of when, simply acting as an overlay
            if (state.detailState is AppUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            if (state.detailState is AppUiState.Error) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Error loading details", color = Color.White)
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Retry")
                    }
                }
            }

            // --- UI Control Layer (TopBar, BottomBar) ---
            // --- Top Bar ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Note: Do NOT add statusBarsPadding to the outer layer, and do NOT fix the height
                    // This allows the Box to extend to the very top of the screen (behind the status bar)
                    .align(Alignment.TopCenter)
            ) {
                // 1. Background Layer
                // This layer is for aesthetics; it covers the status bar to make white text clear
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp) // Set height taller to ensure a natural gradient
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                // 2. Content Layer
                // This layer is for functionality and needs to avoid the status bar (Safe Area)
                // We set the height to 64dp (standard TopAppBar height) here
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // ★ Key: Only the button layer needs to avoid the status bar
                        .height(64.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 4.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            }

            // Bottom Info Bar
            AnimatedVisibility(
                visible = state.detailState is AppUiState.Success,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // Outer Box: Only responsible for bottom positioning, do NOT set a fixed height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Background Layer (Gradient)
                    // Set height taller (e.g., 120dp) to ensure it covers the Home Indicator area and extends upwards
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter) // Align to bottom
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.6f))
                                )
                            )
                    )

                    // Content Layer
                    // Use navigationBarsPadding here to push the content up
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding() // ★ Key: Avoid the bottom navigation bar
                            .height(64.dp), // Set height of the content area
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IconButton(
                            onClick = { showBottomSheet = true },
                            modifier = Modifier
                                .padding(start = 8.dp), // Only horizontal padding needed; vertical alignment handled by Box
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info"
                            )
                        }
                    }
                }
            }
        }

        // --- Bottom Sheet ---
        if (showBottomSheet && photoDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PhotoDetailInfoContent(detail = photoDetail)
            }
        }
    }
}

@Composable
fun PhotoDetailInfoContent(detail: Photo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Info", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        HorizontalDivider()

        detail.exif?.let { exif ->
            val subtitle = listOfNotNull(
                exif.aperture,
                exif.exposureTime,
                exif.iso?.let { "ISO $it" },
                exif.focalLength
            ).joinToString(" • ")

            val title = listOfNotNull(exif.make, exif.model).joinToString(" ").trim()
            val finalTitle = title.ifBlank { "Unknown Camera" }

            if (subtitle.isNotBlank() || title.isNotBlank()) {
                InfoRow(Icons.Default.CameraAlt, finalTitle, subtitle)
            }
        }

        detail.location?.displayString()?.let { locationStr ->
            val coords = if (detail.location.latitude != null && detail.location.longitude != null) {
                "${detail.location.latitude}, ${detail.location.longitude}"
            } else ""
            InfoRow(Icons.Default.LocationOn, locationStr, coords)
        }

        if (!detail.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Photo by ${detail.username}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ----------------------------------------------------------------
// Preview Section
// ----------------------------------------------------------------

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun PhotoDetailScreenPreview() {
    // 1. Prepare dummy data
    val dummyPhoto = Photo(
        id = "1",
        fullUrl = "https://picsum.photos/400/600",
        username = "Test User",
        userAvatar = null,
        description = "This is a beautiful test photo description.",
        exif = PhotoExif("Canon", "EOS R5", "f/2.8", "1/200", 100, "24mm"),
        location = PhotoLocation("Taipei", "Taiwan", 25.0, 121.0)
    )
    val dummyState = PhotoDetailUiState(
        detailState = AppUiState.Success(dummyPhoto)
    )

    // 2. Setup SharedTransitionLayout environment (This is key)
    SharedTransitionLayout {
        // 3. Setup AnimatedVisibility environment (This is also key)
        AnimatedVisibility(visible = true) {

            // 4. Call Stateless Content
            PhotoDetailContent(
                id = "1",
                thumbUrl = "https://picsum.photos/400/600",
                state = dummyState,
                onBack = {},
                onRetry = {},
                sharedTransitionScope = this@SharedTransitionLayout, // Pass in Layout Scope
                animatedVisibilityScope = this // Pass in AnimatedVisibility Scope
            )
        }
    }
}

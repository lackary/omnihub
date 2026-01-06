package io.lackstudio.omnihub.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.LocalPlatformContext
import coil3.size.Size
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.utils.toSimpleDateStr
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
            // Top Bar
            PhotoDetailTopBar(
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Bottom Bar
            PhotoDetailBottomBar(
                photoDetail = photoDetail,
                isVisible = state.detailState is AppUiState.Success,
                onInfoClick = { showBottomSheet = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
fun PhotoDetailTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Background Layer (Gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // 2. Content Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
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
}

@Composable
fun PhotoDetailBottomBar(
    photoDetail: Photo?,
    isVisible: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Outer Box
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Background Layer (Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.6f))
                        )
                    )
            )

            // Content Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
            ) {
                // Left side: Info Icon + Date
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info"
                        )
                    }

                    photoDetail?.createdAt?.let { date ->
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = date.toSimpleDateStr(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Right side: Metadata Overlay
                photoDetail?.let {
                    PhotoMetadataOverlay(
                        photo = it,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp)
                    )
                }
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
        description = "Description",
        exif = PhotoExif("Canon", "EOS R5", "f/2.8", "1/200", 100, "24mm"),
        location = PhotoLocation("Taipei", "Taiwan", 25.0, 121.0),
        // [Add dummy data]
        views = 15400,
        likes = 342,
        downloads = 890,
        createdAt = "2024-03-25T10:00:00Z"
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

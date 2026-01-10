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
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.LocalPlatformContext
import coil3.size.Size
import io.lackstudio.omnifeed.ui.state.AppUiState
import kotlin.math.min
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

// Stateful Composable (Responsible for logic and data flow)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailScreen(
    id: String,
    thumbUrl: String,
    onBack: () -> Unit,
    onNavigateToUser: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: PhotoViewModel = koinViewModel()
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
        onNavigateToUser = onNavigateToUser,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

// Stateless Composable (Pure UI, convenient for Preview and testing)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailContent(
    id: String,
    thumbUrl: String,
    state: PhotoDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToUser: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Attempt to get high-resolution photo data
    val photoDetail = (state.detailState as? AppUiState.Success)?.data

    var imageIntrinsicSize by remember { mutableStateOf(GeometrySize.Zero) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val density = LocalDensity.current

            // Calculate layout parameters
            val detailLayoutInfo = remember(imageIntrinsicSize, screenWidth, screenHeight) {
                if (imageIntrinsicSize == GeometrySize.Zero || imageIntrinsicSize.width == 0f || imageIntrinsicSize.height == 0f) {
                    DetailLayoutInfo(0.dp, 0.dp, false, false)
                } else {
                    with(density) {
                        // First convert screen dimensions to Pixels (unified unit)
                        val screenWidthPx = screenWidth.toPx()
                        val screenHeightPx = screenHeight.toPx()

                        val srcW = imageIntrinsicSize.width
                        val srcH = imageIntrinsicSize.height

                        // Use Pixels to calculate scaling ratio
                        val scale = min(screenWidthPx / srcW, screenHeightPx / srcH)

                        val displayW = srcW * scale
                        val displayH = srcH * scale

                        // Calculate black bars (Pixels)
                        val blackBarWidthPx = (screenWidthPx - displayW) / 2
                        val bottomBlackBarHeightPx = (screenHeightPx - displayH) / 2

                        // Determine threshold (Convert to Px for comparison, or convert above Px back to Dp)
                        // Here we convert the calculated Px back to Dp for logic and storage
                        val blackBarWidthDp = blackBarWidthPx.toDp()
                        val bottomBlackBarHeightDp = bottomBlackBarHeightPx.toDp()

                        // If black bar is larger than 80dp, place content outside
                        val isOutsideH = blackBarWidthDp > 80.dp
                        val isOutsideV = bottomBlackBarHeightDp > 100.dp

                        DetailLayoutInfo(
                            blackBarWidth = blackBarWidthDp,
                            bottomBlackBarHeight = bottomBlackBarHeightDp,
                            isOutsideHorizontal = isOutsideH,
                            isOutsideVertical = isOutsideV
                        )
                    }
                }
            }
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
                        onSuccess = { state ->
                            if (imageIntrinsicSize == GeometrySize.Zero) {
                                imageIntrinsicSize = state.painter.intrinsicSize
                            }
                        },
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
                        onSuccess = { state ->
                            imageIntrinsicSize = state.painter.intrinsicSize
                        },
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
                    onUserClick = onNavigateToUser,
                    layoutInfo = detailLayoutInfo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
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
fun PhotoDetailTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background Layer (Gradient)
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

        // Content Layer
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
    onUserClick: (String) -> Unit,
    layoutInfo: DetailLayoutInfo,
    modifier: Modifier = Modifier
) {
    val gap = 12.dp

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier
            // First apply system safe area padding (this automatically becomes about 34dp on iOS)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Outer Box
        Box(modifier = Modifier.fillMaxWidth()) {
            val gradientAlpha = if (layoutInfo.isOutsideHorizontal || layoutInfo.isOutsideVertical) 0.0f else 0.6f
            // Background Layer (Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(gradientAlpha))
                        )
                    )
            )

            // --- Left Area (Info + Date) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart) // Default anchor: BottomStart
                    .padding(bottom = 8.dp)
            ) {
                // Determine content position based on mode
                val contentModifier = if (layoutInfo.isOutsideHorizontal) {
                    // [Mode A: Place inside black bar]
                    // Width limited to black bar width, content aligned to End (Right) -> Sticks to the left edge of the image
                    Modifier
                        .width(layoutInfo.blackBarWidth)
                        .padding(end = gap)
                        .align(Alignment.CenterStart) // Box itself positioned on the left
                } else {
                    // [Mode B: Overlay on image]
                    // Content starts from left (black bar width + gap) -> Overlays inside the image
                    Modifier
                        .padding(start = layoutInfo.blackBarWidth + gap)
                        .align(Alignment.CenterStart)
                }

                // Inner container
                Column(
                    modifier = contentModifier, // Apply Modifier from above
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // If this Column is in Outside mode, since parent width is restricted,
                    // to make it align right (close to image), we need to set Column's self alignment to End
                    // But since Modifier already handled padding/width, here we just need to ensure content is centered

                    // To be safe, if in Outside mode, force it to align End (Right)
                    val alignment = if (layoutInfo.isOutsideHorizontal) Alignment.End else Alignment.Start

                    Column(
                        modifier = Modifier.fillMaxWidth(), // Fill this small section
                        horizontalAlignment = alignment
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onInfoClick,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = "Info")
                            }

                            if (photoDetail != null) {
                                Text(
                                    text = "Photo by ${photoDetail.name} on Unsplash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.width(IntrinsicSize.Max)
                                )
                            }
                        }
                    }
                }
            }

            // --- Right Area (Metadata Overlay) ---
            photoDetail?.let { photo ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Default anchor: BottomEnd
                        .padding(bottom = 8.dp)
                ) {
                    val contentModifier = if (layoutInfo.isOutsideHorizontal) {
                        // [Mode A: Place inside black bar]
                        // Width limited to black bar width, content aligned to Start (Left) -> Sticks to the right edge of the image
                        Modifier
                            .width(layoutInfo.blackBarWidth)
                            .padding(start = gap)
                            .align(Alignment.CenterEnd)
                    } else {
                        // [Mode B: Overlay on image]
                        Modifier
                            .padding(end = layoutInfo.blackBarWidth + gap)
                            .align(Alignment.CenterEnd)
                    }

                    Box(modifier = contentModifier) {
                        // Ensure internal alignment here as well
                        val alignment = if (layoutInfo.isOutsideHorizontal) Alignment.BottomStart else Alignment.BottomEnd

                        PhotoMetadataOverlay(
                            photo = photo,
                            isLayoutOutside = layoutInfo.isOutsideHorizontal,
                            onUserClick = onUserClick,
                            modifier = Modifier.align(alignment)
                        )
                    }
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
    // Prepare dummy data
    val dummyPhoto = Photo(
        id = "1",
        fullUrl = "https://picsum.photos/400/600",
        username = "TestUser",
        name = "Test User",
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

    // Setup SharedTransitionLayout environment (This is key)
    SharedTransitionLayout {
        // Setup AnimatedVisibility environment (This is also key)
        AnimatedVisibility(visible = true) {

            // Call Stateless Content
            PhotoDetailContent(
                id = "1",
                thumbUrl = "https://picsum.photos/400/600",
                state = dummyState,
                onBack = {},
                onRetry = {},
                onNavigateToUser = {},
                sharedTransitionScope = this@SharedTransitionLayout, // Pass in Layout Scope
                animatedVisibilityScope = this // Pass in AnimatedVisibility Scope
            )
        }
    }
}

data class DetailLayoutInfo(
    val blackBarWidth: Dp,
    val bottomBlackBarHeight: Dp,
    val isOutsideHorizontal: Boolean,
    val isOutsideVertical: Boolean
)

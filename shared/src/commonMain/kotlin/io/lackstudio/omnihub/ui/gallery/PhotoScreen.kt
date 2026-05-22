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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.LocalPlatformContext
import coil3.size.Size
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.navigation.Feature
import io.lackstudio.omnihub.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.utils.LocalXrNavigation
import io.lackstudio.omnihub.utils.UnsplashLinks
import io.lackstudio.omnihub.utils.logging.rememberLogger
import kotlin.math.min
import org.koin.compose.viewmodel.koinViewModel

// Stateful Composable (Responsible for logic and data flow)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailScreen(
    id: String,
    thumbUrl: String,
    onBack: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    // Add key = id
    // This forces Koin to create and retain a separate ViewModel instance for each unique photo ID.
    viewModel: PhotoViewModel = koinViewModel(key = id)
) {
    val logger = rememberLogger("PhotoDetailScreen")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val xrNav = LocalXrNavigation.current

    LaunchedEffect(id) {
        viewModel.handleIntent(PhotoDetailIntent.LoadDetail(id))
    }

    PhotoDetailContent(
        id = id,
        thumbUrl = thumbUrl,
        state = state,
        onBack = onBack,
        onRetry = {
            logger.d { "PhotoDetailScreen: onRetry" }
            viewModel.handleIntent(PhotoDetailIntent.Retry)
        },
        onNavigateToUser = { username ->
            if (xrNav != null) {
                xrNav(XrNavEvent.NavigateToUser(username))
            } else {
                onNavigateToFeature(Feature.User(username))
            }

        },
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
                    // Get the error message and check if it's a Rate Limit error
                    val errorMessage = state.detailState.message
                    val isRateLimited =
                        errorMessage.contains("Rate Limit", ignoreCase = true) ||
                                errorMessage.contains("403")

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isRateLimited) {
                            // If it's a Rate Limit error, only show the message, no Retry button
                            Text("Rate Limited Exceeded", color = Color.White)
                        } else {
                            // For other errors, show the Retry button
                            Text("Error loading details", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Retry")
                            }
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
    val contentGap = 8.dp // Safety gap between left and right blocks
    val edgePadding = 8.dp // Distance from content to image edge

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier
            // First apply system safe area padding (this automatically becomes about 34dp on iOS)
            .windowInsetsPadding(WindowInsets.navigationBars),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Outer Box
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Layer (Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.8f))
                        )
                    )
            )

            // 2. Content Layout Layer (Uses Row to manage left-right arrangement)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    // start/end: Add blackBarWidth to ensure content is always "flush with the image edge", not on the black bars
                    // bottom: Unified bottom padding
                    .padding(
                        start = layoutInfo.blackBarWidth + edgePadding,
                        end = layoutInfo.blackBarWidth + edgePadding,
                        bottom = edgePadding
                    ),
                horizontalArrangement = Arrangement.SpaceBetween, // Spread out left and right
                verticalAlignment = Alignment.Bottom // Bottom aligned
            ) {

                // --- Left side：(Info icon + attribution text) ---
                Column(
                    modifier = Modifier
                        .weight(1f) // Occupy all remaining space to allow long text to wrap automatically
                        .padding(end = contentGap), // Avoid being too close to the right side
                    horizontalAlignment = Alignment.Start
                ) {
                    // Info Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onInfoClick,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                            modifier = Modifier.size(24.dp) // Adjust button size to align with text
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Attribution Link
                    if (photoDetail != null) {
                        val annotatedString = buildAnnotatedString {
                            append("Photo by ")
                            // User Profile -> Unsplash
                            withLink(LinkAnnotation.Url(UnsplashLinks.userProfile(photoDetail.username))) {
                                withStyle(
                                    SpanStyle(
                                        color = Color.White,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(color = Color.Black, blurRadius = 4f)
                                    )
                                ) {
                                    append(photoDetail.name)
                                }
                            }
                            append(" on ")
                            // Unsplash Home -> Unsplash
                            withLink(LinkAnnotation.Url(UnsplashLinks.home())) {
                                withStyle(
                                    SpanStyle(
                                        color = Color.White,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(color = Color.Black, blurRadius = 4f)
                                    )
                                ) {
                                    append("Unsplash")
                                }
                            }
                        }

                        // attribution text
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.labelSmall.copy(
                                shadow = Shadow(color = Color.Black, blurRadius = 4f)
                            ),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth() 
                        )
                    }
                }

                // --- Right side (avatar + views + likes + downloads + created ) ---
                if (photoDetail != null) {
                    // Box to wrap the right-side content, ensuring it only occupies the required width
                    Box(modifier = Modifier.wrapContentWidth()) {
                        PhotoMetadataOverlay(
                            photo = photoDetail,
                            isLayoutOutside = false,
                            onUserClick = onUserClick
                        )
                    }
                }
            }
        }
    }
}

data class DetailLayoutInfo(
    val blackBarWidth: Dp,
    val bottomBlackBarHeight: Dp,
    val isOutsideHorizontal: Boolean,
    val isOutsideVertical: Boolean
)

// ----------------------------------------------------------------
// Previews
// ----------------------------------------------------------------
@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Mobile", widthDp = 360, heightDp = 640)
@Preview(name = "Desktop", widthDp = 1024, heightDp = 768)
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

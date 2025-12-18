package io.lackstudio.omnihub.ui.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.lackstudio.omnihub.ui.navigation.Feature
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

// 1. Define Tab Enum (place at the top of the file or in a separate file)
enum class PhotoTab(
    val title: String,
    val icon: ImageVector
) {
    List("Photos", Icons.Filled.PhotoLibrary),
    Collections("Collections", Icons.Filled.PhotoAlbum),
    Topics("Topics", Icons.Filled.Topic)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit
) {
    // State setup
    val tabs = PhotoTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope() // Used to handle scroll animation when clicking a Tab

    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                },
                actions = {
                    IconButton(onClick = { /* Handle search click */ }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                },
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 3. Tab Row (TabRow)
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider() }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            // When clicked, scroll to the page via Coroutine
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }

            // 4. Content Area (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f) // weight(1f) fills the remaining space
            ) { pageIndex ->
                // Decide which screen to display based on the current pageIndex
                when (tabs[pageIndex]) {
                    PhotoTab.List -> PhotoListContent()
                    PhotoTab.Collections -> CollectionsContent()
                    PhotoTab.Topics -> TopicsContent()
                }
            }
        }
    }
}

// --- Temporary sub-page Placeholders below (recommended to split into separate files later) ---

@Composable
fun PhotoListContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Latest Photos List")
    }
}

@Composable
fun CollectionsContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Photo Collections / Albums")
    }
}

@Composable
fun TopicsContent() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { Text("Photo Topics") }
}

// Add this function specifically for preview
@Preview
@Composable
fun GalleryScreenPreview() {
    GalleryScreen(
        onNavigateToFeature = {}, // Provide an empty lambda to satisfy parameter requirements
        onBack = {}
    )
}

package io.lackstudio.omnihub.compose.ui.extensions

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.lackstudio.omnihub.compose.ui.components.EndOfListFooter
import io.lackstudio.omnihub.compose.ui.components.LoadingFooter

/**
 * List item builder supporting "Infinite Scrolling"
 *
 * TODO: [Refactor] Move to 'omnifeed' UI module
 * This extension function is a generic UI tool and should be moved to the Core Module (io.lackstudio.omnifeed.ui.extensions) in the future
 * so that other Feature Modules (such as User, Feed, etc.) can also share it.
 *
 * @param items List of data items
 * @param isEndOfList Whether the end of the list has been reached (controls whether to display the Footer)
 * @param onLoadMore Callback triggered when scrolling to the bottom
 * @param key (Optional) Unique key used for performance optimization (recommended to pass { it.id })
 * @param itemContent Composable for rendering each item
 * @param prefetchDistance preload more before the last 3th item
 */
inline fun <T> LazyListScope.pagingItems(
    items: List<T>,
    isEndOfList: Boolean,
    prefetchDistance: Int = 3, // Default triggers when the 3rd from last item is reached
    noinline onLoadMore: () -> Unit,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable (item: T) -> Unit
) {
    itemsIndexed(
        items = items,
        key = key?.let { keySelector -> { _, item -> keySelector(item) } }
    ) { index, item ->

        // Render content
        itemContent(item)

        // Only trigger load more when "not yet at the end"
        if (index >= items.lastIndex - prefetchDistance && !isEndOfList) {
            LaunchedEffect(Unit) {
                onLoadMore()
            }
        }
    }

    // Render bottom Footer (decide what to display based on state)
    if (items.isNotEmpty()) {
        if (isEndOfList) {
            // Case A: Already at the end -> Show "No more data"
            item(key = "footer_end_of_list") {
                EndOfListFooter()
            }
        } else {
            // Case B: Not yet at the end -> Show "Loading Spinner"
            // When the user scrolls here, they will see the spinner,
            // and the LaunchedEffect above will be triggered simultaneously
            item(key = "footer_loading") {
                LoadingFooter()
            }
        }
    }
}

/**
 * Grid list item builder supporting "Infinite Scrolling"
 *
 * Dedicated for use with LazyVerticalGrid.
 * Automatically handles Footer spanning issues, ensuring the Footer always occupies a full row.
 */
inline fun <T> LazyGridScope.pagingGridItems(
    items: List<T>,
    isEndOfList: Boolean,
    noinline onLoadMore: () -> Unit,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable (item: T) -> Unit
) {
    itemsIndexed(
        items = items,
        key = key?.let { keySelector -> { _, item -> keySelector(item) } }
    ) { index, item ->

        itemContent(item)

        // Detect bottom logic
        if (index == items.lastIndex && !isEndOfList) {
            LaunchedEffect(Unit) {
                onLoadMore()
            }
        }
    }

    // Render bottom Footer
    if (items.isNotEmpty()) {
        item(
            key = "footer",
            // Key: Make the Footer occupy all columns of the row
            span = { GridItemSpan(maxLineSpan) }
        ) {
            if (isEndOfList) {
                EndOfListFooter()
            } else {
                LoadingFooter()
            }
        }
    }
}

/**
 * Staggered Grid list item builder supporting "Infinite Scrolling"
 *
 * Designed specifically for LazyVerticalStaggeredGrid (Waterfall Flow).
 */
inline fun <T> LazyStaggeredGridScope.pagingStaggeredGridItems(
    items: List<T>,
    isEndOfList: Boolean,
    noinline onLoadMore: () -> Unit,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable (item: T) -> Unit
) {
    itemsIndexed(
        items = items,
        key = key?.let { keySelector -> { _, item -> keySelector(item) } }
    ) { index, item ->

        itemContent(item)

        // Detect bottom logic
        if (index == items.lastIndex && !isEndOfList) {
            LaunchedEffect(Unit) {
                onLoadMore()
            }
        }
    }

    // Render bottom Footer
    if (items.isNotEmpty()) {
        item(
            key = "footer",
            // Make the Footer span all columns (FullLine)
            span = StaggeredGridItemSpan.FullLine
        ) {
            if (isEndOfList) {
                EndOfListFooter()
            } else {
                LoadingFooter()
            }
        }
    }
}

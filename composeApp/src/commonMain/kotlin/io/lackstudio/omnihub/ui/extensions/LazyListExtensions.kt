package io.lackstudio.omnihub.ui.extensions

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.lackstudio.omnihub.ui.components.EndOfListFooter
import io.lackstudio.omnihub.ui.components.LoadingFooter

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
 */
inline fun <T> LazyListScope.pagingItems(
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

        // Render content
        itemContent(item)

        // Only trigger load more when "not yet at the end"
        if (index == items.lastIndex && !isEndOfList) {
            LaunchedEffect(Unit) {
                onLoadMore()
            }
        }
    }

    // Render bottom Footer (decide what to display based on state)
    if (items.isNotEmpty()) {
        if (isEndOfList) {
            // ✅ Case A: Already at the end -> Show "No more data"
            item(key = "footer_end_of_list") {
                EndOfListFooter()
            }
        } else {
            // ✅ Case B: Not yet at the end -> Show "Loading Spinner"
            // When the user scrolls here, they will see the spinner, and the LaunchedEffect above will be triggered simultaneously
            item(key = "footer_loading") {
                LoadingFooter()
            }
        }
    }
}

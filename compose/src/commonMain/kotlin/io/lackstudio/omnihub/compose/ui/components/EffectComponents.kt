package io.lackstudio.omnihub.compose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.lackstudio.omnihub.compose.utils.logging.rememberLogger
import io.lackstudio.omnifeed.ui.state.AppUiState

/**
 * Generic error state monitor.
 * Automatically filters out non-Error states and only logs Error states.
 *
 * @param tag The tag for logging, defaults to "ErrorMonitor".
 * @param states List of states to monitor, formatted as "Log Prefix" to State.
 */
@Composable
fun MonitorErrorStates(
    tag: String = "ErrorMonitor",
    vararg states: Pair<String, AppUiState<*>>
) {
    // Obtain Logger internally, no need to pass it from outside.
    val logger = rememberLogger(tag)

    states.forEach { (name, state) ->
        LaunchedEffect(state) {
            if (state is AppUiState.Error) {
                logger.e { "$name error: ${state.message}" }
            }
        }
    }
}

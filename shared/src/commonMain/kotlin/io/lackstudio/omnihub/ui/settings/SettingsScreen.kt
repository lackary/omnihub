package io.lackstudio.omnihub.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lackstudio.omnihub.shared.BuildKonfig.APP_BUILD_NUMBER
import io.lackstudio.omnihub.shared.BuildKonfig.APP_VERSION
import io.lackstudio.omnihub.ui.navigation.Feature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToFeature: (Feature) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") } // "Account" title to indicate current screen
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Content padding to avoid overlap with TopAppBar
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
        ) {
            // ... User profile information at the top ...
            Text("About", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.weight(1f)) // Push content to the bottom

            // --- Industry standard simple approach (Footer) ---
            VersionFooter(
                versionName = APP_VERSION,
                buildNumber = APP_BUILD_NUMBER
            )

            Spacer(modifier = Modifier.height(24.dp)) // Bottom spacing
        }
    }

}

@Composable
fun VersionFooter(
    versionName: String = "1.0.0",
    buildNumber: String = "1",
) {
    var showDetails by remember { mutableStateOf(false) }
    var clickCount by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = if (showDetails) {
                "v$versionName+$buildNumber"
            } else {
                "v$versionName"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clickable {
                clickCount++
                if (clickCount >= 5) {
                    showDetails = !showDetails
                    clickCount = 0
                }
            }
        )

        if (showDetails) {
            Text(
                text = "Debug Mode Enabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Add this function specifically for preview
@Preview
@Composable
fun AccountScreenPreview() {
    SettingsScreen (
        onNavigateToFeature = {} // Provide an empty lambda to satisfy parameter requirements
    )
}

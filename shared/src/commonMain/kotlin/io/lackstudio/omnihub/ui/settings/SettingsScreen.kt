package io.lackstudio.omnihub.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.text.font.FontWeight
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
        modifier = Modifier
            .widthIn(max = 600.dp) // Settings can be a bit wider
            .fillMaxWidth(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... User profile information at the top ...
            Text("About", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.weight(1f))

            VersionFooter(
                versionName = APP_VERSION,
                buildNumber = APP_BUILD_NUMBER
            )

            Spacer(modifier = Modifier.height(24.dp))
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

@Preview
@Composable
fun AccountScreenPreview() {
    SettingsScreen (
        onNavigateToFeature = {}
    )
}

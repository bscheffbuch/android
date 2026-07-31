package io.homeassistant.companion.android.settings.developer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview

class DeveloperSettingsScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Developer settings with every optional row visible`() {
        HAThemeForPreview {
            DeveloperSettingsContent(
                uiState = DeveloperSettingsUiState(
                    locationTrackingVisible = true,
                    threadDebugVisible = true,
                    webViewClearCacheVisible = true,
                ),
                onShowLogsClicked = {},
                onLocationTrackingClicked = {},
                onRemoteDebuggingToggled = {},
                onThreadDebugClicked = {},
                onClearWebViewCacheClicked = {},
                onDismissThreadDebugResult = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Developer settings with a thread debug result dialog shown`() {
        HAThemeForPreview {
            DeveloperSettingsContent(
                uiState = DeveloperSettingsUiState(
                    threadDebugVisible = true,
                    threadDebugResult = ThreadDebugResult(
                        message = "Home Assistant and this device use the same network",
                        success = true,
                    ),
                ),
                onShowLogsClicked = {},
                onLocationTrackingClicked = {},
                onRemoteDebuggingToggled = {},
                onThreadDebugClicked = {},
                onClearWebViewCacheClicked = {},
                onDismissThreadDebugResult = {},
            )
        }
    }
}

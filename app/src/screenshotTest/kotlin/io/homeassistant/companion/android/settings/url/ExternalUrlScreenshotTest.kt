package io.homeassistant.companion.android.settings.url

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.url.views.ExternalUrlView

class ExternalUrlScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `External url view with cloud enabled`() {
        HAThemeForPreview {
            ExternalUrlView(
                canUseCloud = true,
                useCloud = true,
                externalUrl = "https://home.example.com:8123/",
                onUseCloudToggle = {},
                onExternalUrlSaved = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `External url view with cloud available but disabled`() {
        HAThemeForPreview {
            ExternalUrlView(
                canUseCloud = true,
                useCloud = false,
                externalUrl = "https://home.example.com:8123/",
                onUseCloudToggle = {},
                onExternalUrlSaved = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `External url view without cloud availability`() {
        HAThemeForPreview {
            ExternalUrlView(
                canUseCloud = false,
                useCloud = false,
                externalUrl = "https://home.example.com:8123/",
                onUseCloudToggle = {},
                onExternalUrlSaved = {},
            )
        }
    }
}

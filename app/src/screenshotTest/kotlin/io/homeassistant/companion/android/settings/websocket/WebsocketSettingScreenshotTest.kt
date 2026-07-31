package io.homeassistant.companion.android.settings.websocket

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.database.settings.WebsocketSetting
import io.homeassistant.companion.android.settings.websocket.views.WebsocketSettingView

class WebsocketSettingScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Websocket setting with always selected and unrestricted background access`() {
        HAThemeForPreview {
            WebsocketSettingView(
                websocketSetting = WebsocketSetting.ALWAYS,
                unrestrictedBackgroundAccess = true,
                hasWifi = true,
                onSettingChanged = {},
                onBackgroundAccessTapped = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Websocket setting with screen on selected and restricted background access`() {
        HAThemeForPreview {
            WebsocketSettingView(
                websocketSetting = WebsocketSetting.SCREEN_ON,
                unrestrictedBackgroundAccess = false,
                hasWifi = true,
                onSettingChanged = {},
                onBackgroundAccessTapped = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Websocket setting with never selected and no wifi`() {
        HAThemeForPreview {
            WebsocketSettingView(
                websocketSetting = WebsocketSetting.NEVER,
                unrestrictedBackgroundAccess = false,
                hasWifi = false,
                onSettingChanged = {},
                onBackgroundAccessTapped = {},
            )
        }
    }
}

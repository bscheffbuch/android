package io.homeassistant.companion.android.settings.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.notification.views.NotificationChannelRow

class NotificationChannelScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Notification channel row for server created channel with delete icon`() {
        HAThemeForPreview {
            NotificationChannelRow(
                name = "Server-created channel",
                canDelete = true,
                onEditClicked = {},
                onDeleteClicked = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Notification channel row for app created channel without delete icon`() {
        HAThemeForPreview {
            NotificationChannelRow(
                name = "App-created channel",
                canDelete = false,
                onEditClicked = {},
                onDeleteClicked = {},
            )
        }
    }
}

package io.homeassistant.companion.android.settings.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.notification.views.LoadNotification
import io.homeassistant.companion.android.util.notificationItem

class NotificationDetailScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Notification detail view for a received notification`() {
        HAThemeForPreview {
            LoadNotification(notification = notificationItem)
        }
    }
}

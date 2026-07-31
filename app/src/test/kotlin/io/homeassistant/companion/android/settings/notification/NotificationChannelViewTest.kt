package io.homeassistant.companion.android.settings.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.util.CHANNEL_GENERAL
import io.homeassistant.companion.android.settings.notification.views.NotificationChannelView
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [Build.VERSION_CODES.Q])
@HiltAndroidTest
class NotificationChannelViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun `Given app created and server created channels then only server created channel shows delete icon`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val notificationManager = app.getSystemService<NotificationManager>()!!
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_GENERAL, "App channel", NotificationManager.IMPORTANCE_DEFAULT),
        )
        notificationManager.createNotificationChannel(
            NotificationChannel("server_channel", "Server channel", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val viewModel = NotificationViewModel(app)

        composeTestRule.setContent {
            HAThemeForPreview {
                NotificationChannelView(notificationViewModel = viewModel)
            }
        }

        composeTestRule.onAllNodesWithContentDescription(composeTestRule.stringResource(commonR.string.delete_channel))
            .assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription(composeTestRule.stringResource(commonR.string.edit_channel))
            .assertCountEquals(2)
    }
}

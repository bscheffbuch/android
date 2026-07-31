package io.homeassistant.companion.android.settings.websocket.views

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.database.settings.WebsocketSetting
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class WebsocketSettingViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private var selectedSetting: WebsocketSetting? = null
    private var backgroundAccessTapped = false

    private fun setContent(
        websocketSetting: WebsocketSetting,
        unrestrictedBackgroundAccess: Boolean = true,
        hasWifi: Boolean = true,
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                WebsocketSettingView(
                    websocketSetting = websocketSetting,
                    unrestrictedBackgroundAccess = unrestrictedBackgroundAccess,
                    hasWifi = hasWifi,
                    onSettingChanged = { selectedSetting = it },
                    onBackgroundAccessTapped = { backgroundAccessTapped = true },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given the never setting selected when the always option is clicked then onSettingChanged is invoked with always`() {
        setContent(WebsocketSetting.NEVER)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_setting_always))
            .performScrollTo()
            .performClick()

        assertEquals(WebsocketSetting.ALWAYS, selectedSetting)
    }

    @Test
    fun `Given hasWifi true when composed then the home wifi option is shown`() {
        setContent(WebsocketSetting.NEVER, hasWifi = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_setting_home_wifi))
            .assertExists()
    }

    @Test
    fun `Given hasWifi false when composed then the home wifi option is not shown`() {
        setContent(WebsocketSetting.NEVER, hasWifi = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_setting_home_wifi))
            .assertDoesNotExist()
    }

    @Test
    fun `Given the home wifi option shown when clicked then onSettingChanged is invoked with home wifi`() {
        setContent(WebsocketSetting.NEVER, hasWifi = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_setting_home_wifi))
            .performScrollTo()
            .performClick()

        assertEquals(WebsocketSetting.HOME_WIFI, selectedSetting)
    }

    @Test
    fun `Given background access not unrestricted and setting not never when composed then the warning banner is shown`() {
        setContent(WebsocketSetting.ALWAYS, unrestrictedBackgroundAccess = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_notification_backgroundaccess))
            .assertExists()
    }

    @Test
    fun `Given background access unrestricted when composed then the warning banner is not shown`() {
        setContent(WebsocketSetting.ALWAYS, unrestrictedBackgroundAccess = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_notification_backgroundaccess))
            .assertDoesNotExist()
    }

    @Test
    fun `Given the warning banner action when clicked then onBackgroundAccessTapped is invoked`() {
        setContent(WebsocketSetting.ALWAYS, unrestrictedBackgroundAccess = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.allow))
            .performScrollTo()
            .performClick()

        assertEquals(true, backgroundAccessTapped)
    }

    @Test
    fun `Given the never setting selected when composed then the warning banner is not shown regardless of background access`() {
        setContent(WebsocketSetting.NEVER, unrestrictedBackgroundAccess = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.websocket_notification_backgroundaccess))
            .assertDoesNotExist()
    }
}

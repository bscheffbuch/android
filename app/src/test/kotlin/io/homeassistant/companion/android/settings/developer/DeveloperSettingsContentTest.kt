package io.homeassistant.companion.android.settings.developer

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
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
class DeveloperSettingsContentTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private fun setContent(
        uiState: DeveloperSettingsUiState,
        onShowLogsClicked: () -> Unit = {},
        onLocationTrackingClicked: () -> Unit = {},
        onRemoteDebuggingToggled: (Boolean) -> Unit = {},
        onThreadDebugClicked: () -> Unit = {},
        onClearWebViewCacheClicked: () -> Unit = {},
        onDismissThreadDebugResult: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                DeveloperSettingsContent(
                    uiState = uiState,
                    onShowLogsClicked = onShowLogsClicked,
                    onLocationTrackingClicked = onLocationTrackingClicked,
                    onRemoteDebuggingToggled = onRemoteDebuggingToggled,
                    onThreadDebugClicked = onThreadDebugClicked,
                    onClearWebViewCacheClicked = onClearWebViewCacheClicked,
                    onDismissThreadDebugResult = onDismissThreadDebugResult,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given optional rows hidden when composed then only show logs and remote debugging are shown`() {
        setContent(
            uiState = DeveloperSettingsUiState(
                locationTrackingVisible = false,
                threadDebugVisible = false,
                webViewClearCacheVisible = false,
            ),
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.show_share_logs)).assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.remote_debugging)).assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.location_tracking))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.thread_debug)).assertDoesNotExist()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.clear_webview_cache))
            .assertDoesNotExist()
    }

    @Test
    fun `Given all optional rows visible when composed then every row is shown`() {
        setContent(
            uiState = DeveloperSettingsUiState(
                locationTrackingVisible = true,
                threadDebugVisible = true,
                webViewClearCacheVisible = true,
            ),
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.location_tracking)).assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.thread_debug)).assertExists()
        composeTestRule.onNodeWithTag(DEVELOPER_SETTINGS_LIST_TEST_TAG)
            .performScrollToNode(hasText(composeTestRule.stringResource(commonR.string.clear_webview_cache)))
    }

    @Test
    fun `Given the show logs row when clicked then onShowLogsClicked is invoked`() {
        var clicked = false
        setContent(uiState = DeveloperSettingsUiState(), onShowLogsClicked = { clicked = true })

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.show_share_logs)).performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `Given location tracking visible when clicked then onLocationTrackingClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = DeveloperSettingsUiState(locationTrackingVisible = true),
            onLocationTrackingClicked = { clicked = true },
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.location_tracking)).performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `Given thread debug visible when clicked then onThreadDebugClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = DeveloperSettingsUiState(threadDebugVisible = true),
            onThreadDebugClicked = { clicked = true },
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.thread_debug)).performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `Given cache clear visible when clicked then onClearWebViewCacheClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = DeveloperSettingsUiState(webViewClearCacheVisible = true),
            onClearWebViewCacheClicked = { clicked = true },
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.clear_webview_cache)).performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `Given remote debugging row when clicked then onRemoteDebuggingToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(
            uiState = DeveloperSettingsUiState(remoteDebuggingEnabled = false),
            onRemoteDebuggingToggled = { toggledTo = it },
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.remote_debugging)).performClick()

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given a thread debug result when composed then the result dialog shows the message`() {
        setContent(
            uiState = DeveloperSettingsUiState(
                threadDebugResult = ThreadDebugResult(message = "Networks match", success = true),
            ),
        )

        composeTestRule.onNodeWithText("Networks match", substring = true).assertExists()
    }

    @Test
    fun `Given a shown thread debug result when the ok button is clicked then onDismissThreadDebugResult is invoked`() {
        var dismissed = false
        setContent(
            uiState = DeveloperSettingsUiState(
                threadDebugResult = ThreadDebugResult(message = "Networks match", success = true),
            ),
            onDismissThreadDebugResult = { dismissed = true },
        )

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.ok)).performClick()

        assertEquals(true, dismissed)
    }
}

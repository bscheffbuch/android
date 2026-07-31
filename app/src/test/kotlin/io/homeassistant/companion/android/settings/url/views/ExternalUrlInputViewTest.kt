package io.homeassistant.companion.android.settings.url.views

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ExternalUrlInputViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private val savedUrl = "https://home.example.com:8123/"

    @Test
    fun `Given input matching the saved url then update button is not shown`() {
        composeTestRule.setContent {
            HAThemeForPreview {
                ExternalUrlInputView(url = savedUrl, focusRequester = remember { FocusRequester() }, onSaveUrl = {})
            }
        }

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.update)).assertDoesNotExist()
    }

    @Test
    fun `Given a new valid url when update is clicked then onSaveUrl is called with the normalized url`() {
        var savedValue: String? = null

        composeTestRule.setContent {
            HAThemeForPreview {
                ExternalUrlInputView(url = savedUrl, focusRequester = remember { FocusRequester() }, onSaveUrl = { savedValue = it })
            }
        }

        composeTestRule.onNodeWithText(savedUrl).performTextReplacement("https://new.example.com:8123")
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.update)).assertIsDisplayed().performClick()

        assertEquals("https://new.example.com:8123/", savedValue)
    }

    @Test
    fun `Given an invalid url when update is clicked then an error is shown and onSaveUrl is not called`() {
        var savedValue: String? = null

        composeTestRule.setContent {
            HAThemeForPreview {
                ExternalUrlInputView(url = savedUrl, focusRequester = remember { FocusRequester() }, onSaveUrl = { savedValue = it })
            }
        }

        composeTestRule.onNodeWithText(savedUrl).performTextReplacement("not a url")
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.update)).assertIsDisplayed().performClick()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.url_parse_error)).assertIsDisplayed()
        assertNull(savedValue)
    }
}

package io.homeassistant.companion.android.settings.url.views

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ExternalUrlCloudViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun `Given cloud disabled when row clicked then onUseCloudToggle is called with true`() {
        var toggledTo: Boolean? = null

        composeTestRule.setContent {
            HAThemeForPreview {
                ExternalUrlCloudView(useCloud = false, onUseCloudToggle = { toggledTo = it })
            }
        }

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.input_cloud)).performClick()

        assertTrue(toggledTo == true)
    }

    @Test
    fun `Given cloud enabled when row clicked then onUseCloudToggle is called with false`() {
        var toggledTo: Boolean? = null

        composeTestRule.setContent {
            HAThemeForPreview {
                ExternalUrlCloudView(useCloud = true, onUseCloudToggle = { toggledTo = it })
            }
        }

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.input_cloud)).performClick()

        assertTrue(toggledTo == false)
    }
}

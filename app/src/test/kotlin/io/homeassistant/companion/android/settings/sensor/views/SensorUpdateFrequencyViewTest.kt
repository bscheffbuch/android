package io.homeassistant.companion.android.settings.sensor.views

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.database.settings.SensorUpdateFrequencySetting
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
class SensorUpdateFrequencyViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private var selectedSetting: SensorUpdateFrequencySetting? = null

    private fun setContent(sensorUpdateFrequency: SensorUpdateFrequencySetting) {
        composeTestRule.setContent {
            HAThemeForPreview {
                SensorUpdateFrequencyView(
                    sensorUpdateFrequency = sensorUpdateFrequency,
                    onSettingChanged = { selectedSetting = it },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given the normal setting selected when the fast while charging option is clicked then onSettingChanged is invoked with fast while charging`() {
        setContent(SensorUpdateFrequencySetting.NORMAL)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.sensor_update_frequency_fast_charging))
            .performClick()

        assertEquals(SensorUpdateFrequencySetting.FAST_WHILE_CHARGING, selectedSetting)
    }

    @Test
    fun `Given the normal setting selected when the fast always option is clicked then onSettingChanged is invoked with fast always`() {
        setContent(SensorUpdateFrequencySetting.NORMAL)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.sensor_update_frequency_fast_always))
            .performClick()

        assertEquals(SensorUpdateFrequencySetting.FAST_ALWAYS, selectedSetting)
    }

    @Test
    fun `Given the fast always setting selected when the normal option is clicked then onSettingChanged is invoked with normal`() {
        setContent(SensorUpdateFrequencySetting.FAST_ALWAYS)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.sensor_update_frequency_normal))
            .performClick()

        assertEquals(SensorUpdateFrequencySetting.NORMAL, selectedSetting)
    }
}

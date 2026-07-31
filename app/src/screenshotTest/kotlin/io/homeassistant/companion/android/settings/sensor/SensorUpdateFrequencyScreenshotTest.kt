package io.homeassistant.companion.android.settings.sensor

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.database.settings.SensorUpdateFrequencySetting
import io.homeassistant.companion.android.settings.sensor.views.SensorUpdateFrequencyView

class SensorUpdateFrequencyScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Sensor update frequency with normal selected`() {
        HAThemeForPreview {
            SensorUpdateFrequencyView(
                sensorUpdateFrequency = SensorUpdateFrequencySetting.NORMAL,
                onSettingChanged = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Sensor update frequency with fast always selected`() {
        HAThemeForPreview {
            SensorUpdateFrequencyView(
                sensorUpdateFrequency = SensorUpdateFrequencySetting.FAST_ALWAYS,
                onSettingChanged = {},
            )
        }
    }
}

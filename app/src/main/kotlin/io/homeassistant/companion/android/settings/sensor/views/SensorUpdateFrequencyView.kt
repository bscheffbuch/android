package io.homeassistant.companion.android.settings.sensor.views

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HARadioGroup
import io.homeassistant.companion.android.common.compose.composable.RadioOption
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.util.CHANNEL_SENSOR_WORKER
import io.homeassistant.companion.android.database.settings.SensorUpdateFrequencySetting
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.compose.InfoNotification
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@Composable
fun SensorUpdateFrequencyView(
    sensorUpdateFrequency: SensorUpdateFrequencySetting,
    onSettingChanged: (SensorUpdateFrequencySetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.verticalScroll(scrollState)) {
        Column(
            modifier = Modifier
                .padding(safeBottomPaddingValues(applyHorizontal = false))
                .padding(all = HADimens.SPACE4),
        ) {
            Text(
                text = stringResource(R.string.sensor_update_frequency_description),
                style = HATextStyle.Body,
                color = LocalHAColorScheme.current.colorTextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = HADimens.SPACE4),
            )
            HARadioGroup(
                options = listOf(
                    RadioOption(
                        selectionKey = SensorUpdateFrequencySetting.NORMAL,
                        headline = stringResource(R.string.sensor_update_frequency_normal),
                    ),
                    RadioOption(
                        selectionKey = SensorUpdateFrequencySetting.FAST_WHILE_CHARGING,
                        headline = stringResource(R.string.sensor_update_frequency_fast_charging),
                    ),
                    RadioOption(
                        selectionKey = SensorUpdateFrequencySetting.FAST_ALWAYS,
                        headline = stringResource(R.string.sensor_update_frequency_fast_always),
                    ),
                ),
                selectionKey = sensorUpdateFrequency,
                onSelect = { onSettingChanged(it.selectionKey) },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                HomeAssistantAppTheme {
                    InfoNotification(
                        infoString = R.string.sensor_update_notification,
                        channelId = CHANNEL_SENSOR_WORKER,
                        buttonString = R.string.sensor_worker_notification_channel,
                    )
                }
            }
        }
    }
}

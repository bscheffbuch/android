package io.homeassistant.companion.android.settings.sensor.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.compose.Image
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAHorizontalDivider
import io.homeassistant.companion.android.common.compose.composable.HASettingsRow
import io.homeassistant.companion.android.common.compose.composable.HASettingsSubheader
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.sensors.SensorManager
import io.homeassistant.companion.android.common.sensors.id
import io.homeassistant.companion.android.database.sensor.Sensor
import io.homeassistant.companion.android.settings.sensor.SensorSettingsViewModel
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SensorListView(
    viewModel: SensorSettingsViewModel,
    onSensorClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalHAColorScheme.current
    LazyColumn(
        modifier = modifier,
        contentPadding = safeBottomPaddingValues(applyHorizontal = false),
    ) {
        viewModel.allSensors.filter { it.value.isNotEmpty() }.forEach { (manager, currentSensors) ->
            stickyHeader(
                key = manager.id(),
            ) {
                if (currentSensors.any()) {
                    HASettingsSubheader(
                        text = stringResource(manager.name),
                        modifier = Modifier
                            .background(colorScheme.colorSurfaceDefault)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
            items(
                items = currentSensors,
                key = { "${manager.id()}_${it.id}" },
            ) { basicSensor ->
                SensorRow(
                    basicSensor = basicSensor,
                    dbSensor = viewModel.sensors[basicSensor.id],
                    onSensorClicked = onSensorClicked,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (currentSensors.any() && manager.id() != viewModel.allSensors.keys.last().id()) {
                item {
                    HAHorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun SensorRow(
    basicSensor: SensorManager.BasicSensor,
    dbSensor: Sensor?,
    onSensorClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorScheme = LocalHAColorScheme.current
    var iconToUse = basicSensor.statelessIcon
    if (dbSensor?.enabled == true && dbSensor.icon.isNotBlank()) {
        iconToUse = dbSensor.icon
    }
    val enabled = dbSensor?.enabled == true
    val mdiIcon = try {
        IconicsDrawable(context, "cmd-${iconToUse.split(":")[1]}").icon
    } catch (e: Exception) {
        null
    }

    HASettingsRow(
        primaryText = stringResource(basicSensor.name),
        secondaryText = if (enabled) {
            if (dbSensor?.state.isNullOrBlank()) {
                stringResource(commonR.string.enabled)
            } else {
                if (basicSensor.unitOfMeasurement.isNullOrBlank() || dbSensor?.state?.toDoubleOrNull() == null) {
                    dbSensor?.state.orEmpty()
                } else {
                    "${dbSensor.state} ${basicSensor.unitOfMeasurement}"
                }
            }
        } else {
            stringResource(commonR.string.disabled)
        },
        onClicked = { onSensorClicked(basicSensor.id) },
        modifier = modifier,
        enabled = enabled,
        icon = mdiIcon?.let { icon ->
            {
                Image(
                    asset = icon,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(
                        if (enabled) colorScheme.colorTextPrimary else colorScheme.colorTextDisabled,
                    ),
                )
            }
        },
    )
}

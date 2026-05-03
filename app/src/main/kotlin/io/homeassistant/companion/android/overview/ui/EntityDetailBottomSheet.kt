package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getLightBrightness
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsLightBrightness

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityDetailBottomSheet(
    entity: Entity,
    onDismiss: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalHAColorScheme.current
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId

    val initialBrightness = entity.getLightBrightness()?.value ?: if (isOn) 100f else 0f
    var brightness by remember(entity.entityId) { mutableFloatStateOf(initialBrightness) }

    val ignoredKeys = setOf("friendly_name", "supported_features", "supported_color_modes",
        "color_mode", "entity_picture", "icon")
    val displayAttributes = entity.attributes
        .filterKeys { it !in ignoredKeys }
        .entries
        .sortedBy { it.key }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.colorSurfaceDefault,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (entity.domain == "light") {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = if (isOn) Color(0xFFFFA000) else colors.colorTextDisabled,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Icon(
                        imageVector = EntityIconProvider.iconForDomain(entity.domain, isOn),
                        contentDescription = null,
                        tint = if (isOn) colors.colorFillPrimaryLoudResting else colors.colorTextDisabled,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = friendlyName,
                        color = colors.colorTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = entity.state.replaceFirstChar { it.uppercaseChar() },
                        color = colors.colorTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }

            if (entity.domain == "light" && entity.supportsLightBrightness()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(commonR.string.overview_brightness),
                    color = colors.colorTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    onValueChangeFinished = { onBrightnessChange(brightness) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFA000),
                        activeTrackColor = Color(0xFFFFA000),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${brightness.toInt()}%",
                    color = colors.colorTextSecondary,
                    fontSize = 12.sp,
                )
            }

            if (displayAttributes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.colorBorderNeutralQuiet)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(commonR.string.overview_attributes),
                    color = colors.colorTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(displayAttributes) { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = key.replace('_', ' ').replaceFirstChar { it.uppercaseChar() },
                                color = colors.colorTextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = value?.toString() ?: "",
                                color = colors.colorTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

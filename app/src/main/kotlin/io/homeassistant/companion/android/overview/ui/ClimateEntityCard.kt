package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getClimateCurrentTemperature
import io.homeassistant.companion.android.common.data.integration.getClimateTargetTemperature
import io.homeassistant.companion.android.common.data.integration.getClimateTemperatureStep
import io.homeassistant.companion.android.common.data.integration.isActive
import java.time.LocalDateTime
import kotlin.math.round

private val OnStateBorderWidth = 2.dp

/**
 * A card for climate entities.
 *
 * Gestures:
 * - Tap: cycle to the next supported HVAC mode (off/heat/cool/heat_cool/auto/dry/fan_only)
 * - Long press: open detail sheet
 * - Tap the trailing +/- icon buttons: adjust the target temperature, only shown when the entity
 *   supports [io.homeassistant.companion.android.common.data.integration.supportsClimateSetTemperature]
 */
@Composable
fun ClimateEntityCard(
    entity: Entity,
    onCycleHvacMode: () -> Unit,
    onSetTemperature: (temperature: Float, immediate: Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val targetTemperature = entity.getClimateTargetTemperature()
    val currentTemperature = entity.getClimateCurrentTemperature()
    val temperatureStep = entity.getClimateTemperatureStep()

    val subtitle = buildString {
        append(entity.state.replaceFirstChar { it.uppercaseChar() })
        if (currentTemperature != null) {
            append(" · ")
            append(formatTemperature(currentTemperature))
        }
    }

    val cardBg = if (isOn) colors.colorFillPrimaryQuietResting else colors.colorSurfaceLow
    val iconTint = if (isOn) colors.colorOnPrimaryNormal else colors.colorTextDisabled
    val textColor = if (isOn) colors.colorTextPrimary else colors.colorTextSecondary
    val cardShape = OverviewCardShape
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(entity.entityId) {
            detectTapGestures(
                onTap = { onCycleHvacMode() },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenDetail()
                },
            )
        }
    } else {
        Modifier
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(gestureModifier)
            .then(
                if (isOn) {
                    Modifier.border(OnStateBorderWidth, colors.colorBorderPrimaryLoud, cardShape)
                } else {
                    Modifier
                },
            ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = EntityIconProvider.iconForDomain(entity.domain, isOn),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friendlyName,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (targetTemperature != null) {
                    IconButton(
                        onClick = {
                            onSetTemperature(
                                (targetTemperature.value - temperatureStep).coerceAtLeast(targetTemperature.min),
                                true,
                            )
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = stringResource(commonR.string.overview_climate_decrease_temperature),
                            tint = iconTint,
                        )
                    }
                    Text(
                        text = formatTemperature(targetTemperature.value),
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                    IconButton(
                        onClick = {
                            onSetTemperature(
                                (targetTemperature.value + temperatureStep).coerceAtMost(targetTemperature.max),
                                true,
                            )
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(commonR.string.overview_climate_increase_temperature),
                            tint = iconTint,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTemperature(value: Float): String {
    val rounded = round(value * 10) / 10f
    val displayValue = if (rounded == rounded.toLong().toFloat()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
    return "$displayValue°"
}

private fun previewEntity(state: String, withTargetTemperature: Boolean) = Entity(
    entityId = "climate.living_room",
    state = state,
    attributes = if (withTargetTemperature) {
        mapOf(
            "friendly_name" to "Living room",
            "supported_features" to 1,
            "current_temperature" to 21.5,
            "temperature" to 22.0,
            "min_temp" to 7.0,
            "max_temp" to 35.0,
            "target_temp_step" to 0.5,
            "hvac_modes" to listOf("off", "heat", "cool"),
        )
    } else {
        mapOf("friendly_name" to "Living room", "hvac_modes" to listOf("off", "heat", "cool"))
    },
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun ClimateEntityCardHeatingPreview() {
    HAThemeForPreview {
        ClimateEntityCard(
            entity = previewEntity(state = "heat", withTargetTemperature = true),
            onCycleHvacMode = {},
            onSetTemperature = { _, _ -> },
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ClimateEntityCardOffPreview() {
    HAThemeForPreview {
        ClimateEntityCard(
            entity = previewEntity(state = "off", withTargetTemperature = false),
            onCycleHvacMode = {},
            onSetTemperature = { _, _ -> },
            onOpenDetail = {},
        )
    }
}

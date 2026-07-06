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
import androidx.compose.material.icons.rounded.Tune
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
import io.homeassistant.companion.android.common.data.integration.getHumidifierHumidityStep
import io.homeassistant.companion.android.common.data.integration.getHumidifierMode
import io.homeassistant.companion.android.common.data.integration.getHumidifierTargetHumidity
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsHumidifierModes
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val OnStateBorderWidth = 2.dp

/**
 * A card for humidifier entities.
 *
 * Gestures:
 * - Tap: toggle on/off
 * - Long press: open detail sheet
 * - Tap the trailing +/- icon buttons: adjust the target humidity, only shown when the entity
 *   currently exposes a `humidity` attribute (see
 *   [io.homeassistant.companion.android.common.data.integration.getHumidifierTargetHumidity])
 * - Tap the trailing tune icon button: cycle through the entity's available modes, only shown
 *   when the entity supports the `MODES` feature (see
 *   [io.homeassistant.companion.android.common.data.integration.supportsHumidifierModes])
 */
@Composable
fun HumidifierEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onSetHumidity: (humidity: Float, immediate: Boolean) -> Unit,
    onCycleMode: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val targetHumidity = entity.getHumidifierTargetHumidity()
    val currentHumidity = (entity.attributes["current_humidity"] as? Number)?.toFloat()
    val humidityStep = entity.getHumidifierHumidityStep()
    val supportsModes = entity.supportsHumidifierModes()
    val currentMode = entity.getHumidifierMode()

    val subtitle = buildString {
        append(entity.state.replaceFirstChar { it.uppercaseChar() })
        if (currentHumidity != null) {
            append(" · ")
            append(formatHumidity(currentHumidity))
        }
        if (supportsModes && currentMode != null) {
            append(" · ")
            append(currentMode.replaceFirstChar { it.uppercaseChar() })
        }
    }

    val cardBg = if (isOn) colors.colorFillPrimaryQuietResting else colors.colorSurfaceLow
    val iconTint = if (isOn) colors.colorOnPrimaryNormal else colors.colorTextDisabled
    val textColor = if (isOn) colors.colorTextPrimary else colors.colorTextSecondary
    val cardShape = OverviewCardShape
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(entity.entityId) {
            detectTapGestures(
                onTap = { onToggle() },
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
                if (supportsModes) {
                    IconButton(
                        onClick = onCycleMode,
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = stringResource(commonR.string.overview_humidifier_cycle_mode),
                            tint = iconTint,
                        )
                    }
                }
                if (targetHumidity != null) {
                    IconButton(
                        onClick = {
                            onSetHumidity(
                                (targetHumidity.value - humidityStep).coerceAtLeast(targetHumidity.min),
                                true,
                            )
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = stringResource(commonR.string.overview_humidifier_decrease_humidity),
                            tint = iconTint,
                        )
                    }
                    Text(
                        text = formatHumidity(targetHumidity.value),
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                    IconButton(
                        onClick = {
                            onSetHumidity(
                                (targetHumidity.value + humidityStep).coerceAtMost(targetHumidity.max),
                                true,
                            )
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(commonR.string.overview_humidifier_increase_humidity),
                            tint = iconTint,
                        )
                    }
                }
            }
        }
    }
}

private fun formatHumidity(value: Float): String = "${value.roundToInt()}%"

private fun previewEntity(state: String, withTargetHumidity: Boolean, withModes: Boolean = false) = Entity(
    entityId = "humidifier.bedroom",
    state = state,
    attributes = buildMap {
        put("friendly_name", "Bedroom humidifier")
        if (withTargetHumidity) {
            put("current_humidity", 38)
            put("humidity", 45)
            put("min_humidity", 0)
            put("max_humidity", 100)
        }
        if (withModes) {
            put("supported_features", 8)
            put("mode", "auto")
            put("available_modes", listOf("auto", "away", "boost"))
        }
    },
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun HumidifierEntityCardOnPreview() {
    HAThemeForPreview {
        HumidifierEntityCard(
            entity = previewEntity(state = "on", withTargetHumidity = true, withModes = true),
            onToggle = {},
            onSetHumidity = { _, _ -> },
            onCycleMode = {},
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HumidifierEntityCardOffPreview() {
    HAThemeForPreview {
        HumidifierEntityCard(
            entity = previewEntity(state = "off", withTargetHumidity = false),
            onToggle = {},
            onSetHumidity = { _, _ -> },
            onCycleMode = {},
            onOpenDetail = {},
        )
    }
}

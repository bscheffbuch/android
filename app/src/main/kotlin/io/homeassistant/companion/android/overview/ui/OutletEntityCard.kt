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
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Outlet
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.isActive

private val OnStateBorderWidth = 2.dp
private const val DISPLAYED_AS_LIGHT_FILL_ALPHA = 0.10f

/**
 * A card for an on/off entity in the Overview: an outlet switch by default, or any switch /
 * input_boolean the user chose to display as a lamp.
 *
 * When [displayedAsLight] is enabled (a local, per-entity display preference set from the entity
 * detail sheet) the card adopts the warmer visual treatment used for light entities and swaps its
 * outlet icon for a bulb, since these entities commonly drive lamps and read better grouped with
 * lights. Brightness is not offered — these entities are on/off only.
 *
 * Gestures:
 * - Tap: toggle on/off
 * - Long press: open detail sheet
 */
@Composable
fun OutletEntityCard(
    entity: Entity,
    displayedAsLight: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId

    val cardBg = when {
        !isOn -> colors.colorSurfaceLow
        displayedAsLight -> colors.colorFillNeutralQuietResting
        else -> colors.colorFillPrimaryQuietResting
    }
    val iconTint = when {
        !isOn -> colors.colorTextDisabled
        displayedAsLight -> colors.colorFillLightLoudResting
        else -> colors.colorOnPrimaryNormal
    }
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
                if (isOn && !displayedAsLight) {
                    Modifier.border(OnStateBorderWidth, colors.colorBorderPrimaryLoud, cardShape)
                } else {
                    Modifier
                },
            ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (isOn && displayedAsLight) {
                        drawRect(color = colors.colorFillLightLoudResting.copy(alpha = DISPLAYED_AS_LIGHT_FILL_ALPHA))
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (displayedAsLight) Icons.Rounded.Lightbulb else Icons.Rounded.Outlet,
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
                        text = entity.state.replaceFirstChar { it.uppercaseChar() },
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.rounded.PlayArrow
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
import io.homeassistant.companion.android.common.data.integration.isActive
import java.time.LocalDateTime

private val OnStateBorderWidth = 2.dp

/**
 * A card for automation entities, adding a "run now" action alongside the usual enable/disable
 * toggle.
 *
 * Gestures:
 * - Tap: toggle enabled/disabled
 * - Long press: open detail sheet
 * - Tap the trailing icon button: trigger the automation immediately, independent of its
 *   enabled/disabled state
 */
@Composable
fun AutomationEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
    onTriggerNow: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId

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
                        text = entity.state.replaceFirstChar { it.uppercaseChar() },
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onTriggerNow, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(commonR.string.overview_automation_run_now),
                        tint = iconTint,
                    )
                }
            }
        }
    }
}

private fun previewEntity(state: String) = Entity(
    entityId = "automation.morning_routine",
    state = state,
    attributes = mapOf("friendly_name" to "Morning routine"),
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun AutomationEntityCardOnPreview() {
    HAThemeForPreview {
        AutomationEntityCard(
            entity = previewEntity(state = "on"),
            onToggle = {},
            onOpenDetail = {},
            onTriggerNow = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AutomationEntityCardOffPreview() {
    HAThemeForPreview {
        AutomationEntityCard(
            entity = previewEntity(state = "off"),
            onToggle = {},
            onOpenDetail = {},
            onTriggerNow = {},
        )
    }
}

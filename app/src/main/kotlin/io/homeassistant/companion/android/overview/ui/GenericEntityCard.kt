package io.homeassistant.companion.android.overview.ui

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * A generic card for non-light entities.
 *
 * Gestures:
 * - Tap: toggle on/off
 * - Long press: open detail sheet
 */
@Composable
fun GenericEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId

    val cardBg = if (isOn) colors.colorFillPrimaryQuietResting else colors.colorSurfaceLow
    val iconTint = if (isOn) colors.colorFillPrimaryLoudResting else colors.colorTextDisabled
    val textColor = if (isOn) colors.colorTextPrimary else colors.colorTextSecondary

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(entity.entityId) {
                detectTapGestures(
                    onTap = { onToggle() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDetail()
                    },
                )
            },
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
            }
        }
    }
}

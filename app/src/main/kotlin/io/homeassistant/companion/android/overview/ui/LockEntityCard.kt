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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import java.time.LocalDateTime

private val OnStateBorderWidth = 2.dp
private const val LOCK_STATE_LOCKED = "locked"
private const val LOCK_STATE_JAMMED = "jammed"

/**
 * A card for lock entities. Unlike the generic on/off treatment, a `jammed` lock is called out
 * with a distinct warning style instead of silently looking identical to a plain unlocked lock.
 *
 * Gestures:
 * - Tap: toggle locked/unlocked
 * - Long press: open detail sheet
 */
@Composable
fun LockEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isLocked = entity.state == LOCK_STATE_LOCKED
    val isJammed = entity.state == LOCK_STATE_JAMMED
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId

    val cardBg: Color
    val iconTint: Color
    val textColor: Color
    val borderColor: Color?
    when {
        isJammed -> {
            cardBg = colors.colorFillWarningQuietResting
            iconTint = colors.colorOnWarningNormal
            textColor = colors.colorTextPrimary
            borderColor = colors.colorBorderWarningNormal
        }
        isLocked -> {
            cardBg = colors.colorFillPrimaryQuietResting
            iconTint = colors.colorOnPrimaryNormal
            textColor = colors.colorTextPrimary
            borderColor = colors.colorBorderPrimaryLoud
        }
        else -> {
            cardBg = colors.colorSurfaceLow
            iconTint = colors.colorTextDisabled
            textColor = colors.colorTextSecondary
            borderColor = null
        }
    }
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
                if (borderColor != null) {
                    Modifier.border(OnStateBorderWidth, borderColor, cardShape)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = EntityIconProvider.iconForDomain(entity.domain, isLocked),
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
                        color = if (isJammed) colors.colorOnWarningNormal else colors.colorTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun previewEntity(state: String) = Entity(
    entityId = "lock.front_door",
    state = state,
    attributes = mapOf("friendly_name" to "Front door"),
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun LockEntityCardLockedPreview() {
    HAThemeForPreview {
        LockEntityCard(
            entity = previewEntity(state = "locked"),
            onToggle = {},
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LockEntityCardUnlockedPreview() {
    HAThemeForPreview {
        LockEntityCard(
            entity = previewEntity(state = "unlocked"),
            onToggle = {},
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LockEntityCardJammedPreview() {
    HAThemeForPreview {
        LockEntityCard(
            entity = previewEntity(state = "jammed"),
            onToggle = {},
            onOpenDetail = {},
        )
    }
}

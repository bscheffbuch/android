package io.homeassistant.companion.android.overview.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
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
import io.homeassistant.companion.android.common.data.integration.getCoverPosition
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsCoverSetPosition
import io.homeassistant.companion.android.common.data.integration.supportsCoverStop
import java.time.LocalDateTime
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

private const val COVER_ACTIVE_FILL_ALPHA = 0.28f
private const val COVER_DIM_FILL_ALPHA = 0.10f

// Ensures the position fill stays visible even when a cover reports a very low or zero position
// while open, instead of disappearing to an imperceptible sliver
private const val MINIMUM_POSITION_FILL_FRACTION = 0.08f

/**
 * A card for cover entities (garage doors, blinds, curtains, etc.).
 *
 * Gestures:
 * - Tap: toggle open/closed
 * - Horizontal drag: adjust position live while sliding (only when the cover supports
 *   `set_cover_position`)
 * - Long press: open detail sheet
 * - Tap the trailing stop button (only shown when the cover supports `stop_cover`): stop the
 *   cover mid-movement
 */
@Composable
fun CoverEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onPositionChange: (percentage: Float, immediate: Boolean) -> Unit,
    onStop: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current

    val isOpen = entity.isActive()
    val supportsPosition = entity.supportsCoverSetPosition()
    val supportsStop = entity.supportsCoverStop()
    val entityPosition = entity.getCoverPosition()?.value ?: if (isOpen) 100f else 0f
    var displayPosition by remember(entity.entityId) { mutableFloatStateOf(entityPosition) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(entityPosition) {
        if (!isDragging) displayPosition = entityPosition
    }

    val animatedPosition by animateFloatAsState(targetValue = displayPosition, label = "cover_position_fill")

    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val cardBg = if (isOpen) colors.colorFillNeutralQuietResting else colors.colorSurfaceLow
    val textColor = if (isOpen) colors.colorTextPrimary else colors.colorTextSecondary
    // requireUnconsumed = true so the trailing stop IconButton (a real Composable click target)
    // can claim the down event first; otherwise tapping "stop" would also toggle the cover.
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(entity.entityId) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                var dragStarted = false
                val startX = down.position.x
                val positionAtGestureStart = displayPosition

                val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    var active = true
                    while (active) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: return@withTimeoutOrNull null
                        if (!change.pressed) {
                            active = false
                        } else {
                            val dx = change.position.x - startX
                            if (!dragStarted && abs(dx) > viewConfiguration.touchSlop) {
                                dragStarted = true
                                isDragging = true
                                return@withTimeoutOrNull true
                            }
                        }
                    }
                    false
                }

                when {
                    result == null -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDetail()
                        var waiting = true
                        while (waiting) {
                            val e = awaitPointerEvent(PointerEventPass.Main)
                            if (e.changes.all { !it.pressed }) waiting = false
                        }
                    }

                    result == false -> {
                        onToggle()
                    }

                    else -> {
                        isDragging = true
                        var active = true
                        while (active) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                active = false
                                isDragging = false
                                if (supportsPosition) {
                                    onPositionChange(displayPosition, true)
                                }
                            } else {
                                val dx = change.position.x - startX
                                if (supportsPosition) {
                                    change.consume()
                                    displayPosition = (positionAtGestureStart + dx / size.width.toFloat() * 100f)
                                        .coerceIn(0f, 100f)
                                    onPositionChange(displayPosition, false)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(gestureModifier),
        shape = OverviewCardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (isOpen || animatedPosition > 0f) {
                        if (supportsPosition) {
                            val fillFraction = (animatedPosition / 100f).coerceIn(0f, 1f)
                                .let { if (isOpen) it.coerceAtLeast(MINIMUM_POSITION_FILL_FRACTION) else it }
                            drawRect(
                                color = colors.colorFillPrimaryLoudResting.copy(alpha = COVER_ACTIVE_FILL_ALPHA),
                                size = Size(width = size.width * fillFraction, height = size.height),
                            )
                        } else {
                            drawRect(color = colors.colorFillPrimaryLoudResting.copy(alpha = COVER_DIM_FILL_ALPHA))
                        }
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = if (supportsStop) 4.dp else 16.dp,
                        top = 12.dp,
                        bottom = 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = EntityIconProvider.iconForDomain(entity.domain, isOpen),
                    contentDescription = null,
                    tint = if (isOpen) colors.colorFillPrimaryLoudResting else colors.colorTextDisabled,
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
                        text = if (isOpen && supportsPosition) {
                            "${animatedPosition.toInt()}%"
                        } else {
                            entity.state.replaceFirstChar { it.uppercaseChar() }
                        },
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (supportsStop) {
                    IconButton(onClick = onStop, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = stringResource(commonR.string.overview_cover_stop),
                            tint = if (isOpen) colors.colorFillPrimaryLoudResting else colors.colorTextDisabled,
                        )
                    }
                }
            }
        }
    }
}

private fun previewEntity(state: String, position: Int? = null, stoppable: Boolean = false) = Entity(
    entityId = "cover.garage_door",
    state = state,
    attributes = buildMap {
        put("friendly_name", "Garage door")
        var supportedFeatures = 0
        if (position != null) {
            put("current_position", position)
            supportedFeatures = supportedFeatures or 4
        }
        if (stoppable) {
            supportedFeatures = supportedFeatures or 8
        }
        put("supported_features", supportedFeatures)
    },
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun CoverEntityCardOpenPreview() {
    HAThemeForPreview {
        CoverEntityCard(
            entity = previewEntity(state = "open", position = 70, stoppable = true),
            onToggle = {},
            onPositionChange = { _, _ -> },
            onStop = {},
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CoverEntityCardClosedPreview() {
    HAThemeForPreview {
        CoverEntityCard(
            entity = previewEntity(state = "closed"),
            onToggle = {},
            onPositionChange = { _, _ -> },
            onStop = {},
            onOpenDetail = {},
        )
    }
}

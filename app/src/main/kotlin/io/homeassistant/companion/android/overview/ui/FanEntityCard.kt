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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getFanSpeed
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsFanSetSpeed
import java.time.LocalDateTime
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

private const val FAN_ACTIVE_FILL_ALPHA = 0.28f
private const val FAN_DIM_FILL_ALPHA = 0.10f

// Ensures the speed fill stays visible even when a fan reports very low or zero speed while
// on, instead of disappearing to an imperceptible sliver
private const val MINIMUM_SPEED_FILL_FRACTION = 0.08f

/**
 * A card representing a fan entity.
 *
 * Gestures:
 * - Tap: toggle on/off
 * - Horizontal drag: adjust speed live while sliding (only when the fan supports `set_percentage`)
 * - Long press: open detail sheet
 */
@Composable
fun FanEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onSpeedChange: (percentage: Float, immediate: Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current

    val isOn = entity.isActive()
    val supportsSpeed = entity.supportsFanSetSpeed()
    val entitySpeed = entity.getFanSpeed()?.value ?: if (isOn) 100f else 0f
    var displaySpeed by remember(entity.entityId) { mutableFloatStateOf(entitySpeed) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(entitySpeed) {
        if (!isDragging) displaySpeed = entitySpeed
    }

    val animatedSpeed by animateFloatAsState(targetValue = displaySpeed, label = "fan_speed_fill")

    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val cardBg = if (isOn) colors.colorFillNeutralQuietResting else colors.colorSurfaceLow
    val textColor = if (isOn) colors.colorTextPrimary else colors.colorTextSecondary
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(entity.entityId) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragStarted = false
                val startX = down.position.x
                val speedAtGestureStart = displaySpeed

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
                                if (supportsSpeed) {
                                    onSpeedChange(displaySpeed, true)
                                }
                            } else {
                                val dx = change.position.x - startX
                                if (supportsSpeed) {
                                    change.consume()
                                    displaySpeed = (speedAtGestureStart + dx / size.width.toFloat() * 100f)
                                        .coerceIn(0f, 100f)
                                    onSpeedChange(displaySpeed, false)
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
                    if (isOn || animatedSpeed > 0f) {
                        if (supportsSpeed) {
                            val fillFraction = (animatedSpeed / 100f).coerceIn(0f, 1f)
                                .let { if (isOn) it.coerceAtLeast(MINIMUM_SPEED_FILL_FRACTION) else it }
                            drawRect(
                                color = colors.colorFillPrimaryLoudResting.copy(alpha = FAN_ACTIVE_FILL_ALPHA),
                                size = Size(width = size.width * fillFraction, height = size.height),
                            )
                        } else {
                            drawRect(color = colors.colorFillPrimaryLoudResting.copy(alpha = FAN_DIM_FILL_ALPHA))
                        }
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
                    imageVector = Icons.Rounded.Air,
                    contentDescription = null,
                    tint = if (isOn) colors.colorFillPrimaryLoudResting else colors.colorTextDisabled,
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
                        text = if (isOn && supportsSpeed) {
                            "${animatedSpeed.toInt()}%"
                        } else {
                            entity.state.replaceFirstChar { it.uppercaseChar() }
                        },
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private fun previewEntity(state: String, percentage: Int? = null) = Entity(
    entityId = "fan.living_room",
    state = state,
    attributes = buildMap {
        put("friendly_name", "Living room fan")
        if (percentage != null) {
            put("percentage", percentage)
            put("percentage_step", 100.0 / 3)
            put("supported_features", 1)
        }
    },
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun FanEntityCardOnPreview() {
    HAThemeForPreview {
        FanEntityCard(
            entity = previewEntity(state = "on", percentage = 66),
            onToggle = {},
            onSpeedChange = { _, _ -> },
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FanEntityCardOffPreview() {
    HAThemeForPreview {
        FanEntityCard(
            entity = previewEntity(state = "off"),
            onToggle = {},
            onSpeedChange = { _, _ -> },
            onOpenDetail = {},
        )
    }
}

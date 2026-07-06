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
import androidx.compose.material.icons.rounded.Lightbulb
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getLightBrightness
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsLightBrightness
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

// Ensures the brightness fill stays visible even when a light reports very low or zero
// brightness while on, instead of disappearing to an imperceptible sliver
private const val MINIMUM_BRIGHTNESS_FILL_FRACTION = 0.08f

/**
 * A card representing a light entity.
 *
 * Gestures:
 * - Tap: toggle on/off
 * - Horizontal drag: adjust brightness live while sliding
 * - Long press: open detail sheet
 *
 * @param accentColor Color of the "on" brightness fill. Defaults to the standard light accent;
 * pass a light group's accent color when rendering one of that group's member entities, so the
 * member visually reads as part of the group instead of a plain standalone light.
 */
@Composable
fun LightEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalHAColorScheme.current.colorFillLightLoudResting,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current

    val isOn = entity.isActive()
    val entityBrightness = entity.getLightBrightness()?.value ?: if (isOn) 100f else 0f
    var displayBrightness by remember(entity.entityId) { mutableFloatStateOf(entityBrightness) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(entityBrightness) {
        if (!isDragging) displayBrightness = entityBrightness
    }

    val animatedBrightness by animateFloatAsState(
        targetValue = displayBrightness,
        label = "brightness_fill",
    )

    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val cardBg = if (isOn) colors.colorFillNeutralLoudResting else colors.colorSurfaceLow
    val textColor = if (isOn) colors.colorOnLightLoud else colors.colorTextSecondary
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(entity.entityId) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragStarted = false
                var yieldedToScroll = false
                val startX = down.position.x
                val startY = down.position.y
                val brightnessAtGestureStart = displayBrightness

                val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    var active = true
                    while (active) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: return@withTimeoutOrNull null
                        if (!change.pressed) {
                            active = false
                        } else {
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            if (!dragStarted) {
                                if (abs(dx) > viewConfiguration.touchSlop && abs(dx) >= abs(dy)) {
                                    dragStarted = true
                                    isDragging = true
                                    return@withTimeoutOrNull true
                                } else if (abs(dy) > viewConfiguration.touchSlop) {
                                    // Predominantly vertical movement is a list scroll, not a card
                                    // interaction — yield so the grid scrolls instead of us toggling.
                                    yieldedToScroll = true
                                    return@withTimeoutOrNull false
                                }
                            }
                        }
                    }
                    false
                }

                when {
                    yieldedToScroll -> Unit // yielded to a list scroll — neither toggle nor drag

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
                                if (entity.supportsLightBrightness()) {
                                    onBrightnessChange(displayBrightness, true)
                                }
                            } else {
                                val dx = change.position.x - startX
                                if (entity.supportsLightBrightness()) {
                                    change.consume()
                                    // Snap to the extremes at the card's physical edges so 0% and
                                    // 100% are reachable by dragging to the edge, instead of stalling
                                    // ~1% short and forcing an over-drag past the card to top out.
                                    displayBrightness = when {
                                        change.position.x >= size.width.toFloat() -> 100f
                                        change.position.x <= 0f -> 0f
                                        else -> (brightnessAtGestureStart + dx / size.width.toFloat() * 100f)
                                            .coerceIn(0f, 100f)
                                    }
                                    onBrightnessChange(displayBrightness, false)
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
                    if (isOn || animatedBrightness > 0f) {
                        if (entity.supportsLightBrightness()) {
                            val fillFraction = (animatedBrightness / 100f).coerceIn(0f, 1f)
                                .let { if (isOn) it.coerceAtLeast(MINIMUM_BRIGHTNESS_FILL_FRACTION) else it }
                            drawRect(
                                color = accentColor,
                                size = Size(width = size.width * fillFraction, height = size.height),
                            )
                        } else {
                            drawRect(color = accentColor)
                        }
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = if (isOn) colors.colorOnLightLoud else colors.colorTextDisabled,
                    modifier = Modifier.size(24.dp),
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
                        text = if (isOn && entity.supportsLightBrightness()) {
                            // Round (not truncate) to match Home Assistant's frontend, so a light at
                            // its practical max (HA brightness 254 = 99.6%) reads "100%", not "99%".
                            "${animatedBrightness.roundToInt()}%"
                        } else if (isOn) {
                            "On"
                        } else {
                            "Off"
                        },
                        color = textColor,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

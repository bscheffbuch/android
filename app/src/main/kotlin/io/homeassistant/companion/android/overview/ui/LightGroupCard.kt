package io.homeassistant.companion.android.overview.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalDensity
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
import io.homeassistant.companion.android.overview.OverviewLightGroup
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

// Ensures the brightness fill stays visible even when the group reports very low or zero
// average brightness while on, instead of disappearing to an imperceptible sliver
private const val MINIMUM_BRIGHTNESS_FILL_FRACTION = 0.08f

@Composable
fun LightGroupCard(
    group: OverviewLightGroup,
    entities: List<Entity>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalHAColorScheme.current.colorFillLightLoudResting,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val expandHitWidthPx = with(LocalDensity.current) { 56.dp.toPx() }
    val isOn = entities.any { it.isActive() }
    val supportsBrightness = entities.any { it.supportsLightBrightness() }
    val entityBrightness = entities
        .mapNotNull { it.getLightBrightness()?.value }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
        ?: if (isOn) 100f else 0f
    var displayBrightness by remember(group.id) { mutableFloatStateOf(entityBrightness) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(entityBrightness) {
        if (!isDragging) displayBrightness = entityBrightness
    }

    val animatedBrightness by animateFloatAsState(
        targetValue = displayBrightness,
        label = "group_brightness_fill",
    )
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(group.id, isExpanded) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.x >= size.width - expandHitWidthPx) {
                    // Toggle on release (tap up), not on press, so it behaves like a normal button:
                    // a press that turns into a scroll, or a finger that slides off the chevron before
                    // lifting, leaves the group as it was instead of flipping it the instant it is
                    // touched. The down is left unconsumed so a scroll starting here still scrolls.
                    var released = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        val movedBeyondSlop =
                            abs(change.position.x - down.position.x) > viewConfiguration.touchSlop ||
                                abs(change.position.y - down.position.y) > viewConfiguration.touchSlop
                        if (movedBeyondSlop) break
                        if (!change.pressed) {
                            change.consume()
                            released = true
                            break
                        }
                    }
                    if (released) onExpandedChange(!isExpanded)
                    return@awaitEachGesture
                }
                val startX = down.position.x
                val startY = down.position.y
                val brightnessAtGestureStart = displayBrightness
                var yieldedToScroll = false

                // Classify the gesture: true = brightness drag, false = tap (toggle), null = long
                // press (open detail). The long-press timeout is applied per event *gap* rather than
                // once for the whole gesture, so it only fires when the finger is genuinely held
                // still — a slow or slightly-delayed horizontal slide keeps producing move events
                // that restart the timeout and so still adjusts brightness instead of being hijacked
                // into opening the detail sheet.
                var result: Boolean? = false
                var deciding = true
                while (deciding) {
                    val event = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        awaitPointerEvent(PointerEventPass.Main)
                    }
                    val change = event?.changes?.firstOrNull()
                    when {
                        // No pointer event for a full timeout => finger held still => long press.
                        event == null -> {
                            result = null
                            deciding = false
                        }

                        change == null || !change.pressed -> {
                            result = false // released without dragging => tap => toggle
                            deciding = false
                        }

                        else -> {
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            if (abs(dx) > viewConfiguration.touchSlop && abs(dx) >= abs(dy)) {
                                isDragging = true
                                result = true // horizontal drag => brightness
                                deciding = false
                            } else if (abs(dy) > viewConfiguration.touchSlop) {
                                // Predominantly vertical movement is a list scroll, not a card
                                // interaction — yield so the grid scrolls instead of us toggling.
                                yieldedToScroll = true
                                result = false
                                deciding = false
                            }
                            // Sub-slop movement: keep deciding; the next iteration restarts the
                            // long-press timeout, so a moving finger never trips it.
                        }
                    }
                }

                when {
                    yieldedToScroll -> Unit // yielded to a list scroll — neither toggle nor drag

                    result == null -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDetail()
                        var waiting = true
                        while (waiting) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.changes.all { !it.pressed }) waiting = false
                        }
                    }

                    result == false -> onToggle()

                    else -> {
                        isDragging = true
                        var active = true
                        while (active) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                active = false
                                isDragging = false
                                if (supportsBrightness) onBrightnessChange(displayBrightness, true)
                            } else if (supportsBrightness) {
                                change.consume()
                                val dx = change.position.x - startX
                                // Snap to the extremes at the card's physical edges so 0% and 100%
                                // are reachable by dragging to the edge, instead of stalling ~1%
                                // short and forcing an over-drag past the card to top out.
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
    } else {
        Modifier
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(gestureModifier),
        shape = OverviewCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isOn) colors.colorFillNeutralLoudResting else colors.colorSurfaceLow,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (isOn || animatedBrightness > 0f) {
                        val fillFraction = (animatedBrightness / 100f).coerceIn(0f, 1f)
                            .let { if (isOn) it.coerceAtLeast(MINIMUM_BRIGHTNESS_FILL_FRACTION) else it }
                        drawRect(
                            color = accentColor,
                            size = Size(width = size.width * fillFraction, height = size.height),
                        )
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
                        text = group.name,
                        color = if (isOn) colors.colorOnLightLoud else colors.colorTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isOn && supportsBrightness) {
                            // Round (not truncate) to match Home Assistant's frontend, so a light at
                            // its practical max (HA brightness 254 = 99.6%) reads "100%", not "99%".
                            "${animatedBrightness.roundToInt()}%"
                        } else if (isOn) {
                            "On"
                        } else {
                            "Off"
                        },
                        color = if (isOn) colors.colorOnLightLoud else colors.colorTextSecondary,
                        fontSize = 16.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = Color.Black.copy(alpha = if (isOn) 0.18f else 0.05f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Rounded.KeyboardArrowUp
                        } else {
                            Icons.Rounded.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = if (isOn) colors.colorOnLightLoud else colors.colorTextSecondary,
                    )
                }
            }
        }
    }
}

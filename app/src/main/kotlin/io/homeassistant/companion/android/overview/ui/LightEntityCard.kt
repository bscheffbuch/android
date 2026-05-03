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
import kotlinx.coroutines.withTimeoutOrNull

private val LightActiveColor = Color(0xFFFFA000)
private val LightActiveFill = Color(0xFFFFA000).copy(alpha = 0.28f)
private val LightDimFill = Color(0xFFFFA000).copy(alpha = 0.10f)

/**
 * A card representing a light entity.
 *
 * Gestures:
 * - Tap: toggle on/off
 * - Horizontal drag: adjust brightness (committed on release)
 * - Long press: open detail sheet
 */
@Composable
fun LightEntityCard(
    entity: Entity,
    onToggle: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
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
    val cardBg = if (isOn) colors.colorFillNeutralQuietResting else colors.colorSurfaceLow
    val textColor = if (isOn) colors.colorTextPrimary else colors.colorTextSecondary

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(entity.entityId) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragStarted = false
                    val startX = down.position.x
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
                            // Long press
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenDetail()
                            var waiting = true
                            while (waiting) {
                                val e = awaitPointerEvent(PointerEventPass.Main)
                                if (e.changes.all { !it.pressed }) waiting = false
                            }
                        }

                        result == false -> {
                            // Tap
                            onToggle()
                        }

                        else -> {
                            // Drag detected — continue collecting drag events
                            isDragging = true
                            var active = true
                            while (active) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    active = false
                                    isDragging = false
                                    if (entity.supportsLightBrightness()) {
                                        onBrightnessChange(displayBrightness)
                                    }
                                } else {
                                    val dx = change.position.x - startX
                                    if (entity.supportsLightBrightness()) {
                                        change.consume()
                                        displayBrightness = (brightnessAtGestureStart + dx / size.width.toFloat() * 100f)
                                            .coerceIn(0f, 100f)
                                    }
                                }
                            }
                        }
                    }
                }
            },
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (isOn) {
                        if (entity.supportsLightBrightness()) {
                            drawRect(
                                color = LightActiveFill,
                                size = Size(
                                    width = size.width * (animatedBrightness / 100f).coerceIn(0f, 1f),
                                    height = size.height,
                                ),
                            )
                        } else {
                            drawRect(color = LightDimFill)
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
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = if (isOn) LightActiveColor else colors.colorTextDisabled,
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
                        text = if (isOn && entity.supportsLightBrightness()) {
                            "${animatedBrightness.toInt()}%"
                        } else if (isOn) {
                            "On"
                        } else {
                            "Off"
                        },
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

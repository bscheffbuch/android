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
                    down.consume()
                    onExpandedChange(!isExpanded)
                    var waiting = true
                    while (waiting) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { it.consume() }
                        if (event.changes.all { !it.pressed }) waiting = false
                    }
                    return@awaitEachGesture
                }
                val startX = down.position.x
                val brightnessAtGestureStart = displayBrightness
                var dragStarted = false

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
                                displayBrightness = (brightnessAtGestureStart + dx / size.width.toFloat() * 100f)
                                    .coerceIn(0f, 100f)
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
                            "${animatedBrightness.toInt()}%"
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

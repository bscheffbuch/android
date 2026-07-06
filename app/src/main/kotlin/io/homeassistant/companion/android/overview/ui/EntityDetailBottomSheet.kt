package io.homeassistant.companion.android.overview.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getLightBrightness
import io.homeassistant.companion.android.common.data.integration.getLightColor
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsLightBrightness
import io.homeassistant.companion.android.common.data.integration.supportsLightColorTemperature
import io.homeassistant.companion.android.overview.OverviewLightGroup
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityDetailBottomSheet(
    entity: Entity,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onColorChange: (rgbColor: List<Int>) -> Unit,
    onColorTemperatureChange: (kelvin: Int, immediate: Boolean) -> Unit,
    displayedAsLight: Boolean = false,
    onDisplayedAsLightChange: ((Boolean) -> Unit)? = null,
) {
    val isOn = entity.isActive()
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val deviceClass = entity.attributes["device_class"] as? String
    val isOutlet = entity.domain == "switch" && deviceClass == "outlet"
    LightControlBottomSheet(
        title = friendlyName,
        subtitle = entity.state.replaceFirstChar { it.uppercaseChar() },
        domain = entity.domain,
        deviceClass = deviceClass,
        isOn = isOn,
        brightness = entity.getLightBrightness()?.value ?: if (isOn) 100f else 0f,
        supportsBrightness = entity.supportsLightBrightness(),
        supportsColor = entity.supportsLightColor(),
        selectedColor = entity.getLightColor()?.let { Color(it) },
        supportsColorTemperature = entity.supportsLightColorTemperature(),
        colorTemperatureKelvin = entity.colorTemperatureKelvin(),
        colorTemperatureRange = entity.colorTemperatureRange(),
        canToggle = entity.domain in ToggleDomains,
        attributes = entity.displayAttributes(),
        onDismiss = onDismiss,
        onToggle = onToggle,
        onBrightnessChange = onBrightnessChange,
        onColorChange = onColorChange,
        onColorTemperatureChange = onColorTemperatureChange,
        extraContent = if (isOutlet && onDisplayedAsLightChange != null) {
            { DisplayedAsRow(displayedAsLight = displayedAsLight, onDisplayedAsLightChange = onDisplayedAsLightChange) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightGroupDetailBottomSheet(
    group: OverviewLightGroup,
    entities: List<Entity>,
    onDismiss: () -> Unit,
    onBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onColorChange: (rgbColor: List<Int>) -> Unit,
    onColorTemperatureChange: (kelvin: Int, immediate: Boolean) -> Unit,
) {
    val isOn = entities.any { it.isActive() }
    val brightness = entities
        .mapNotNull { it.getLightBrightness()?.value }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
        ?: if (isOn) 100f else 0f
    val colorTemperatureValues = entities.mapNotNull { it.colorTemperatureKelvin() }
    val colorTemperature = colorTemperatureValues.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    LightControlBottomSheet(
        title = group.name,
        subtitle = "${entities.size} lights",
        domain = "light",
        isOn = isOn,
        brightness = brightness,
        supportsBrightness = entities.any { it.supportsLightBrightness() },
        supportsColor = entities.any { it.supportsLightColor() },
        selectedColor = entities.firstNotNullOfOrNull { it.getLightColor()?.let { color -> Color(color) } },
        supportsColorTemperature = entities.any { it.supportsLightColorTemperature() },
        colorTemperatureKelvin = colorTemperature,
        colorTemperatureRange = entities.firstNotNullOfOrNull { it.colorTemperatureRange() },
        canToggle = false,
        attributes = entities.map { (it.attributes["friendly_name"]?.toString() ?: it.entityId) to it.state },
        onDismiss = onDismiss,
        onToggle = {},
        onBrightnessChange = onBrightnessChange,
        onColorChange = onColorChange,
        onColorTemperatureChange = onColorTemperatureChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LightControlBottomSheet(
    title: String,
    subtitle: String,
    domain: String,
    isOn: Boolean,
    brightness: Float,
    supportsBrightness: Boolean,
    supportsColor: Boolean,
    selectedColor: Color?,
    supportsColorTemperature: Boolean,
    colorTemperatureKelvin: Int?,
    colorTemperatureRange: IntRange?,
    canToggle: Boolean,
    attributes: List<Pair<String, Any?>>,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onColorChange: (rgbColor: List<Int>) -> Unit,
    onColorTemperatureChange: (kelvin: Int, immediate: Boolean) -> Unit,
    deviceClass: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalHAColorScheme.current
    var currentBrightness by remember(title) { mutableFloatStateOf(brightness) }
    var currentTemperature by remember(title) {
        mutableFloatStateOf((colorTemperatureKelvin ?: colorTemperatureRange?.last ?: 4000).toFloat())
    }
    var currentColor by remember(title) { mutableStateOf(selectedColor) }

    LaunchedEffect(brightness) {
        currentBrightness = brightness
    }
    LaunchedEffect(colorTemperatureKelvin) {
        colorTemperatureKelvin?.let { currentTemperature = it.toFloat() }
    }
    LaunchedEffect(selectedColor) {
        currentColor = selectedColor
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.colorSurfaceDefault,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (domain ==
                        "light"
                    ) {
                        Icons.Rounded.Lightbulb
                    } else {
                        EntityIconProvider.iconForDomain(domain, isOn, deviceClass)
                    },
                    contentDescription = null,
                    tint = currentColor ?: if (isOn) colors.colorFillLightLoudResting else colors.colorTextDisabled,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = title,
                        color = colors.colorTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Text(
                        text = subtitle,
                        color = colors.colorTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }

            if (canToggle) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(commonR.string.overview_power),
                        color = colors.colorTextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Switch(checked = isOn, onCheckedChange = { onToggle() })
                }
            }

            extraContent?.let {
                Spacer(modifier = Modifier.height(12.dp))
                it()
            }

            if (supportsBrightness) {
                DetailSectionLabel(text = stringResource(commonR.string.overview_brightness))
                Slider(
                    value = currentBrightness,
                    onValueChange = {
                        currentBrightness = it
                        onBrightnessChange(it, false)
                    },
                    onValueChangeFinished = { onBrightnessChange(currentBrightness, true) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.colorFillLightLoudResting,
                        activeTrackColor = colors.colorFillLightLoudResting,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${currentBrightness.toInt()}%",
                    color = colors.colorTextSecondary,
                    fontSize = 12.sp,
                )
            }

            if (supportsColor) {
                DetailSectionLabel(text = stringResource(commonR.string.overview_color))
                ColorWheel(
                    selectedColor = currentColor,
                    onColorSelected = {
                        currentColor = it
                        onColorChange(it.toRgbList())
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                )
            }

            if (supportsColorTemperature && colorTemperatureRange != null) {
                DetailSectionLabel(text = stringResource(commonR.string.overview_color_temperature))
                Slider(
                    value = currentTemperature,
                    onValueChange = {
                        currentTemperature = it
                        onColorTemperatureChange(it.roundToInt(), false)
                    },
                    onValueChangeFinished = { onColorTemperatureChange(currentTemperature.roundToInt(), true) },
                    valueRange = colorTemperatureRange.first.toFloat()..colorTemperatureRange.last.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${currentTemperature.roundToInt()} K",
                    color = colors.colorTextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.colorBorderNeutralQuiet)
            DetailSectionLabel(text = stringResource(commonR.string.overview_current_values))
            ValueRow(
                label = stringResource(commonR.string.entity),
                value = domain.replaceFirstChar {
                    it.uppercaseChar()
                },
            )
            ValueRow(label = stringResource(commonR.string.state), value = subtitle)
            if (supportsBrightness) {
                ValueRow(
                    label = stringResource(commonR.string.overview_brightness),
                    value = "${currentBrightness.roundToInt()}%",
                )
            }
            currentColor?.let {
                ValueRow(
                    label = stringResource(commonR.string.overview_color),
                    value = it.toRgbList().joinToString(prefix = "RGB(", postfix = ")"),
                )
            }
            if (supportsColorTemperature && colorTemperatureRange != null) {
                ValueRow(
                    label = stringResource(commonR.string.overview_color_temperature),
                    value = "${currentTemperature.roundToInt()} K",
                )
            }

            if (attributes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.colorBorderNeutralQuiet)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(commonR.string.overview_attributes),
                    color = colors.colorTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(attributes) { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = key.replace('_', ' ').replaceFirstChar { it.uppercaseChar() },
                                color = colors.colorTextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = value?.toString() ?: "",
                                color = colors.colorTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DisplayedAsRow(displayedAsLight: Boolean, onDisplayedAsLightChange: (Boolean) -> Unit) {
    val colors = LocalHAColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(commonR.string.overview_displayed_as),
            color = colors.colorTextPrimary,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DisplayedAsOption(
                label = stringResource(commonR.string.overview_displayed_as_outlet),
                selected = !displayedAsLight,
                onClick = { onDisplayedAsLightChange(false) },
            )
            DisplayedAsOption(
                label = stringResource(commonR.string.overview_displayed_as_light),
                selected = displayedAsLight,
                onClick = { onDisplayedAsLightChange(true) },
            )
        }
    }
}

@Composable
private fun DisplayedAsOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalHAColorScheme.current
    Text(
        text = label,
        color = if (selected) colors.colorOnPrimaryNormal else colors.colorTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.colorFillPrimaryNormalResting else colors.colorFillNeutralQuietResting)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun DetailSectionLabel(text: String) {
    val colors = LocalHAColorScheme.current
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = text,
        color = colors.colorTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ValueRow(label: String, value: String) {
    val colors = LocalHAColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = colors.colorTextSecondary, fontSize = 13.sp)
        Text(text = value, color = colors.colorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ColorWheel(selectedColor: Color?, onColorSelected: (Color) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalHAColorScheme.current
    val radiusPx = with(LocalDensity.current) { 110.dp.toPx() }
    val wheelColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red,
        )
    }
    var selectedOffset by remember(selectedColor, radiusPx) { mutableStateOf(selectedColor?.toWheelOffset(radiusPx)) }

    fun selectFromOffset(offset: Offset, radius: Float) {
        val center = Offset(radius, radius)
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
        val hue = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
        val saturation = (distance / radius).coerceIn(0f, 1f)
        selectedOffset = Offset(
            x = center.x + cos(hue / 180f * PI.toFloat()) * saturation * radius,
            y = center.y + sin(hue / 180f * PI.toFloat()) * saturation * radius,
        )
        onColorSelected(Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f))))
    }

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .border(1.dp, colors.colorBorderNeutralQuiet, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures { selectFromOffset(it, minOf(size.width, size.height) / 2f) }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { selectFromOffset(it, minOf(size.width, size.height) / 2f) },
                        onDrag = { change, _ ->
                            change.consume()
                            selectFromOffset(change.position, minOf(size.width, size.height) / 2f)
                        },
                    )
                },
        ) {
            drawCircle(Brush.sweepGradient(wheelColors))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f,
                ),
            )
        }
        selectedOffset?.let { offset ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopStart)
                    .padding(0.dp)
                    .graphicsLayer {
                        translationX = offset.x - size.width / 2f
                        translationY = offset.y - size.height / 2f
                    }
                    .clip(CircleShape)
                    .background(selectedColor ?: Color.White)
                    .border(2.dp, colors.colorSurfaceDefault, CircleShape),
            )
        }
    }
}

private val ToggleDomains = setOf(
    "automation",
    "cover",
    "fan",
    "humidifier",
    "input_boolean",
    "light",
    "lock",
    "media_player",
    "remote",
    "siren",
    "switch",
)

private fun Color.toRgbList(): List<Int> {
    val argb = toArgb()
    return listOf(AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb))
}

private fun Color.toWheelOffset(radius: Float): Offset {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    val angle = hsv[0] / 180f * PI.toFloat()
    val saturation = hsv[1].coerceIn(0f, 1f)
    return Offset(
        x = radius + cos(angle) * saturation * radius,
        y = radius + sin(angle) * saturation * radius,
    )
}

private fun Entity.displayAttributes(): List<Pair<String, Any?>> {
    val ignoredKeys = setOf(
        "friendly_name",
        "supported_features",
        "supported_color_modes",
        "color_mode",
        "entity_picture",
        "icon",
        "brightness",
        "rgb_color",
        "color_temp_kelvin",
        "min_color_temp_kelvin",
        "max_color_temp_kelvin",
    )
    return attributes
        .filterKeys { it !in ignoredKeys }
        .entries
        .sortedBy { it.key }
        .map { it.key to it.value }
}

private fun Entity.supportsLightColor(): Boolean {
    val modes = attributes["supported_color_modes"] as? List<*>
    return domain == "light" &&
        (
            modes?.any { it == "rgb" || it == "hs" || it == "xy" }
                ?: (attributes["rgb_color"] != null)
            )
}

private fun Entity.colorTemperatureKelvin(): Int? = (attributes["color_temp_kelvin"] as? Number)?.toInt()

private fun Entity.colorTemperatureRange(): IntRange? {
    val min = (attributes["min_color_temp_kelvin"] as? Number)?.toInt()
    val max = (attributes["max_color_temp_kelvin"] as? Number)?.toInt()
    return if (min != null && max != null && min < max) min..max else null
}

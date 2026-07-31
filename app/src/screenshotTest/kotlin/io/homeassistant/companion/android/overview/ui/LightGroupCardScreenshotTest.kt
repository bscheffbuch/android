package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.overview.OverviewLightGroup
import java.time.LocalDateTime

private fun lightEntityOf(entityId: String, brightnessPercent: Int?) = Entity(
    entityId = entityId,
    state = if (brightnessPercent != null) "on" else "off",
    attributes = mapOf(
        "friendly_name" to entityId.substringAfter('.'),
        "supported_color_modes" to listOf("brightness"),
        "supported_features" to 0,
        "brightness" to brightnessPercent?.let { (it / 100f * 255f).toInt() },
    ),
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

private fun genericEntityOf(entityId: String) = Entity(
    entityId = entityId,
    state = "on",
    attributes = mapOf("friendly_name" to entityId.substringAfter('.')),
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

private val readingLampGroup = OverviewLightGroup(
    id = "group.reading_lamp_group",
    name = "Reading Lamp",
    entityIds = listOf("light.top", "light.bottom"),
)

class LightGroupCardScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `LightGroupCard collapsed on`() {
        HAThemeForPreview {
            Box(modifier = Modifier.width(194.dp)) {
                LightGroupCard(
                    group = readingLampGroup,
                    entities = listOf(
                        lightEntityOf("light.top", brightnessPercent = 100),
                        lightEntityOf("light.bottom", brightnessPercent = 50),
                    ),
                    isExpanded = false,
                    onExpandedChange = {},
                    onToggle = {},
                    onBrightnessChange = { _, _ -> },
                    onOpenDetail = {},
                )
            }
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `LightGroupCard collapsed off`() {
        HAThemeForPreview {
            Box(modifier = Modifier.width(194.dp)) {
                LightGroupCard(
                    group = readingLampGroup,
                    entities = listOf(lightEntityOf("light.reading_lamp", brightnessPercent = null)),
                    isExpanded = false,
                    onExpandedChange = {},
                    onToggle = {},
                    onBrightnessChange = { _, _ -> },
                    onOpenDetail = {},
                )
            }
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `ExpandedLightGroupCard with controller anchored in the left grid cell`() {
        HAThemeForPreview {
            // 4dp padding keeps the highlight's 4dp bleed (drawn beyond the card's bounds) inside the capture.
            Box(modifier = Modifier.width(420.dp).padding(4.dp)) {
                ExpandedLightGroupCard(
                    group = readingLampGroup,
                    entities = listOf(
                        lightEntityOf("light.top", brightnessPercent = 100),
                        lightEntityOf("light.bottom", brightnessPercent = 50),
                    ),
                    accentColor = LocalHAColorScheme.current.colorFillLightLoudResting,
                    isAnchorRightColumn = false,
                    isEditMode = false,
                    onExpandedChange = {},
                    onToggleGroup = {},
                    onGroupBrightnessChange = { _, _ -> },
                    onOpenGroupDetail = {},
                    onEditGroup = {},
                    onDeleteGroup = {},
                    onToggleEntity = {},
                    onEntityBrightnessChange = { _, _, _ -> },
                    onOpenEntityDetail = {},
                    borrowedNeighbor = genericEntityOf("sensor.other_lamp"),
                    displayedAsLightEntityIds = emptySet(),
                    onTriggerAutomation = {},
                    onSetFanSpeed = { _, _, _ -> },
                    onSetCoverPosition = { _, _, _ -> },
                    onStopCover = {},
                    onCycleClimateHvacMode = {},
                    onSetClimateTemperature = { _, _, _ -> },
                    onTogglePlayback = {},
                    onSetMediaVolume = { _, _, _ -> },
                    onSkipToPreviousTrack = {},
                    onSkipToNextTrack = {},
                    onSetHumidifierHumidity = { _, _, _ -> },
                    onCycleHumidifierMode = {},
                )
            }
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `ExpandedLightGroupCard with controller anchored in the right grid cell`() {
        HAThemeForPreview {
            // 4dp padding keeps the highlight's 4dp bleed (drawn beyond the card's bounds) inside the capture.
            Box(modifier = Modifier.width(420.dp).padding(4.dp)) {
                ExpandedLightGroupCard(
                    group = OverviewLightGroup(
                        id = "group.office_lamps",
                        name = "Office Lamps",
                        entityIds = listOf("light.floor_lamp", "light.desk_lamp"),
                    ),
                    entities = listOf(
                        lightEntityOf("light.floor_lamp", brightnessPercent = 60),
                        lightEntityOf("light.desk_lamp", brightnessPercent = 90),
                    ),
                    accentColor = LocalHAColorScheme.current.colorFillLightLoudResting,
                    isAnchorRightColumn = true,
                    isEditMode = false,
                    onExpandedChange = {},
                    onToggleGroup = {},
                    onGroupBrightnessChange = { _, _ -> },
                    onOpenGroupDetail = {},
                    onEditGroup = {},
                    onDeleteGroup = {},
                    onToggleEntity = {},
                    onEntityBrightnessChange = { _, _, _ -> },
                    onOpenEntityDetail = {},
                    borrowedNeighbor = genericEntityOf("sensor.other_lamp"),
                    displayedAsLightEntityIds = emptySet(),
                    onTriggerAutomation = {},
                    onSetFanSpeed = { _, _, _ -> },
                    onSetCoverPosition = { _, _, _ -> },
                    onStopCover = {},
                    onCycleClimateHvacMode = {},
                    onSetClimateTemperature = { _, _, _ -> },
                    onTogglePlayback = {},
                    onSetMediaVolume = { _, _, _ -> },
                    onSkipToPreviousTrack = {},
                    onSkipToNextTrack = {},
                    onSetHumidifierHumidity = { _, _, _ -> },
                    onCycleHumidifierMode = {},
                )
            }
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `ExpandedLightGroupCard with odd member count has no partial trailing row`() {
        HAThemeForPreview {
            // 4dp padding keeps the highlight's 4dp bleed (drawn beyond the card's bounds) inside the capture.
            Box(modifier = Modifier.width(420.dp).padding(4.dp)) {
                ExpandedLightGroupCard(
                    group = OverviewLightGroup(
                        id = "group.hallway_lamps",
                        name = "Hallway Lamps",
                        entityIds = listOf("light.one", "light.two", "light.three"),
                    ),
                    entities = listOf(
                        lightEntityOf("light.one", brightnessPercent = 100),
                        lightEntityOf("light.two", brightnessPercent = 75),
                        lightEntityOf("light.three", brightnessPercent = 50),
                    ),
                    accentColor = LocalHAColorScheme.current.colorFillLightLoudResting,
                    isAnchorRightColumn = false,
                    isEditMode = false,
                    onExpandedChange = {},
                    onToggleGroup = {},
                    onGroupBrightnessChange = { _, _ -> },
                    onOpenGroupDetail = {},
                    onEditGroup = {},
                    onDeleteGroup = {},
                    onToggleEntity = {},
                    onEntityBrightnessChange = { _, _, _ -> },
                    onOpenEntityDetail = {},
                    borrowedNeighbor = null,
                    displayedAsLightEntityIds = emptySet(),
                    onTriggerAutomation = {},
                    onSetFanSpeed = { _, _, _ -> },
                    onSetCoverPosition = { _, _, _ -> },
                    onStopCover = {},
                    onCycleClimateHvacMode = {},
                    onSetClimateTemperature = { _, _, _ -> },
                    onTogglePlayback = {},
                    onSetMediaVolume = { _, _, _ -> },
                    onSkipToPreviousTrack = {},
                    onSkipToNextTrack = {},
                    onSetHumidifierHumidity = { _, _, _ -> },
                    onCycleHumidifierMode = {},
                )
            }
        }
    }
}

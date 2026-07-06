package io.homeassistant.companion.android.overview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.BaseActivity
import io.homeassistant.companion.android.common.compose.theme.HATheme
import io.homeassistant.companion.android.overview.ui.OverviewScreen

@AndroidEntryPoint
class OverviewActivity : BaseActivity() {

    private val viewModel: OverviewViewModel by viewModels()

    companion object {
        fun newInstance(context: Context): Intent = Intent(context, OverviewActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HATheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                OverviewScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadEntities() },
                    onToggleEntity = { entityId -> viewModel.toggleEntity(entityId) },
                    onToggleLightGroup = { groupId -> viewModel.toggleLightGroup(groupId) },
                    onBrightnessChange = { entityId, brightness, immediate ->
                        viewModel.setBrightness(entityId, brightness, immediate)
                    },
                    onGroupBrightnessChange = { groupId, brightness, immediate ->
                        viewModel.setGroupBrightness(groupId, brightness, immediate)
                    },
                    onColorChange = { entityId, rgbColor -> viewModel.setLightColor(entityId, rgbColor) },
                    onGroupColorChange = { groupId, rgbColor -> viewModel.setGroupColor(groupId, rgbColor) },
                    onColorTemperatureChange = { entityId, kelvin, immediate ->
                        viewModel.setLightColorTemperature(entityId, kelvin, immediate)
                    },
                    onGroupColorTemperatureChange = { groupId, kelvin, immediate ->
                        viewModel.setGroupColorTemperature(groupId, kelvin, immediate)
                    },
                    onEditModeChange = { viewModel.setEditMode(it) },
                    onSaveLightGroup = { groupId, name, entityIds, colorArgb ->
                        viewModel.saveLightGroup(groupId, name, entityIds, colorArgb)
                    },
                    onDeleteLightGroup = { groupId -> viewModel.deleteLightGroup(groupId) },
                    onMoveItem = { fromKey, toKey -> viewModel.moveItem(fromKey, toKey) },
                    onItemDrop = { sourceKey, targetKey -> viewModel.handleItemDrop(sourceKey, targetKey) },
                    onRemoveEntityFromGroup = { groupId, entityId ->
                        viewModel.removeLightFromGroup(groupId, entityId)
                    },
                    onGroupExpandedChange = { groupId, expanded -> viewModel.setLightGroupExpanded(groupId, expanded) },
                    onSetDisplayedAsLight = { entityId, asLight -> viewModel.setDisplayedAsLight(entityId, asLight) },
                    onTriggerAutomation = { entityId -> viewModel.triggerAutomation(entityId) },
                    onSetFanSpeed = { entityId, percentage, immediate ->
                        viewModel.setFanSpeed(entityId, percentage, immediate)
                    },
                    onSetCoverPosition = { entityId, percentage, immediate ->
                        viewModel.setCoverPosition(entityId, percentage, immediate)
                    },
                    onStopCover = { entityId -> viewModel.stopCover(entityId) },
                    onCycleClimateHvacMode = { entityId -> viewModel.cycleClimateHvacMode(entityId) },
                    onSetClimateTemperature = { entityId, temperature, immediate ->
                        viewModel.setClimateTemperature(entityId, temperature, immediate)
                    },
                    onTogglePlayback = { entityId -> viewModel.toggleMediaPlayback(entityId) },
                    onSetMediaVolume = { entityId, volume, immediate ->
                        viewModel.setMediaVolume(entityId, volume, immediate)
                    },
                    onSkipToPreviousTrack = { entityId -> viewModel.skipToPreviousTrack(entityId) },
                    onSkipToNextTrack = { entityId -> viewModel.skipToNextTrack(entityId) },
                    onSetHumidifierHumidity = { entityId, humidity, immediate ->
                        viewModel.setHumidifierHumidity(entityId, humidity, immediate)
                    },
                    onCycleHumidifierMode = { entityId -> viewModel.cycleHumidifierMode(entityId) },
                    errorEvents = viewModel.errorEvents,
                )
            }
        }
    }
}

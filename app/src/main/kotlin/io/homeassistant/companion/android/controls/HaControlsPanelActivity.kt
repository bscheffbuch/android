package io.homeassistant.companion.android.controls

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.os.Bundle
import android.service.controls.ControlsProviderService
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HATheme
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.overview.OverviewViewModel
import io.homeassistant.companion.android.overview.ui.OverviewScreen
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HaControlsPanelActivity : AppCompatActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    private val overviewViewModel: OverviewViewModel by viewModels()

    @SuppressLint("InlinedApi") // This activity will only be launched on Android 14+
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        lifecycleScope.launch {
            if (!serverManager.isRegistered()) {
                finish()
            }
        }

        val disallowLocked =
            intent?.hasExtra(ControlsProviderService.EXTRA_LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS) != true ||
                intent?.getBooleanExtra(ControlsProviderService.EXTRA_LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS, false) != true
        val keyguardManager = getSystemService<KeyguardManager>()
        val isLocked = keyguardManager?.isKeyguardLocked ?: true
        if (disallowLocked && isLocked) {
            setContent { LockedPanelView() }
            return
        }

        setContent {
            HATheme {
                val uiState by overviewViewModel.uiState.collectAsStateWithLifecycle()
                OverviewScreen(
                    uiState = uiState,
                    onRefresh = overviewViewModel::loadEntities,
                    onToggleEntity = overviewViewModel::toggleEntity,
                    onToggleLightGroup = overviewViewModel::toggleLightGroup,
                    onBrightnessChange = overviewViewModel::setBrightness,
                    onGroupBrightnessChange = overviewViewModel::setGroupBrightness,
                    onColorChange = overviewViewModel::setLightColor,
                    onGroupColorChange = overviewViewModel::setGroupColor,
                    onColorTemperatureChange = overviewViewModel::setLightColorTemperature,
                    onGroupColorTemperatureChange = overviewViewModel::setGroupColorTemperature,
                    onEditModeChange = overviewViewModel::setEditMode,
                    onSaveLightGroup = overviewViewModel::saveLightGroup,
                    onDeleteLightGroup = overviewViewModel::deleteLightGroup,
                    onMoveItem = overviewViewModel::moveItem,
                    onItemDrop = overviewViewModel::handleItemDrop,
                    onRemoveEntityFromGroup = overviewViewModel::removeLightFromGroup,
                    onMoveGroupMember = overviewViewModel::moveGroupMember,
                    onMoveEntityIntoGroup = overviewViewModel::moveEntityIntoGroup,
                    onMoveEntityOutOfGroup = overviewViewModel::moveEntityOutOfGroup,
                    onGroupExpandedChange = overviewViewModel::setLightGroupExpanded,
                    onSetDisplayedAsLight = overviewViewModel::setDisplayedAsLight,
                    onTriggerAutomation = overviewViewModel::triggerAutomation,
                    onSetFanSpeed = overviewViewModel::setFanSpeed,
                    onSetCoverPosition = overviewViewModel::setCoverPosition,
                    onStopCover = overviewViewModel::stopCover,
                    onCycleClimateHvacMode = overviewViewModel::cycleClimateHvacMode,
                    onSetClimateTemperature = overviewViewModel::setClimateTemperature,
                    onTogglePlayback = overviewViewModel::toggleMediaPlayback,
                    onSetMediaVolume = overviewViewModel::setMediaVolume,
                    onSkipToPreviousTrack = overviewViewModel::skipToPreviousTrack,
                    onSkipToNextTrack = overviewViewModel::skipToNextTrack,
                    onSetHumidifierHumidity = overviewViewModel::setHumidifierHumidity,
                    onCycleHumidifierMode = overviewViewModel::cycleHumidifierMode,
                    onClose = { finish() },
                    errorEvents = overviewViewModel.errorEvents,
                )
            }
        }
    }

    @Composable
    fun LockedPanelView(modifier: Modifier = Modifier) {
        HomeAssistantAppTheme {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(commonR.string.tile_auth_required),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

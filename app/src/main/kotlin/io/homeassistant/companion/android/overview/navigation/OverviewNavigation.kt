package io.homeassistant.companion.android.overview.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.homeassistant.companion.android.automations.AutomationsViewModel
import io.homeassistant.companion.android.automations.ui.AutomationsScreen
import io.homeassistant.companion.android.frontend.navigation.navigateToFrontend
import io.homeassistant.companion.android.launch.HAStartDestinationRoute
import io.homeassistant.companion.android.overview.OverviewViewModel
import io.homeassistant.companion.android.overview.ui.HomeBottomNavigationBar
import io.homeassistant.companion.android.overview.ui.HomeContentTab
import io.homeassistant.companion.android.overview.ui.OverviewScreen
import io.homeassistant.companion.android.settings.SettingsActivity
import kotlinx.serialization.Serializable

/**
 * Start destination used when [io.homeassistant.companion.android.WIPFeature.USE_NATIVE_OVERVIEW_LANDING]
 * is enabled, replacing [io.homeassistant.companion.android.frontend.navigation.FrontendRoute] as the
 * app's cold-start landing screen. Always targets the active server, see
 * [io.homeassistant.companion.android.common.data.servers.ServerManager.Companion.SERVER_ID_ACTIVE].
 */
@Serializable
internal data object OverviewLandingRoute : HAStartDestinationRoute

/**
 * Registers the native Overview screen as a navigable destination, used as the app's start
 * destination while [io.homeassistant.companion.android.WIPFeature.USE_NATIVE_OVERVIEW_LANDING]
 * is enabled.
 *
 * @param navController The navigation controller
 */
internal fun NavGraphBuilder.overviewLandingScreen(navController: NavController) {
    composable<OverviewLandingRoute> {
        val context = LocalContext.current
        val viewModel: OverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val automationsViewModel: AutomationsViewModel = hiltViewModel()
        val automationsUiState by automationsViewModel.uiState.collectAsStateWithLifecycle()
        val saveSceneDialogState by automationsViewModel.saveSceneDialogState.collectAsStateWithLifecycle()
        var selectedTab by remember { mutableStateOf(HomeContentTab.HOME) }

        // Settings hosts this same bottom bar (see SettingsActivity's showHomeNavBar) and reports
        // back which tab the user tapped there, so switching to it from Settings works the same
        // as switching between tabs here.
        val settingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val requestedTab = result.data?.let {
                IntentCompat.getSerializableExtra(it, SettingsActivity.EXTRA_SELECTED_TAB, HomeContentTab::class.java)
            }
            if (requestedTab != null) {
                selectedTab = requestedTab
            }
        }

        Scaffold(
            bottomBar = {
                HomeBottomNavigationBar(
                    selectedTab = selectedTab,
                    onSelectHome = { selectedTab = HomeContentTab.HOME },
                    onSelectAutomationsAndScenes = { selectedTab = HomeContentTab.AUTOMATIONS_AND_SCENES },
                    onOpenSettings = {
                        settingsLauncher.launch(SettingsActivity.newInstance(context, showHomeNavBar = true))
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (selectedTab) {
                    HomeContentTab.HOME -> OverviewScreen(
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
                        onMoveGroupMember = { groupId, fromEntityId, toEntityId ->
                            viewModel.moveGroupMember(groupId, fromEntityId, toEntityId)
                        },
                        onMoveEntityIntoGroup = { entityId, groupId, targetEntityId ->
                            viewModel.moveEntityIntoGroup(entityId, groupId, targetEntityId)
                        },
                        onMoveEntityOutOfGroup = { groupId, entityId, targetKey ->
                            viewModel.moveEntityOutOfGroup(groupId, entityId, targetKey)
                        },
                        onGroupExpandedChange = { groupId, expanded ->
                            viewModel.setLightGroupExpanded(groupId, expanded)
                        },
                        onSetDisplayedAsLight = { entityId, asLight ->
                            viewModel.setDisplayedAsLight(entityId, asLight)
                        },
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
                        // The landing screen has no previous destination to pop back to, so the "close"
                        // affordance instead opens the full web frontend — the escape hatch back to
                        // feature parity. The Automations/Settings top-bar shortcuts are gone: the
                        // bottom navigation bar now covers that.
                        onClose = { navController.navigateToFrontend() },
                        errorEvents = viewModel.errorEvents,
                    )

                    HomeContentTab.AUTOMATIONS_AND_SCENES -> AutomationsScreen(
                        uiState = automationsUiState,
                        onToggle = { entityId -> automationsViewModel.toggle(entityId) },
                        onTriggerNow = { entityId -> automationsViewModel.triggerNow(entityId) },
                        onActivateScene = { entityId -> automationsViewModel.activateScene(entityId) },
                        errorEvents = automationsViewModel.errorEvents,
                        // Embedded as a bottom-nav tab, not pushed onto the back stack, so there is no
                        // "back" destination to pop — the tab switch itself is the navigation.
                        onNavigateBack = null,
                        saveSceneDialogState = saveSceneDialogState,
                        onCreateSceneClicked = automationsViewModel::onCreateSceneClicked,
                        onDismissCreateScene = automationsViewModel::onDismissCreateScene,
                        onSaveScene = automationsViewModel::saveScene,
                    )
                }
            }
        }
    }
}

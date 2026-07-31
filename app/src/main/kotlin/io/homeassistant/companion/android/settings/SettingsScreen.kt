package io.homeassistant.companion.android.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.authenticator.Authenticator
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAPlainButton
import io.homeassistant.companion.android.common.compose.composable.HARadioGroup
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.composable.HASettingsRow
import io.homeassistant.companion.android.common.compose.composable.HASettingsSubheader
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.composable.HATopBar
import io.homeassistant.companion.android.common.compose.composable.RadioOption
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.prefs.NightModeTheme
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.launch.intentLaunchOnboarding
import io.homeassistant.companion.android.nfc.NfcSetupActivity
import io.homeassistant.companion.android.settings.wear.SettingsWearActivity
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import io.homeassistant.companion.android.webview.WebViewActivity
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

/** Navigation destinations reachable from the top-level Settings screen. */
sealed interface SettingsDestination {
    data object Sensors : SettingsDestination
    data object SensorUpdateFrequency : SettingsDestination
    data object Gestures : SettingsDestination
    data object AssistSettings : SettingsDestination
    data object NotificationChannels : SettingsDestination
    data object NotificationHistory : SettingsDestination
    data object ManageDeviceControls : SettingsDestination
    data object ManageTiles : SettingsDestination
    data object ManageShortcuts : SettingsDestination
    data object ManageWidgets : SettingsDestination
    data object ManageAndroidAuto : SettingsDestination
    data object Developer : SettingsDestination
    data object Licenses : SettingsDestination
    data class ServerSettings(val serverId: Int) : SettingsDestination
}

/**
 * Top-level Settings screen: the landing page reached from the app's main navigation, exposing
 * server management and every app-wide preference.
 *
 * @param onNavigate Requests navigation to one of the sub-screens reachable from here.
 * @param onRequestAuthentication Requests biometric re-authentication before unlocking a server's
 * settings, mirroring [SettingsActivity.requestAuthentication]: returns `false` if authentication
 * could not be requested (e.g. no biometrics configured), in which case the caller should treat it
 * as an implicit success.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    appLockViewModel: AppLockViewModel,
    onNavigate: (SettingsDestination) -> Unit,
    onRequestAuthentication: (title: String, onResult: (Int) -> Boolean) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricSetTitle = stringResource(commonR.string.biometric_set_title)

    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshNotificationChannelState()
    }
    val openNotificationSettings = {
        notificationSettingsLauncher.launch(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }

    LaunchedEffect(uiState.addServerResult) {
        val result = uiState.addServerResult ?: return@LaunchedEffect
        val message = context.getString(
            if (result.success) commonR.string.server_add_success else commonR.string.server_add_failed,
        )
        val actionLabel = if (result.success && result.serverId != null) {
            context.getString(commonR.string.activate)
        } else {
            null
        }
        val snackbarResult = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
        )
        if (snackbarResult == SnackbarResult.ActionPerformed && result.serverId != null) {
            context.startActivity(
                WebViewActivity.newInstance(context, path = null, serverId = result.serverId)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) },
            )
        }
        viewModel.onAddServerResultAcknowledged()
    }

    Column(modifier = modifier.fillMaxSize()) {
        HATopBar(title = { Text(text = stringResource(commonR.string.companion_app)) })
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsContent(
                uiState = uiState,
                onAddServerClicked = {
                    context.startActivity(
                        context.intentLaunchOnboarding(
                            urlToOnboard = null,
                            hideExistingServers = true,
                            skipWelcome = true,
                        ),
                    )
                },
                onServerClicked = { server ->
                    coroutineScope.launch {
                        handleServerClicked(
                            server,
                            biometricSetTitle,
                            appLockViewModel,
                            onRequestAuthentication,
                            onNavigate,
                        )
                    }
                },
                onWearSettingsClicked = { context.startActivity(SettingsWearActivity.newInstance(context)) },
                onSensorsClicked = { onNavigate(SettingsDestination.Sensors) },
                onSensorUpdateFrequencyClicked = { onNavigate(SettingsDestination.SensorUpdateFrequency) },
                onGesturesClicked = { onNavigate(SettingsDestination.Gestures) },
                onFullscreenToggled = viewModel::onFullscreenToggled,
                onScreenOrientationSelected = viewModel::onScreenOrientationSelected,
                onKeepScreenOnToggled = viewModel::onKeepScreenOnToggled,
                onPageZoomLevelSelected = viewModel::onPageZoomLevelSelected,
                onPinchToZoomToggled = viewModel::onPinchToZoomToggled,
                onAutoplayVideoToggled = viewModel::onAutoplayVideoToggled,
                onAlwaysShowFirstViewOnAppStartToggled = viewModel::onAlwaysShowFirstViewOnAppStartToggled,
                onNfcTagsClicked = { context.startActivity(NfcSetupActivity.newInstance(context)) },
                onLanguageSelected = viewModel::onLanguageSelected,
                onThemeSelected = viewModel::onThemeSelected,
                onBackgroundAccessRowClicked = { viewModel.onBackgroundAccessRowClicked(context) },
                onNotificationPermissionRowClicked = openNotificationSettings,
                onNotificationChannelsClicked = { onNavigate(SettingsDestination.NotificationChannels) },
                onNotificationHistoryClicked = { onNavigate(SettingsDestination.NotificationHistory) },
                onAssistSettingsClicked = { onNavigate(SettingsDestination.AssistSettings) },
                onAssistVoiceCommandIntentToggled = viewModel::onAssistVoiceCommandIntentToggled,
                onAutoFavoritesClicked = { onNavigate(SettingsDestination.ManageAndroidAuto) },
                onManageDeviceControlsClicked = { onNavigate(SettingsDestination.ManageDeviceControls) },
                onManageTilesClicked = { onNavigate(SettingsDestination.ManageTiles) },
                onManageShortcutsClicked = { onNavigate(SettingsDestination.ManageShortcuts) },
                onManageWidgetsClicked = { onNavigate(SettingsDestination.ManageWidgets) },
                onEnableHaLauncherToggled = viewModel::onEnableHaLauncherToggled,
                onSetLauncherAppClicked = {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                onDocsClicked = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, "https://companion.home-assistant.io".toAndroidUri()),
                    )
                },
                onDeveloperClicked = { onNavigate(SettingsDestination.Developer) },
                onShowChangeLogClicked = { viewModel.onShowChangeLogClicked(context) },
                onChangeLogPopupEnabledToggled = viewModel::onChangeLogPopupEnabledToggled,
                onChangelogGithubClicked = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uiState.changelogGithubUrl.toAndroidUri()))
                },
                onLicensesClicked = { onNavigate(SettingsDestination.Licenses) },
                onPrivacyClicked = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, context.getString(commonR.string.privacy_url).toAndroidUri()),
                    )
                },
                onCrashReportingToggled = viewModel::onCrashReportingToggled,
                onSuggestionClicked = {
                    when (uiState.suggestion?.id) {
                        SettingsViewModel.SUGGESTION_ASSISTANT_APP -> context.startActivity(
                            viewModel.getSetDefaultAssistantIntent(),
                        )
                        SettingsViewModel.SUGGESTION_NOTIFICATION_PERMISSION -> openNotificationSettings()
                    }
                },
                onSuggestionDismissed = viewModel::onSuggestionDismissed,
            )
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/**
 * Handles a click on a server row: authenticates via biometrics if the server is locked, then
 * requests navigation to that server's settings. Mirrors the legacy
 * `SettingsFragment.updateServers`/`onServerLockResult` pair call-for-call.
 */
private suspend fun handleServerClicked(
    server: Server,
    biometricSetTitle: String,
    appLockViewModel: AppLockViewModel,
    onRequestAuthentication: (title: String, onResult: (Int) -> Boolean) -> Boolean,
    onNavigate: (SettingsDestination) -> Unit,
) {
    fun onLockResult(result: Int): Boolean {
        if (result == Authenticator.SUCCESS) {
            appLockViewModel.setAppActive(server.id, true)
            onNavigate(SettingsDestination.ServerSettings(server.id))
        }
        return true
    }

    val needsAuth = appLockViewModel.isAppLocked(server.id)
    if (!needsAuth) {
        onLockResult(Authenticator.SUCCESS)
    } else {
        val canAuth = onRequestAuthentication(biometricSetTitle, ::onLockResult)
        if (!canAuth) onLockResult(Authenticator.SUCCESS)
    }
}

private fun String.toAndroidUri(): Uri = Uri.parse(this)

@VisibleForTesting
internal const val SETTINGS_LIST_TEST_TAG = "settings_list"

@Composable
@VisibleForTesting
internal fun SettingsContent(
    uiState: SettingsUiState,
    onAddServerClicked: () -> Unit,
    onServerClicked: (Server) -> Unit,
    onWearSettingsClicked: () -> Unit,
    onSensorsClicked: () -> Unit,
    onSensorUpdateFrequencyClicked: () -> Unit,
    onGesturesClicked: () -> Unit,
    onFullscreenToggled: (Boolean) -> Unit,
    onScreenOrientationSelected: (String) -> Unit,
    onKeepScreenOnToggled: (Boolean) -> Unit,
    onPageZoomLevelSelected: (Int) -> Unit,
    onPinchToZoomToggled: (Boolean) -> Unit,
    onAutoplayVideoToggled: (Boolean) -> Unit,
    onAlwaysShowFirstViewOnAppStartToggled: (Boolean) -> Unit,
    onNfcTagsClicked: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onThemeSelected: (NightModeTheme) -> Unit,
    onBackgroundAccessRowClicked: () -> Unit,
    onNotificationPermissionRowClicked: () -> Unit,
    onNotificationChannelsClicked: () -> Unit,
    onNotificationHistoryClicked: () -> Unit,
    onAssistSettingsClicked: () -> Unit,
    onAssistVoiceCommandIntentToggled: (Boolean) -> Unit,
    onAutoFavoritesClicked: () -> Unit,
    onManageDeviceControlsClicked: () -> Unit,
    onManageTilesClicked: () -> Unit,
    onManageShortcutsClicked: () -> Unit,
    onManageWidgetsClicked: () -> Unit,
    onEnableHaLauncherToggled: (Boolean) -> Unit,
    onSetLauncherAppClicked: () -> Unit,
    onDocsClicked: () -> Unit,
    onDeveloperClicked: () -> Unit,
    onShowChangeLogClicked: () -> Unit,
    onChangeLogPopupEnabledToggled: (Boolean) -> Unit,
    onChangelogGithubClicked: () -> Unit,
    onLicensesClicked: () -> Unit,
    onPrivacyClicked: () -> Unit,
    onCrashReportingToggled: (Boolean) -> Unit,
    onSuggestionClicked: () -> Unit,
    onSuggestionDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE4) +
            safeBottomPaddingValues(applyHorizontal = false),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
        modifier = modifier.testTag(SETTINGS_LIST_TEST_TAG),
    ) {
        uiState.suggestion?.let { suggestion ->
            item {
                SuggestionBanner(
                    suggestion = suggestion,
                    onClicked = onSuggestionClicked,
                    onDismissed = onSuggestionDismissed,
                )
            }
        }

        serversDevicesSection(uiState, onAddServerClicked, onServerClicked, onWearSettingsClicked)
        sensorsSection(onSensorsClicked, onSensorUpdateFrequencyClicked)
        otherSettingsSection(
            uiState = uiState,
            onGesturesClicked = onGesturesClicked,
            onFullscreenToggled = onFullscreenToggled,
            onScreenOrientationSelected = onScreenOrientationSelected,
            onKeepScreenOnToggled = onKeepScreenOnToggled,
            onPageZoomLevelSelected = onPageZoomLevelSelected,
            onPinchToZoomToggled = onPinchToZoomToggled,
            onAutoplayVideoToggled = onAutoplayVideoToggled,
            onAlwaysShowFirstViewOnAppStartToggled = onAlwaysShowFirstViewOnAppStartToggled,
            onNfcTagsClicked = onNfcTagsClicked,
            onLanguageSelected = onLanguageSelected,
            onThemeSelected = onThemeSelected,
            onBackgroundAccessRowClicked = onBackgroundAccessRowClicked,
        )
        notificationsSection(
            uiState = uiState,
            onNotificationPermissionRowClicked = onNotificationPermissionRowClicked,
            onNotificationChannelsClicked = onNotificationChannelsClicked,
            onNotificationHistoryClicked = onNotificationHistoryClicked,
        )
        assistSection(uiState, onAssistSettingsClicked, onAssistVoiceCommandIntentToggled)
        androidAutoSection(uiState, onAutoFavoritesClicked)
        deviceControlsSection(uiState, onManageDeviceControlsClicked)
        quickSettingsSection(uiState, onManageTilesClicked)
        shortcutsSection(uiState, onManageShortcutsClicked)
        widgetsSection(uiState, onManageWidgetsClicked)
        launcherSection(uiState, onEnableHaLauncherToggled, onSetLauncherAppClicked)
        needHelpSection(onDocsClicked, onDeveloperClicked)
        appVersionInfoSection(
            uiState = uiState,
            onShowChangeLogClicked = onShowChangeLogClicked,
            onChangeLogPopupEnabledToggled = onChangeLogPopupEnabledToggled,
            onChangelogGithubClicked = onChangelogGithubClicked,
            onLicensesClicked = onLicensesClicked,
            onPrivacyClicked = onPrivacyClicked,
            onCrashReportingToggled = onCrashReportingToggled,
        )
    }
}

@Composable
private fun SuggestionBanner(suggestion: SettingsHomeSuggestion, onClicked: () -> Unit, onDismissed: () -> Unit) {
    HASettingsRow(
        primaryText = stringResource(suggestion.title),
        secondaryText = stringResource(suggestion.summary),
        onClicked = onClicked,
    )
    HAPlainButton(text = stringResource(commonR.string.close), onClick = onDismissed)
}

/**
 * A settings row pairing an [HASwitch] with its label, styled as a standalone settings card.
 * Mirrors the `RemoteDebuggingRow`/`AssistWakeWordEnableRow` pattern used elsewhere for switch rows.
 */
@Composable
private fun SwitchRow(
    primaryText: String,
    secondaryText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalHAColorScheme.current
    HASettingsCard(
        modifier = modifier
            .clip(RoundedCornerShape(HARadius.XL))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
            ) {
                Text(
                    text = primaryText,
                    style = HATextStyle.Body,
                    textAlign = TextAlign.Start,
                    color = colorScheme.colorTextPrimary,
                )
                Text(
                    text = secondaryText,
                    style = HATextStyle.BodyMedium,
                    textAlign = TextAlign.Start,
                    color = colorScheme.colorTextSecondary,
                )
            }
            HASwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * A settings row that opens an [AlertDialog] with an [HARadioGroup] to pick one of [options].
 * This is the Compose replacement for the legacy `ListPreference` rows (theme, screen orientation,
 * page zoom, language), which have no existing dialog-picker precedent elsewhere in the codebase.
 */
@Composable
private fun <T> ListPreferenceRow(
    primaryText: String,
    options: List<RadioOption<T>>,
    selectionKey: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    HASettingsRow(
        primaryText = primaryText,
        secondaryText = options.firstOrNull { it.selectionKey == selectionKey }?.headline.orEmpty(),
        onClicked = { showDialog = true },
        modifier = modifier,
    )
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = primaryText) },
            text = {
                HARadioGroup(
                    options = options,
                    selectionKey = selectionKey,
                    onSelect = {
                        onSelect(it.selectionKey)
                        showDialog = false
                    },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {},
        )
    }
}

private fun LazyListScope.serversDevicesSection(
    uiState: SettingsUiState,
    onAddServerClicked: () -> Unit,
    onServerClicked: (Server) -> Unit,
    onWearSettingsClicked: () -> Unit,
) {
    item { HASettingsSubheader(stringResource(commonR.string.servers_devices_category)) }
    items(uiState.servers, key = { it.id }) { server ->
        HASettingsRow(
            primaryText = server.friendlyName,
            secondaryText = server.deviceName.orEmpty(),
            onClicked = { onServerClicked(server) },
        )
    }
    if (uiState.wearSettingsVisible) {
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.wear_os_settings_title),
                secondaryText = stringResource(commonR.string.wear_os_settings_summary),
                onClicked = onWearSettingsClicked,
            )
        }
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.server_add),
            secondaryText = "",
            onClicked = onAddServerClicked,
        )
    }
}

private fun LazyListScope.sensorsSection(onSensorsClicked: () -> Unit, onSensorUpdateFrequencyClicked: () -> Unit) {
    item { HASettingsSubheader(stringResource(commonR.string.sensors)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.sensor_title),
            secondaryText = stringResource(commonR.string.sensor_summary),
            onClicked = onSensorsClicked,
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.sensor_update_frequency),
            secondaryText = stringResource(commonR.string.sensor_update_frequency_summary),
            onClicked = onSensorUpdateFrequencyClicked,
        )
    }
}

private val PAGE_ZOOM_PERCENTAGES = listOf(50, 75, 85, 100, 115, 125, 150, 175, 200)

@Suppress("LongParameterList")
private fun LazyListScope.otherSettingsSection(
    uiState: SettingsUiState,
    onGesturesClicked: () -> Unit,
    onFullscreenToggled: (Boolean) -> Unit,
    onScreenOrientationSelected: (String) -> Unit,
    onKeepScreenOnToggled: (Boolean) -> Unit,
    onPageZoomLevelSelected: (Int) -> Unit,
    onPinchToZoomToggled: (Boolean) -> Unit,
    onAutoplayVideoToggled: (Boolean) -> Unit,
    onAlwaysShowFirstViewOnAppStartToggled: (Boolean) -> Unit,
    onNfcTagsClicked: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onThemeSelected: (NightModeTheme) -> Unit,
    onBackgroundAccessRowClicked: () -> Unit,
) {
    item { HASettingsSubheader(stringResource(commonR.string.other_settings)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.gestures),
            secondaryText = "",
            onClicked = onGesturesClicked,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.fullscreen),
            secondaryText = stringResource(commonR.string.fullscreen_def),
            checked = uiState.fullscreenEnabled,
            onCheckedChange = onFullscreenToggled,
        )
    }
    item {
        val options = listOf(
            RadioOption(
                stringResource(R.string.screen_orientation_option_array_value_system),
                stringResource(commonR.string.screen_orientation_option_label_system),
            ),
            RadioOption(
                stringResource(R.string.screen_orientation_option_array_value_landscape),
                stringResource(commonR.string.screen_orientation_option_label_landscape),
            ),
            RadioOption(
                stringResource(R.string.screen_orientation_option_array_value_portrait),
                stringResource(commonR.string.screen_orientation_option_label_portrait),
            ),
        )
        ListPreferenceRow(
            primaryText = stringResource(commonR.string.screen_orientation_title_settings),
            options = options,
            selectionKey = uiState.screenOrientation,
            onSelect = onScreenOrientationSelected,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.keep_screen_on),
            secondaryText = stringResource(commonR.string.keep_screen_on_def),
            checked = uiState.keepScreenOnEnabled,
            onCheckedChange = onKeepScreenOnToggled,
        )
    }
    item {
        val options = PAGE_ZOOM_PERCENTAGES.map { pct ->
            RadioOption(
                pct,
                if (pct == 100) {
                    stringResource(commonR.string.page_zoom_default, pct)
                } else {
                    stringResource(commonR.string.page_zoom_pct, pct)
                },
            )
        }
        ListPreferenceRow(
            primaryText = stringResource(commonR.string.page_zoom),
            options = options,
            selectionKey = uiState.pageZoomLevel,
            onSelect = onPageZoomLevelSelected,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.pinch_to_zoom),
            secondaryText = stringResource(commonR.string.pinch_to_zoom_def),
            checked = uiState.pinchToZoomEnabled,
            onCheckedChange = onPinchToZoomToggled,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.autoplay_video),
            secondaryText = stringResource(commonR.string.autoplay_video_summary),
            checked = uiState.autoplayVideoEnabled,
            onCheckedChange = onAutoplayVideoToggled,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.always_show_first_view_on_app_start),
            secondaryText = stringResource(commonR.string.always_show_first_view_on_app_start_summary),
            checked = uiState.alwaysShowFirstViewOnAppStartEnabled,
            onCheckedChange = onAlwaysShowFirstViewOnAppStartToggled,
        )
    }
    if (uiState.nfcVisible) {
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.nfc_title_settings),
                secondaryText = stringResource(commonR.string.nfc_summary),
                onClicked = onNfcTagsClicked,
            )
        }
    }
    item {
        val options = uiState.supportedLanguages.map { (label, tag) -> RadioOption(tag, label) }
        ListPreferenceRow(
            primaryText = stringResource(commonR.string.lang_title_settings),
            options = options,
            selectionKey = uiState.currentLanguageTag,
            onSelect = onLanguageSelected,
        )
    }
    item {
        val options = buildList {
            if (uiState.includeSystemThemeOption) {
                add(RadioOption(NightModeTheme.SYSTEM, stringResource(commonR.string.themes_option_label_system)))
            }
            add(RadioOption(NightModeTheme.LIGHT, stringResource(commonR.string.themes_option_label_light)))
            add(RadioOption(NightModeTheme.DARK, stringResource(commonR.string.themes_option_label_dark)))
        }
        ListPreferenceRow(
            primaryText = stringResource(commonR.string.themes_title_settings),
            options = options,
            selectionKey = uiState.currentTheme,
            onSelect = onThemeSelected,
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.background_access_title),
            secondaryText = stringResource(
                if (uiState.backgroundAccessEnabled) {
                    commonR.string.background_access_enabled
                } else {
                    commonR.string.background_access_disabled
                },
            ),
            onClicked = onBackgroundAccessRowClicked,
        )
    }
}

private fun LazyListScope.notificationsSection(
    uiState: SettingsUiState,
    onNotificationPermissionRowClicked: () -> Unit,
    onNotificationChannelsClicked: () -> Unit,
    onNotificationHistoryClicked: () -> Unit,
) {
    item { HASettingsSubheader(stringResource(commonR.string.notifications)) }
    if (uiState.notificationPermissionVisible) {
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.notification_permission),
                secondaryText = stringResource(commonR.string.notification_permission_summary),
                onClicked = onNotificationPermissionRowClicked,
            )
        }
    }
    if (uiState.notificationChannelsVisible) {
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.notification_channels),
                secondaryText = stringResource(commonR.string.notification_channels_summary),
                onClicked = onNotificationChannelsClicked,
            )
        }
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.notification_history),
            secondaryText = stringResource(commonR.string.notification_history_summary),
            onClicked = onNotificationHistoryClicked,
        )
    }
    uiState.notificationRateLimitSummary?.let { summary ->
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.rate_limit_title),
                secondaryText = summary,
                onClicked = {},
            )
        }
    }
}

private fun LazyListScope.assistSection(
    uiState: SettingsUiState,
    onAssistSettingsClicked: () -> Unit,
    onAssistVoiceCommandIntentToggled: (Boolean) -> Unit,
) {
    if (!uiState.assistCategoryVisible) return
    item { HASettingsSubheader(stringResource(commonR.string.assist)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.assist_for_android),
            secondaryText = stringResource(commonR.string.assist_summary),
            onClicked = onAssistSettingsClicked,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.open_with_headphone_button),
            secondaryText = stringResource(commonR.string.open_with_headphone_button_summary),
            checked = uiState.assistVoiceCommandIntentEnabled,
            onCheckedChange = onAssistVoiceCommandIntentToggled,
        )
    }
}

private fun LazyListScope.androidAutoSection(uiState: SettingsUiState, onAutoFavoritesClicked: () -> Unit) {
    if (!uiState.androidAutoCategoryVisible) return
    item {
        val titleRes = if (uiState.isAutomotive) {
            commonR.string.android_automotive
        } else {
            commonR.string.basic_sensor_name_android_auto
        }
        HASettingsSubheader(stringResource(titleRes))
    }
    item {
        HASettingsRow(
            primaryText = stringResource(
                if (uiState.isAutomotive) commonR.string.android_automotive_favorites else commonR.string.aa_favorites,
            ),
            secondaryText = stringResource(commonR.string.aa_favorites_summary),
            onClicked = onAutoFavoritesClicked,
        )
    }
}

private fun LazyListScope.deviceControlsSection(uiState: SettingsUiState, onManageDeviceControlsClicked: () -> Unit) {
    if (!uiState.deviceControlsCategoryVisible) return
    item { HASettingsSubheader(stringResource(commonR.string.controls_setting_category)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.controls_setting_title),
            secondaryText = stringResource(commonR.string.controls_setting_summary),
            onClicked = onManageDeviceControlsClicked,
        )
    }
}

private fun LazyListScope.quickSettingsSection(uiState: SettingsUiState, onManageTilesClicked: () -> Unit) {
    if (!uiState.quickSettingsCategoryVisible) return
    item { HASettingsSubheader(stringResource(commonR.string.quick_settings)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.manage_tiles),
            secondaryText = stringResource(commonR.string.manage_tiles_summary),
            onClicked = onManageTilesClicked,
        )
    }
}

private fun LazyListScope.shortcutsSection(uiState: SettingsUiState, onManageShortcutsClicked: () -> Unit) {
    if (!uiState.shortcutsCategoryVisible) return
    item { HASettingsSubheader(stringResource(commonR.string.shortcuts)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.manage_shortcuts),
            secondaryText = stringResource(commonR.string.manage_shortcuts_summary),
            onClicked = onManageShortcutsClicked,
        )
    }
}

private fun LazyListScope.widgetsSection(uiState: SettingsUiState, onManageWidgetsClicked: () -> Unit) {
    if (!uiState.widgetsCategoryVisible) return
    item { HASettingsSubheader(stringResource(commonR.string.widgets)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.manage_widgets),
            secondaryText = stringResource(commonR.string.manage_widgets_summary),
            onClicked = onManageWidgetsClicked,
        )
    }
}

private fun LazyListScope.launcherSection(
    uiState: SettingsUiState,
    onEnableHaLauncherToggled: (Boolean) -> Unit,
    onSetLauncherAppClicked: () -> Unit,
) {
    if (uiState.isAutomotive) return
    item { HASettingsSubheader(stringResource(commonR.string.launcher)) }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.launcher_option_title),
            secondaryText = stringResource(commonR.string.launcher_option_summary),
            checked = uiState.enableHaLauncherEnabled,
            onCheckedChange = onEnableHaLauncherToggled,
        )
    }
    if (uiState.launcherAppRowVisible) {
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.default_launcher_prompt),
                secondaryText = stringResource(
                    commonR.string.default_launcher_prompt_def,
                    uiState.defaultLauncherLabel,
                ),
                onClicked = onSetLauncherAppClicked,
            )
        }
    }
}

private fun LazyListScope.needHelpSection(onDocsClicked: () -> Unit, onDeveloperClicked: () -> Unit) {
    item { HASettingsSubheader(stringResource(commonR.string.need_help)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.documentation),
            secondaryText = "https://companion.home-assistant.io",
            onClicked = onDocsClicked,
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.troubleshooting),
            secondaryText = stringResource(commonR.string.troubleshooting_summary),
            onClicked = onDeveloperClicked,
        )
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.appVersionInfoSection(
    uiState: SettingsUiState,
    onShowChangeLogClicked: () -> Unit,
    onChangeLogPopupEnabledToggled: (Boolean) -> Unit,
    onChangelogGithubClicked: () -> Unit,
    onLicensesClicked: () -> Unit,
    onPrivacyClicked: () -> Unit,
    onCrashReportingToggled: (Boolean) -> Unit,
) {
    item { HASettingsSubheader(stringResource(commonR.string.app_version_info)) }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.show_changelog),
            secondaryText = stringResource(commonR.string.show_changelog_summary),
            onClicked = onShowChangeLogClicked,
        )
    }
    item {
        SwitchRow(
            primaryText = stringResource(commonR.string.enable_change_log_popup),
            secondaryText = stringResource(commonR.string.enable_change_log_popup_summary),
            checked = uiState.changeLogPopupEnabled,
            onCheckedChange = onChangeLogPopupEnabledToggled,
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.changelog),
            secondaryText = uiState.changelogGithubUrl,
            onClicked = onChangelogGithubClicked,
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.application_version),
            secondaryText = BuildConfig.VERSION_NAME,
            onClicked = {},
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.licenses),
            secondaryText = stringResource(commonR.string.licenses_summary),
            onClicked = onLicensesClicked,
        )
    }
    item {
        HASettingsRow(
            primaryText = stringResource(commonR.string.privacy_policy),
            secondaryText = stringResource(commonR.string.privacy_url),
            onClicked = onPrivacyClicked,
        )
    }
    if (uiState.crashReportingVisible) {
        item {
            SwitchRow(
                primaryText = stringResource(commonR.string.crash_reporting),
                secondaryText = stringResource(commonR.string.crash_reporting_summary),
                checked = uiState.crashReportingEnabled,
                onCheckedChange = onCrashReportingToggled,
            )
        }
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    HAThemeForPreview {
        SettingsContent(
            uiState = SettingsUiState(
                servers = emptyList(),
                assistCategoryVisible = true,
                widgetsCategoryVisible = true,
                notificationRateLimitSummary = "\nSuccessful: 10       Errors: 0",
                crashReportingVisible = true,
                currentTheme = NightModeTheme.SYSTEM,
                supportedLanguages = mapOf("Default" to "default", "English" to "en"),
                defaultLauncherLabel = "Home Assistant",
                enableHaLauncherEnabled = true,
                suggestion = SettingsHomeSuggestion(
                    SettingsViewModel.SUGGESTION_ASSISTANT_APP,
                    commonR.string.suggestion_assist_title,
                    commonR.string.suggestion_assist_summary,
                    R.drawable.ic_comment_processing_outline,
                ),
            ),
            onAddServerClicked = {},
            onServerClicked = {},
            onWearSettingsClicked = {},
            onSensorsClicked = {},
            onSensorUpdateFrequencyClicked = {},
            onGesturesClicked = {},
            onFullscreenToggled = {},
            onScreenOrientationSelected = {},
            onKeepScreenOnToggled = {},
            onPageZoomLevelSelected = {},
            onPinchToZoomToggled = {},
            onAutoplayVideoToggled = {},
            onAlwaysShowFirstViewOnAppStartToggled = {},
            onNfcTagsClicked = {},
            onLanguageSelected = {},
            onThemeSelected = {},
            onBackgroundAccessRowClicked = {},
            onNotificationPermissionRowClicked = {},
            onNotificationChannelsClicked = {},
            onNotificationHistoryClicked = {},
            onAssistSettingsClicked = {},
            onAssistVoiceCommandIntentToggled = {},
            onAutoFavoritesClicked = {},
            onManageDeviceControlsClicked = {},
            onManageTilesClicked = {},
            onManageShortcutsClicked = {},
            onManageWidgetsClicked = {},
            onEnableHaLauncherToggled = {},
            onSetLauncherAppClicked = {},
            onDocsClicked = {},
            onDeveloperClicked = {},
            onShowChangeLogClicked = {},
            onChangeLogPopupEnabledToggled = {},
            onChangelogGithubClicked = {},
            onLicensesClicked = {},
            onPrivacyClicked = {},
            onCrashReportingToggled = {},
            onSuggestionClicked = {},
            onSuggestionDismissed = {},
        )
    }
}

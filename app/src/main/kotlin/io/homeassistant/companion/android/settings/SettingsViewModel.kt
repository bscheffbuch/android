package io.homeassistant.companion.android.settings

import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.prefs.NightModeTheme
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.util.isAutomotive
import io.homeassistant.companion.android.common.util.isIgnoringBatteryOptimizations
import io.homeassistant.companion.android.common.util.maybeAskForIgnoringBatteryOptimizations
import io.homeassistant.companion.android.settings.assist.DefaultAssistantManager
import io.homeassistant.companion.android.settings.language.LanguagesManager
import io.homeassistant.companion.android.settings.language.LanguagesProvider
import io.homeassistant.companion.android.settings.wear.SettingsWearDetection
import io.homeassistant.companion.android.themes.NightModeManager
import io.homeassistant.companion.android.util.ChangeLog
import io.homeassistant.companion.android.util.QuestUtil
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private val voiceCommandAppComponent = ComponentName(
    BuildConfig.APPLICATION_ID,
    "io.homeassistant.companion.android.assist.VoiceCommandIntentActivity",
)

private val launcherAliasComponent = ComponentName(
    BuildConfig.APPLICATION_ID,
    "io.homeassistant.companion.android.launch.LauncherAlias",
)

/**
 * ViewModel backing the top-level Settings screen ([SettingsScreen]). This is the MVVM
 * replacement for the legacy `SettingsPresenterImpl`/`PreferenceDataStore` pair: every
 * repository/manager call and branching condition below is ported call-for-call from that
 * presenter, merely restructured from string-keyed preference routing into named methods.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverManager: ServerManager,
    private val prefsRepository: PrefsRepository,
    private val nightModeManager: NightModeManager,
    private val langsManager: LanguagesManager,
    private val langsProvider: LanguagesProvider,
    private val changeLog: ChangeLog,
    private val defaultAssistantManager: DefaultAssistantManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isAutomotive = appContext.isAutomotive(),
            includeSystemThemeOption = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
        ),
    )

    /** Current state of the top-level Settings screen. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val isAutomotive = _uiState.value.isAutomotive
        _uiState.update {
            it.copy(
                assistCategoryVisible = !isAutomotive,
                widgetsCategoryVisible = !QuestUtil.isQuest && !isAutomotive,
                shortcutsCategoryVisible = !QuestUtil.isQuest && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1,
                quickSettingsCategoryVisible = !QuestUtil.isQuest && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
                deviceControlsCategoryVisible = !QuestUtil.isQuest &&
                    !isAutomotive &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                androidAutoCategoryVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    (BuildConfig.FLAVOR == "full" || isAutomotive),
                crashReportingVisible = BuildConfig.FLAVOR == "full",
                changelogGithubUrl = buildChangelogGithubUrl(),
            )
        }

        serverManager.serversFlow.onEach { servers -> _uiState.update { it.copy(servers = servers) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val hasWearNodes = SettingsWearDetection.hasAnyNodes(appContext)
            _uiState.update { it.copy(wearSettingsVisible = hasWearNodes) }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fullscreenEnabled = prefsRepository.isFullScreenEnabled(),
                    keepScreenOnEnabled = prefsRepository.isKeepScreenOnEnabled(),
                    pinchToZoomEnabled = prefsRepository.isPinchToZoomEnabled(),
                    crashReportingEnabled = prefsRepository.isCrashReporting(),
                    autoplayVideoEnabled = prefsRepository.isAutoPlayVideoEnabled(),
                    alwaysShowFirstViewOnAppStartEnabled = prefsRepository.isAlwaysShowFirstViewOnAppStartEnabled(),
                    changeLogPopupEnabled = prefsRepository.isChangeLogPopupEnabled(),
                    screenOrientation = prefsRepository.getScreenOrientation(),
                    pageZoomLevel = prefsRepository.getPageZoomLevel(),
                    currentTheme = nightModeManager.getCurrentNightMode(),
                    currentLanguageTag = langsManager.getCurrentLang(),
                    assistVoiceCommandIntentEnabled = isComponentEnabled(voiceCommandAppComponent),
                    enableHaLauncherEnabled = isLauncherAliasEnabled(),
                    nfcVisible = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
                    backgroundAccessEnabled = appContext.isIgnoringBatteryOptimizations(),
                )
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(supportedLanguages = langsProvider.getSupportedLanguages(appContext)) }
        }

        refreshNotificationChannelState()

        if (BuildConfig.FLAVOR == "full") {
            viewModelScope.launch {
                val summary = formatNotificationRateLimitSummary()
                _uiState.update { it.copy(notificationRateLimitSummary = summary) }
            }
        }

        refreshOnResume()
    }

    private fun buildChangelogGithubUrl(): String = if (BuildConfig.VERSION_NAME.startsWith("LOCAL")) {
        "https://github.com/home-assistant/android/releases"
    } else {
        "https://github.com/home-assistant/android/releases/tag/" +
            BuildConfig.VERSION_NAME.replace("-full", "").replace("-minimal", "")
    }

    private fun isComponentEnabled(component: ComponentName): Boolean {
        val setting = appContext.packageManager.getComponentEnabledSetting(component)
        return setting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun isLauncherAliasEnabled(): Boolean {
        val setting = appContext.packageManager.getComponentEnabledSetting(launcherAliasComponent)
        return setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private suspend fun formatNotificationRateLimitSummary(): String? = withContext(Dispatchers.IO) {
        val rateLimits = try {
            if (serverManager.isRegistered()) {
                serverManager.integrationRepository().getNotificationRateLimits()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.d(e, "Unable to get rate limits")
            null
        } ?: return@withContext null

        var formattedDate = rateLimits.resetsAt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val utcDateTime = Instant.parse(rateLimits.resetsAt)
                formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .format(utcDateTime.atZone(ZoneId.systemDefault()))
            } catch (e: Exception) {
                Timber.d(e, "Cannot parse notification rate limit date \"${rateLimits.resetsAt}\"")
            }
        }
        "\n${appContext.getString(commonR.string.successful)}: ${rateLimits.successful}       " +
            "${appContext.getString(commonR.string.errors)}: ${rateLimits.errors}" +
            "\n\n${appContext.getString(commonR.string.remaining)}/${appContext.getString(commonR.string.maximum)}: " +
            "${rateLimits.remaining}/${rateLimits.maximum}" +
            "\n\n${appContext.getString(commonR.string.resets_at)}: $formattedDate"
    }

    /**
     * Re-evaluate whether the notification permission row and the notification channels row
     * should be visible. Called once on startup and again after the user returns from the system
     * notification settings screen, since granting the permission there does not otherwise notify
     * this ViewModel.
     */
    fun refreshNotificationChannelState() {
        val notificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        val uiManager = appContext.getSystemService<UiModeManager>()
        _uiState.update {
            it.copy(
                notificationPermissionVisible = !notificationsEnabled,
                notificationChannelsVisible = notificationsEnabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    uiManager?.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION,
            )
        }
    }

    /**
     * Handle a click on the "background access" row: opens the system battery optimization
     * exemption screen, unless the app is already exempt (in which case the row is inert).
     */
    fun onBackgroundAccessRowClicked(activityContext: Context) {
        if (_uiState.value.backgroundAccessEnabled) return
        activityContext.maybeAskForIgnoringBatteryOptimizations()
    }

    /** Toggle whether the app can be launched from headphone button assist intents. */
    fun onAssistVoiceCommandIntentToggled(enabled: Boolean) {
        _uiState.update { it.copy(assistVoiceCommandIntentEnabled = enabled) }
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        viewModelScope.launch {
            appContext.packageManager.setComponentEnabledSetting(
                voiceCommandAppComponent,
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    /** Toggle whether the WebView frontend runs in fullscreen (immersive) mode. */
    fun onFullscreenToggled(enabled: Boolean) {
        _uiState.update { it.copy(fullscreenEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setFullScreenEnabled(enabled) }
    }

    /** Toggle whether the device screen is kept on while the app is in the foreground. */
    fun onKeepScreenOnToggled(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenOnEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setKeepScreenOnEnabled(enabled) }
    }

    /** Toggle whether pinch-to-zoom gestures are enabled in the WebView frontend. */
    fun onPinchToZoomToggled(enabled: Boolean) {
        _uiState.update { it.copy(pinchToZoomEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setPinchToZoomEnabled(enabled) }
    }

    /** Toggle whether videos in the frontend are allowed to autoplay. */
    fun onAutoplayVideoToggled(enabled: Boolean) {
        _uiState.update { it.copy(autoplayVideoEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setAutoPlayVideo(enabled) }
    }

    /** Toggle whether the app always opens the first dashboard view on start, ignoring the last-viewed one. */
    fun onAlwaysShowFirstViewOnAppStartToggled(enabled: Boolean) {
        _uiState.update { it.copy(alwaysShowFirstViewOnAppStartEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setAlwaysShowFirstViewOnAppStart(enabled) }
    }

    /** Toggle whether crash reports are sent to the crash reporting service. */
    fun onCrashReportingToggled(enabled: Boolean) {
        _uiState.update { it.copy(crashReportingEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setCrashReporting(enabled) }
    }

    /** Toggle whether the changelog dialog is automatically shown after an app update. */
    fun onChangeLogPopupEnabledToggled(enabled: Boolean) {
        _uiState.update { it.copy(changeLogPopupEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setChangeLogPopupEnabled(enabled) }
    }

    /** Toggle whether the app is registered as an alternative launcher (home screen) app. */
    fun onEnableHaLauncherToggled(enabled: Boolean) {
        _uiState.update { it.copy(enableHaLauncherEnabled = enabled) }
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        viewModelScope.launch {
            appContext.packageManager.setComponentEnabledSetting(
                launcherAliasComponent,
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    /**
     * Persist the selected screen orientation. The value must be one of the raw strings defined
     * in `arrays.xml`'s `pref_screen_orientation_option_values` (e.g. "system", "landscape",
     * "portrait"), since [io.homeassistant.companion.android.webview.WebViewActivity] compares
     * against these exact string resources.
     */
    fun onScreenOrientationSelected(value: String?) {
        _uiState.update { it.copy(screenOrientation = value) }
        viewModelScope.launch { prefsRepository.saveScreenOrientation(value) }
    }

    /** Persist the selected WebView page zoom level, as a percentage. */
    fun onPageZoomLevelSelected(level: Int) {
        _uiState.update { it.copy(pageZoomLevel = level) }
        viewModelScope.launch { prefsRepository.setPageZoomLevel(level) }
    }

    /** Persist the selected app display language, identified by its BCP 47 language tag. */
    fun onLanguageSelected(languageTag: String) {
        _uiState.update { it.copy(currentLanguageTag = languageTag) }
        viewModelScope.launch {
            langsManager.saveLang(languageTag)
            _uiState.update { it.copy(currentLanguageTag = langsManager.getCurrentLang()) }
        }
    }

    /** Persist the selected app night mode theme. */
    fun onThemeSelected(theme: NightModeTheme) {
        _uiState.update { it.copy(currentTheme = theme) }
        viewModelScope.launch { nightModeManager.saveNightMode(theme) }
    }

    /** Returns the intent to launch the system flow for changing the default assistant app. */
    fun getSetDefaultAssistantIntent(): Intent = defaultAssistantManager.getSetDefaultAssistantIntent()

    /** Show the full changelog dialog, forced regardless of the "show on update" preference. */
    fun onShowChangeLogClicked(activityContext: Context) {
        viewModelScope.launch { changeLog.showChangeLog(activityContext, forceShow = true) }
    }

    /** Dismiss the currently displayed suggestion banner without re-showing it this session. */
    fun onSuggestionDismissed() {
        val id = _uiState.value.suggestion?.id ?: return
        _uiState.update { it.copy(suggestion = null) }
        viewModelScope.launch {
            val ignored = prefsRepository.getIgnoredSuggestions()
            if (!ignored.contains(id)) {
                prefsRepository.setIgnoredSuggestions(ignored + id)
            }
            refreshSuggestions(overwrite = true)
        }
    }

    /** Re-check preconditions for showing a suggestion banner, called when returning to this screen. */
    fun refreshOnResume() {
        viewModelScope.launch {
            refreshSuggestions(overwrite = false)
            _uiState.update { it.copy(defaultLauncherLabel = getDefaultLauncherInfo()) }
        }
    }

    private suspend fun refreshSuggestions(overwrite: Boolean) {
        val suggestions = mutableListOf<SettingsHomeSuggestion>()

        if (defaultAssistantManager.shouldSuggestAssistantSetup()) {
            suggestions += SettingsHomeSuggestion(
                SUGGESTION_ASSISTANT_APP,
                commonR.string.suggestion_assist_title,
                commonR.string.suggestion_assist_summary,
                R.drawable.ic_comment_processing_outline,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        ) {
            suggestions += SettingsHomeSuggestion(
                SUGGESTION_NOTIFICATION_PERMISSION,
                commonR.string.suggestion_notifications_title,
                commonR.string.suggestion_notifications_summary,
                commonR.drawable.ic_notifications,
            )
        }

        val ignored = prefsRepository.getIgnoredSuggestions()
        val filteredSuggestions = suggestions.filter { !ignored.contains(it.id) }
        val current = _uiState.value.suggestion
        if (overwrite || current == null) {
            _uiState.update { it.copy(suggestion = filteredSuggestions.randomOrNull()) }
        } else if (filteredSuggestions.none { it.id == current.id }) {
            _uiState.update { it.copy(suggestion = null) }
        }
    }

    private fun getDefaultLauncherInfo(): String {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val packageManager = appContext.packageManager
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.let {
            val packageName = it.activityInfo.packageName
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }
        return appContext.getString(commonR.string.unknown_launcher_label)
    }

    /** Record the result of an add-server flow, to be surfaced as a Snackbar with an "Activate" action. */
    fun onAddServerResult(success: Boolean, serverId: Int?) {
        _uiState.update { it.copy(addServerResult = AddServerResult(success, serverId)) }
    }

    /** Acknowledge the currently displayed add-server Snackbar so it isn't shown again. */
    fun onAddServerResultAcknowledged() {
        _uiState.update { it.copy(addServerResult = null) }
    }

    companion object {
        const val SUGGESTION_ASSISTANT_APP = "assistant_app"
        const val SUGGESTION_NOTIFICATION_PERMISSION = "notification_permission"
    }
}

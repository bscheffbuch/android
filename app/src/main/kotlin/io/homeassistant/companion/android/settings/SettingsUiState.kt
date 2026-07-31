package io.homeassistant.companion.android.settings

import io.homeassistant.companion.android.common.data.prefs.NightModeTheme
import io.homeassistant.companion.android.database.server.Server

/**
 * UI state for the top-level Settings screen ([SettingsScreen]).
 */
data class SettingsUiState(
    val isAutomotive: Boolean = false,
    val servers: List<Server> = emptyList(),
    val wearSettingsVisible: Boolean = false,
    val suggestion: SettingsHomeSuggestion? = null,
    val assistCategoryVisible: Boolean = true,
    val assistVoiceCommandIntentEnabled: Boolean = false,
    val widgetsCategoryVisible: Boolean = true,
    val shortcutsCategoryVisible: Boolean = false,
    val quickSettingsCategoryVisible: Boolean = false,
    val deviceControlsCategoryVisible: Boolean = false,
    val notificationPermissionVisible: Boolean = false,
    val notificationChannelsVisible: Boolean = false,
    val notificationRateLimitSummary: String? = null,
    val crashReportingVisible: Boolean = false,
    val crashReportingEnabled: Boolean = false,
    val fullscreenEnabled: Boolean = false,
    val screenOrientation: String? = null,
    val keepScreenOnEnabled: Boolean = false,
    val pageZoomLevel: Int = 100,
    val pinchToZoomEnabled: Boolean = false,
    val autoplayVideoEnabled: Boolean = false,
    val alwaysShowFirstViewOnAppStartEnabled: Boolean = false,
    val nfcVisible: Boolean = false,
    val currentLanguageTag: String? = null,
    val supportedLanguages: Map<String, String> = emptyMap(),
    val currentTheme: NightModeTheme? = null,
    val includeSystemThemeOption: Boolean = true,
    val backgroundAccessEnabled: Boolean = false,
    val androidAutoCategoryVisible: Boolean = false,
    val changelogGithubUrl: String = "",
    val changeLogPopupEnabled: Boolean = false,
    val enableHaLauncherEnabled: Boolean = false,
    val defaultLauncherLabel: String = "",
    val addServerResult: AddServerResult? = null,
) {
    /** Whether the "Set as launcher app" row should currently be shown, per the enable switch. */
    val launcherAppRowVisible: Boolean get() = enableHaLauncherEnabled
}

/** Result of a just-completed add-server flow, surfaced as a Snackbar with an "Activate" action. */
data class AddServerResult(val success: Boolean, val serverId: Int?)

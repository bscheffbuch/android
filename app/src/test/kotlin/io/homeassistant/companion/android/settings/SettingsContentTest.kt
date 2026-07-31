package io.homeassistant.companion.android.settings

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.prefs.NightModeTheme
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class SettingsContentTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Suppress("LongParameterList")
    private fun setContent(
        uiState: SettingsUiState,
        onAddServerClicked: () -> Unit = {},
        onServerClicked: (Server) -> Unit = {},
        onWearSettingsClicked: () -> Unit = {},
        onSensorsClicked: () -> Unit = {},
        onSensorUpdateFrequencyClicked: () -> Unit = {},
        onGesturesClicked: () -> Unit = {},
        onFullscreenToggled: (Boolean) -> Unit = {},
        onScreenOrientationSelected: (String) -> Unit = {},
        onKeepScreenOnToggled: (Boolean) -> Unit = {},
        onPageZoomLevelSelected: (Int) -> Unit = {},
        onPinchToZoomToggled: (Boolean) -> Unit = {},
        onAutoplayVideoToggled: (Boolean) -> Unit = {},
        onAlwaysShowFirstViewOnAppStartToggled: (Boolean) -> Unit = {},
        onNfcTagsClicked: () -> Unit = {},
        onLanguageSelected: (String) -> Unit = {},
        onThemeSelected: (NightModeTheme) -> Unit = {},
        onBackgroundAccessRowClicked: () -> Unit = {},
        onNotificationPermissionRowClicked: () -> Unit = {},
        onNotificationChannelsClicked: () -> Unit = {},
        onNotificationHistoryClicked: () -> Unit = {},
        onAssistSettingsClicked: () -> Unit = {},
        onAssistVoiceCommandIntentToggled: (Boolean) -> Unit = {},
        onAutoFavoritesClicked: () -> Unit = {},
        onManageDeviceControlsClicked: () -> Unit = {},
        onManageTilesClicked: () -> Unit = {},
        onManageShortcutsClicked: () -> Unit = {},
        onManageWidgetsClicked: () -> Unit = {},
        onEnableHaLauncherToggled: (Boolean) -> Unit = {},
        onSetLauncherAppClicked: () -> Unit = {},
        onDocsClicked: () -> Unit = {},
        onDeveloperClicked: () -> Unit = {},
        onShowChangeLogClicked: () -> Unit = {},
        onChangeLogPopupEnabledToggled: (Boolean) -> Unit = {},
        onChangelogGithubClicked: () -> Unit = {},
        onLicensesClicked: () -> Unit = {},
        onPrivacyClicked: () -> Unit = {},
        onCrashReportingToggled: (Boolean) -> Unit = {},
        onSuggestionClicked: () -> Unit = {},
        onSuggestionDismissed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                SettingsContent(
                    uiState = uiState,
                    onAddServerClicked = onAddServerClicked,
                    onServerClicked = onServerClicked,
                    onWearSettingsClicked = onWearSettingsClicked,
                    onSensorsClicked = onSensorsClicked,
                    onSensorUpdateFrequencyClicked = onSensorUpdateFrequencyClicked,
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
                    onNotificationPermissionRowClicked = onNotificationPermissionRowClicked,
                    onNotificationChannelsClicked = onNotificationChannelsClicked,
                    onNotificationHistoryClicked = onNotificationHistoryClicked,
                    onAssistSettingsClicked = onAssistSettingsClicked,
                    onAssistVoiceCommandIntentToggled = onAssistVoiceCommandIntentToggled,
                    onAutoFavoritesClicked = onAutoFavoritesClicked,
                    onManageDeviceControlsClicked = onManageDeviceControlsClicked,
                    onManageTilesClicked = onManageTilesClicked,
                    onManageShortcutsClicked = onManageShortcutsClicked,
                    onManageWidgetsClicked = onManageWidgetsClicked,
                    onEnableHaLauncherToggled = onEnableHaLauncherToggled,
                    onSetLauncherAppClicked = onSetLauncherAppClicked,
                    onDocsClicked = onDocsClicked,
                    onDeveloperClicked = onDeveloperClicked,
                    onShowChangeLogClicked = onShowChangeLogClicked,
                    onChangeLogPopupEnabledToggled = onChangeLogPopupEnabledToggled,
                    onChangelogGithubClicked = onChangelogGithubClicked,
                    onLicensesClicked = onLicensesClicked,
                    onPrivacyClicked = onPrivacyClicked,
                    onCrashReportingToggled = onCrashReportingToggled,
                    onSuggestionClicked = onSuggestionClicked,
                    onSuggestionDismissed = onSuggestionDismissed,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun fakeServer(id: Int, friendlyName: String, deviceName: String? = null): Server = mockk<Server>(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.friendlyName } returns friendlyName
        every { this@mockk.deviceName } returns deviceName
    }

    /**
     * Scrolls the settings [androidx.compose.foundation.lazy.LazyColumn] until a node matching
     * [text] is composed. Rows beyond the initial viewport are not present in the semantics tree
     * until scrolled into view, so this must run before interacting with any row.
     */
    private fun scrollToText(text: String, substring: Boolean = false) {
        composeTestRule.onNodeWithTag(SETTINGS_LIST_TEST_TAG)
            .performScrollToNode(hasText(text, substring = substring))
    }

    private fun clickText(text: String) {
        scrollToText(text)
        composeTestRule.onNodeWithText(text).performClick()
    }

    private fun assertTextExists(text: String, substring: Boolean = false) {
        scrollToText(text, substring = substring)
        composeTestRule.onNodeWithText(text, substring = substring).assertExists()
    }

    /**
     * Asserts a row is genuinely absent (e.g. hidden by a uiState flag) rather than merely
     * off-screen. [performScrollToNode] scrolls through the entire list before giving up, so a
     * thrown [AssertionError] reliably proves the node can never be found.
     */
    private fun assertTextAbsent(text: String) {
        var found = true
        try {
            scrollToText(text)
        } catch (e: AssertionError) {
            found = false
        }
        assertEquals(false, found)
    }

    // --- Servers & devices ---

    @Test
    fun `Given servers when composed then each server row shows its name and clicking invokes onServerClicked`() {
        val server = fakeServer(id = 7, friendlyName = "Home", deviceName = "Pixel")
        var clicked: Server? = null
        setContent(uiState = SettingsUiState(servers = listOf(server)), onServerClicked = { clicked = it })

        clickText("Home")

        assertEquals(server, clicked)
    }

    @Test
    fun `Given the add server row when clicked then onAddServerClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onAddServerClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.server_add))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given wear settings not visible when composed then the wear os row does not exist`() {
        setContent(uiState = SettingsUiState(wearSettingsVisible = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.wear_os_settings_title))
    }

    @Test
    fun `Given wear settings visible when clicked then onWearSettingsClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(wearSettingsVisible = true), onWearSettingsClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.wear_os_settings_title))

        assertEquals(true, clicked)
    }

    // --- Sensors ---

    @Test
    fun `Given the sensors row when clicked then onSensorsClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onSensorsClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.sensor_title))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the sensor update frequency row when clicked then onSensorUpdateFrequencyClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onSensorUpdateFrequencyClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.sensor_update_frequency))

        assertEquals(true, clicked)
    }

    // --- Other settings: switch rows and list-preference dialogs ---

    @Test
    fun `Given fullscreen switch when clicked then onFullscreenToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(uiState = SettingsUiState(fullscreenEnabled = false), onFullscreenToggled = { toggledTo = it })

        clickText(composeTestRule.stringResource(commonR.string.fullscreen))

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given the screen orientation row when a new option is picked then onScreenOrientationSelected fires and the dialog closes`() {
        var selected: String? = null
        setContent(uiState = SettingsUiState(screenOrientation = "system"), onScreenOrientationSelected = { selected = it })
        clickText(composeTestRule.stringResource(commonR.string.screen_orientation_title_settings))
        val landscapeLabel = composeTestRule.stringResource(commonR.string.screen_orientation_option_label_landscape)
        composeTestRule.onNodeWithText(landscapeLabel).assertExists()

        composeTestRule.onNodeWithText(landscapeLabel).performClick()

        assertEquals(
            composeTestRule.activity.getString(R.string.screen_orientation_option_array_value_landscape),
            selected,
        )
        composeTestRule.onNodeWithText(landscapeLabel).assertDoesNotExist()
    }

    @Test
    fun `Given the page zoom row when a new percentage is picked then onPageZoomLevelSelected fires`() {
        var selected: Int? = null
        setContent(uiState = SettingsUiState(pageZoomLevel = 100), onPageZoomLevelSelected = { selected = it })
        clickText(composeTestRule.stringResource(commonR.string.page_zoom))

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(commonR.string.page_zoom_pct, 150))
            .performScrollTo()
            .performClick()

        assertEquals(150, selected)
    }

    @Test
    fun `Given keep screen on switch when clicked then onKeepScreenOnToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(uiState = SettingsUiState(keepScreenOnEnabled = false), onKeepScreenOnToggled = { toggledTo = it })

        clickText(composeTestRule.stringResource(commonR.string.keep_screen_on))

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given pinch to zoom switch when clicked then onPinchToZoomToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(uiState = SettingsUiState(pinchToZoomEnabled = true), onPinchToZoomToggled = { toggledTo = it })

        clickText(composeTestRule.stringResource(commonR.string.pinch_to_zoom))

        assertEquals(false, toggledTo)
    }

    @Test
    fun `Given autoplay video switch when clicked then onAutoplayVideoToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(uiState = SettingsUiState(autoplayVideoEnabled = false), onAutoplayVideoToggled = { toggledTo = it })

        clickText(composeTestRule.stringResource(commonR.string.autoplay_video))

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given always show first view switch when clicked then onAlwaysShowFirstViewOnAppStartToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(
            uiState = SettingsUiState(alwaysShowFirstViewOnAppStartEnabled = false),
            onAlwaysShowFirstViewOnAppStartToggled = { toggledTo = it },
        )

        clickText(composeTestRule.stringResource(commonR.string.always_show_first_view_on_app_start))

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given nfc not visible when composed then the nfc row does not exist`() {
        setContent(uiState = SettingsUiState(nfcVisible = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.nfc_title_settings))
    }

    @Test
    fun `Given nfc visible when clicked then onNfcTagsClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(nfcVisible = true), onNfcTagsClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.nfc_title_settings))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given supported languages when a new language is picked then onLanguageSelected fires with its tag`() {
        var selected: String? = null
        setContent(
            uiState = SettingsUiState(
                supportedLanguages = mapOf("Default" to "default", "English" to "en"),
                currentLanguageTag = "default",
            ),
            onLanguageSelected = { selected = it },
        )
        clickText(composeTestRule.stringResource(commonR.string.lang_title_settings))

        composeTestRule.onNodeWithText("English").performClick()

        assertEquals("en", selected)
    }

    @Test
    fun `Given the theme row when a new theme is picked then onThemeSelected fires with the enum value`() {
        var selected: NightModeTheme? = null
        setContent(
            uiState = SettingsUiState(currentTheme = NightModeTheme.SYSTEM, includeSystemThemeOption = true),
            onThemeSelected = { selected = it },
        )
        clickText(composeTestRule.stringResource(commonR.string.themes_title_settings))

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.themes_option_label_dark))
            .performClick()

        assertEquals(NightModeTheme.DARK, selected)
    }

    @Test
    fun `Given the background access row when clicked then onBackgroundAccessRowClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onBackgroundAccessRowClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.background_access_title))

        assertEquals(true, clicked)
    }

    // --- Notifications ---

    @Test
    fun `Given notification permission and channels hidden when composed then neither row exists`() {
        setContent(
            uiState = SettingsUiState(notificationPermissionVisible = false, notificationChannelsVisible = false),
        )

        assertTextAbsent(composeTestRule.stringResource(commonR.string.notification_permission))
        assertTextAbsent(composeTestRule.stringResource(commonR.string.notification_channels))
    }

    @Test
    fun `Given notification permission visible when clicked then onNotificationPermissionRowClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(notificationPermissionVisible = true),
            onNotificationPermissionRowClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.notification_permission))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given notification channels visible when clicked then onNotificationChannelsClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(notificationChannelsVisible = true),
            onNotificationChannelsClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.notification_channels))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the notification history row when clicked then onNotificationHistoryClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onNotificationHistoryClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.notification_history))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given a rate limit summary when composed then it is shown as its own row`() {
        setContent(uiState = SettingsUiState(notificationRateLimitSummary = "\nSuccessful: 10       Errors: 0"))

        assertTextExists("Successful: 10       Errors: 0", substring = true)
    }

    // --- Assist ---

    @Test
    fun `Given assist category not visible when composed then the assist section does not exist`() {
        setContent(uiState = SettingsUiState(assistCategoryVisible = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.assist_for_android))
        assertTextAbsent(composeTestRule.stringResource(commonR.string.open_with_headphone_button))
    }

    @Test
    fun `Given assist category visible when the row is clicked then onAssistSettingsClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(assistCategoryVisible = true), onAssistSettingsClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.assist_for_android))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the headphone button switch when clicked then onAssistVoiceCommandIntentToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(
            uiState = SettingsUiState(assistCategoryVisible = true, assistVoiceCommandIntentEnabled = false),
            onAssistVoiceCommandIntentToggled = { toggledTo = it },
        )

        clickText(composeTestRule.stringResource(commonR.string.open_with_headphone_button))

        assertEquals(true, toggledTo)
    }

    // --- Android Auto / Automotive ---

    @Test
    fun `Given android auto category not visible when composed then the section does not exist`() {
        setContent(uiState = SettingsUiState(androidAutoCategoryVisible = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.aa_favorites))
    }

    @Test
    fun `Given phone android auto category visible when clicked then onAutoFavoritesClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(androidAutoCategoryVisible = true, isAutomotive = false),
            onAutoFavoritesClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.aa_favorites))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given automotive android auto category visible when composed then the automotive labels are used`() {
        setContent(uiState = SettingsUiState(androidAutoCategoryVisible = true, isAutomotive = true))

        assertTextExists(composeTestRule.stringResource(commonR.string.android_automotive_favorites))
        assertTextAbsent(composeTestRule.stringResource(commonR.string.aa_favorites))
    }

    // --- Device controls / quick settings / shortcuts / widgets ---

    @Test
    fun `Given device controls category not visible when composed then the row does not exist`() {
        setContent(uiState = SettingsUiState(deviceControlsCategoryVisible = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.controls_setting_title))
    }

    @Test
    fun `Given device controls category visible when clicked then onManageDeviceControlsClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(deviceControlsCategoryVisible = true),
            onManageDeviceControlsClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.controls_setting_title))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given quick settings category visible when clicked then onManageTilesClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(quickSettingsCategoryVisible = true),
            onManageTilesClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.manage_tiles))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given shortcuts category visible when clicked then onManageShortcutsClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(shortcutsCategoryVisible = true),
            onManageShortcutsClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.manage_shortcuts))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given widgets category visible when clicked then onManageWidgetsClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(widgetsCategoryVisible = true),
            onManageWidgetsClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.manage_widgets))

        assertEquals(true, clicked)
    }

    // --- Launcher ---

    @Test
    fun `Given automotive when composed then the launcher section does not exist`() {
        setContent(uiState = SettingsUiState(isAutomotive = true))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.launcher_option_title))
    }

    @Test
    fun `Given the launcher switch when clicked then onEnableHaLauncherToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(
            uiState = SettingsUiState(isAutomotive = false, enableHaLauncherEnabled = false),
            onEnableHaLauncherToggled = { toggledTo = it },
        )

        clickText(composeTestRule.stringResource(commonR.string.launcher_option_title))

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given the launcher not enabled when composed then the set launcher app row does not exist`() {
        setContent(uiState = SettingsUiState(enableHaLauncherEnabled = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.default_launcher_prompt))
    }

    @Test
    fun `Given the launcher enabled when the set launcher app row is clicked then onSetLauncherAppClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(enableHaLauncherEnabled = true, defaultLauncherLabel = "Home Assistant"),
            onSetLauncherAppClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.default_launcher_prompt))

        assertEquals(true, clicked)
    }

    // --- Need help & app version info ---

    @Test
    fun `Given the docs row when clicked then onDocsClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onDocsClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.documentation))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the troubleshooting row when clicked then onDeveloperClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onDeveloperClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.troubleshooting))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the show changelog row when clicked then onShowChangeLogClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onShowChangeLogClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.show_changelog))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the changelog popup switch when clicked then onChangeLogPopupEnabledToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(
            uiState = SettingsUiState(changeLogPopupEnabled = false),
            onChangeLogPopupEnabledToggled = { toggledTo = it },
        )

        clickText(composeTestRule.stringResource(commonR.string.enable_change_log_popup))

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given the changelog github row when clicked then onChangelogGithubClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(changelogGithubUrl = "https://github.com/home-assistant/android/releases"),
            onChangelogGithubClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.changelog))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the licenses row when clicked then onLicensesClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onLicensesClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.licenses))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given the privacy policy row when clicked then onPrivacyClicked is invoked`() {
        var clicked = false
        setContent(uiState = SettingsUiState(), onPrivacyClicked = { clicked = true })

        clickText(composeTestRule.stringResource(commonR.string.privacy_policy))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given crash reporting not visible when composed then the row does not exist`() {
        setContent(uiState = SettingsUiState(crashReportingVisible = false))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.crash_reporting))
    }

    @Test
    fun `Given the crash reporting switch when clicked then onCrashReportingToggled is invoked with the flipped value`() {
        var toggledTo: Boolean? = null
        setContent(
            uiState = SettingsUiState(crashReportingVisible = true, crashReportingEnabled = true),
            onCrashReportingToggled = { toggledTo = it },
        )

        clickText(composeTestRule.stringResource(commonR.string.crash_reporting))

        assertEquals(false, toggledTo)
    }

    // --- Suggestion banner ---

    @Test
    fun `Given no suggestion when composed then no suggestion banner is shown`() {
        setContent(uiState = SettingsUiState(suggestion = null))

        assertTextAbsent(composeTestRule.stringResource(commonR.string.suggestion_assist_title))
    }

    @Test
    fun `Given a suggestion when the row is clicked then onSuggestionClicked is invoked`() {
        var clicked = false
        setContent(
            uiState = SettingsUiState(
                suggestion = SettingsHomeSuggestion(
                    SettingsViewModel.SUGGESTION_ASSISTANT_APP,
                    commonR.string.suggestion_assist_title,
                    commonR.string.suggestion_assist_summary,
                    R.drawable.ic_comment_processing_outline,
                ),
            ),
            onSuggestionClicked = { clicked = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.suggestion_assist_title))

        assertEquals(true, clicked)
    }

    @Test
    fun `Given a suggestion when the close button is clicked then onSuggestionDismissed is invoked`() {
        var dismissed = false
        setContent(
            uiState = SettingsUiState(
                suggestion = SettingsHomeSuggestion(
                    SettingsViewModel.SUGGESTION_ASSISTANT_APP,
                    commonR.string.suggestion_assist_title,
                    commonR.string.suggestion_assist_summary,
                    R.drawable.ic_comment_processing_outline,
                ),
            ),
            onSuggestionDismissed = { dismissed = true },
        )

        clickText(composeTestRule.stringResource(commonR.string.close))

        assertEquals(true, dismissed)
    }
}

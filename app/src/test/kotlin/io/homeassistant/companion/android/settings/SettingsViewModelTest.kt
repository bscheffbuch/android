package io.homeassistant.companion.android.settings

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.common.data.prefs.NightModeTheme
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.settings.assist.DefaultAssistantManager
import io.homeassistant.companion.android.settings.language.LanguagesManager
import io.homeassistant.companion.android.settings.language.LanguagesProvider
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit4Rule
import io.homeassistant.companion.android.themes.NightModeManager
import io.homeassistant.companion.android.util.ChangeLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SettingsViewModelTest {

    @get:Rule(order = 0)
    val consoleLogRule = ConsoleLogRule()

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherJUnit4Rule()

    private val serversFlow = MutableStateFlow<List<Server>>(emptyList())
    private val serverManager: ServerManager = mockk(relaxed = true) {
        every { serversFlow } returns this@SettingsViewModelTest.serversFlow
    }
    private val prefsRepository: PrefsRepository = mockk(relaxed = true)
    private val nightModeManager: NightModeManager = mockk(relaxed = true)
    private val langsManager: LanguagesManager = mockk(relaxed = true)
    private val langsProvider: LanguagesProvider = mockk(relaxed = true)
    private val changeLog: ChangeLog = mockk(relaxed = true)
    private val defaultAssistantManager: DefaultAssistantManager = mockk(relaxed = true)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        serverManager = serverManager,
        prefsRepository = prefsRepository,
        nightModeManager = nightModeManager,
        langsManager = langsManager,
        langsProvider = langsProvider,
        changeLog = changeLog,
        defaultAssistantManager = defaultAssistantManager,
        appContext = context,
    )

    private var ignoredSuggestions = mutableListOf<String>()

    @Before
    fun setUp() {
        coEvery { prefsRepository.getIgnoredSuggestions() } answers { ignoredSuggestions.toList() }
        coEvery { prefsRepository.setIgnoredSuggestions(any()) } answers {
            ignoredSuggestions = firstArg<List<String>>().toMutableList()
        }
        coEvery { defaultAssistantManager.shouldSuggestAssistantSetup() } returns false
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
    }

    @Test
    fun `Given servers flow emits when view model is created then uiState servers reflects it`() = runTest {
        val server = mockk<Server>(relaxed = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        serversFlow.value = listOf(server)
        advanceUntilIdle()

        assertEquals(listOf(server), viewModel.uiState.value.servers)
    }

    @Test
    fun `Given fullscreen disabled when toggled on then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onFullscreenToggled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.fullscreenEnabled)
        coVerify { prefsRepository.setFullScreenEnabled(true) }
    }

    @Test
    fun `Given keep screen on when toggled then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onKeepScreenOnToggled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.keepScreenOnEnabled)
        coVerify { prefsRepository.setKeepScreenOnEnabled(true) }
    }

    @Test
    fun `Given pinch to zoom when toggled then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPinchToZoomToggled(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.pinchToZoomEnabled)
        coVerify { prefsRepository.setPinchToZoomEnabled(false) }
    }

    @Test
    fun `Given autoplay video when toggled then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAutoplayVideoToggled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.autoplayVideoEnabled)
        coVerify { prefsRepository.setAutoPlayVideo(true) }
    }

    @Test
    fun `Given always show first view when toggled then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAlwaysShowFirstViewOnAppStartToggled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.alwaysShowFirstViewOnAppStartEnabled)
        coVerify { prefsRepository.setAlwaysShowFirstViewOnAppStart(true) }
    }

    @Test
    fun `Given crash reporting when toggled then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCrashReportingToggled(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.crashReportingEnabled)
        coVerify { prefsRepository.setCrashReporting(false) }
    }

    @Test
    fun `Given changelog popup when toggled then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onChangeLogPopupEnabledToggled(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.changeLogPopupEnabled)
        coVerify { prefsRepository.setChangeLogPopupEnabled(false) }
    }

    @Test
    fun `Given a screen orientation is selected when persisted then state updates and preference is saved`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onScreenOrientationSelected("landscape")
        advanceUntilIdle()

        assertEquals("landscape", viewModel.uiState.value.screenOrientation)
        coVerify { prefsRepository.saveScreenOrientation("landscape") }
    }

    @Test
    fun `Given a page zoom level is selected when persisted then state updates and preference is saved`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPageZoomLevelSelected(150)
        advanceUntilIdle()

        assertEquals(150, viewModel.uiState.value.pageZoomLevel)
        coVerify { prefsRepository.setPageZoomLevel(150) }
    }

    @Test
    fun `Given a language is selected when persisted then current language tag is refreshed from the manager`() = runTest {
        coEvery { langsManager.getCurrentLang() } returns "fr"
        val viewModel = createViewModel()

        viewModel.onLanguageSelected("fr")
        advanceUntilIdle()

        assertEquals("fr", viewModel.uiState.value.currentLanguageTag)
        coVerify { langsManager.saveLang("fr") }
    }

    @Test
    fun `Given a theme is selected when persisted then state updates and preference is saved`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onThemeSelected(NightModeTheme.DARK)
        advanceUntilIdle()

        assertEquals(NightModeTheme.DARK, viewModel.uiState.value.currentTheme)
        coVerify { nightModeManager.saveNightMode(NightModeTheme.DARK) }
    }

    @Test
    fun `Given show changelog clicked when handled then changelog is force shown`() = runTest {
        val viewModel = createViewModel()

        viewModel.onShowChangeLogClicked(context)
        advanceUntilIdle()

        coVerify { changeLog.showChangeLog(context, forceShow = true) }
    }

    @Test
    fun `Given a default assistant intent is requested then it is delegated to the default assistant manager`() {
        val intent = android.content.Intent()
        every { defaultAssistantManager.getSetDefaultAssistantIntent() } returns intent
        val viewModel = createViewModel()

        assertEquals(intent, viewModel.getSetDefaultAssistantIntent())
    }

    @Test
    fun `Given notifications are enabled when channel state refreshed then permission row is hidden and channels row is visible`() = runTest {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        val viewModel = createViewModel()

        viewModel.refreshNotificationChannelState()

        assertEquals(false, viewModel.uiState.value.notificationPermissionVisible)
        assertEquals(true, viewModel.uiState.value.notificationChannelsVisible)
    }

    @Test
    fun `Given notifications are disabled when channel state refreshed then permission row is visible and channels row is hidden`() = runTest {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        val viewModel = createViewModel()

        viewModel.refreshNotificationChannelState()

        assertEquals(true, viewModel.uiState.value.notificationPermissionVisible)
        assertEquals(false, viewModel.uiState.value.notificationChannelsVisible)
    }

    @Test
    fun `Given assistant setup should be suggested when resumed then suggestion banner is shown`() = runTest {
        coEvery { defaultAssistantManager.shouldSuggestAssistantSetup() } returns true
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refreshOnResume()
        advanceUntilIdle()

        assertEquals(SettingsViewModel.SUGGESTION_ASSISTANT_APP, viewModel.uiState.value.suggestion?.id)
    }

    @Test
    fun `Given no suggestion is applicable when resumed then suggestion banner stays hidden`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refreshOnResume()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.suggestion)
    }

    @Test
    fun `Given a suggestion is shown when dismissed then it is cleared and recorded as ignored`() = runTest {
        coEvery { defaultAssistantManager.shouldSuggestAssistantSetup() } returns true
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.refreshOnResume()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.suggestion != null)

        viewModel.onSuggestionDismissed()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.suggestion)
        coVerify { prefsRepository.setIgnoredSuggestions(listOf(SettingsViewModel.SUGGESTION_ASSISTANT_APP)) }
    }

    @Test
    fun `Given dismissed with no suggestion currently shown when dismissed then nothing is recorded as ignored`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSuggestionDismissed()
        advanceUntilIdle()

        coVerify(exactly = 0) { prefsRepository.setIgnoredSuggestions(any()) }
    }

    @Test
    fun `Given an add-server flow succeeds when recorded then state exposes the result`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAddServerResult(success = true, serverId = 42)

        assertEquals(AddServerResult(success = true, serverId = 42), viewModel.uiState.value.addServerResult)
    }

    @Test
    fun `Given an add-server result is shown when acknowledged then it is cleared from state`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAddServerResult(success = true, serverId = 42)

        viewModel.onAddServerResultAcknowledged()

        assertNull(viewModel.uiState.value.addServerResult)
    }

    @Test
    fun `Given voice command intent when toggled on then state updates and component is enabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAssistVoiceCommandIntentToggled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.assistVoiceCommandIntentEnabled)
    }

    @Test
    fun `Given HA launcher when toggled on then state updates and component is enabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEnableHaLauncherToggled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.enableHaLauncherEnabled)
    }
}

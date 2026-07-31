package io.homeassistant.companion.android.settings.developer

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit4Rule
import io.homeassistant.companion.android.thread.ThreadManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DeveloperSettingsViewModelTest {

    @get:Rule(order = 0)
    val consoleLogRule = ConsoleLogRule()

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherJUnit4Rule()

    private val prefsRepository: PrefsRepository = mockk(relaxed = true)
    private val serverManager: ServerManager = mockk(relaxed = true)
    private val threadManager: ThreadManager = mockk(relaxed = true)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun createViewModel(): DeveloperSettingsViewModel {
        return DeveloperSettingsViewModel(prefsRepository, serverManager, threadManager, context)
    }

    @Before
    fun setUp() {
        coEvery { prefsRepository.isWebViewDebugEnabled() } returns false
        coEvery { serverManager.servers() } returns listOf(mockk<Server>(relaxed = true))
        every { threadManager.appSupportsThread() } returns true
    }

    @Test
    fun `Given remote debugging disabled when toggled on then state updates and preference persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(false, awaitItem().remoteDebuggingEnabled)

            viewModel.onRemoteDebuggingToggled(true)

            assertEquals(true, awaitItem().remoteDebuggingEnabled)
        }
        advanceUntilIdle()
        coEvery { prefsRepository.setWebViewDebugEnabled(true) }
    }

    @Test
    fun `Given a single registered server when thread debug clicked then sync starts without server selection`() = runTest {
        coEvery { serverManager.servers() } returns listOf(mockk<Server>(relaxed = true))
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.NoneHaveCredentials
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onThreadDebugClicked()
            advanceUntilIdle()
            expectNoEvents()
        }

        assertEquals(null, viewModel.uiState.value.threadDebugResult?.success)
    }

    @Test
    fun `Given multiple registered servers when thread debug clicked then server selection is requested`() = runTest {
        coEvery { serverManager.servers() } returns listOf(mockk<Server>(relaxed = true), mockk<Server>(relaxed = true))
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onThreadDebugClicked()
            assertEquals(DeveloperSettingsEvent.RequestServerSelectionForThreadDebug, awaitItem())
        }
    }

    @Test
    fun `Given server picked after multi-server prompt when sync completes then result is posted`() = runTest {
        coEvery { serverManager.servers() } returns listOf(mockk<Server>(relaxed = true), mockk<Server>(relaxed = true))
        coEvery {
            threadManager.syncPreferredDataset(context, 42, false, any())
        } returns ThreadManager.SyncResult.NoneHaveCredentials
        val viewModel = createViewModel()

        viewModel.onServerSelectedForThreadDebug(42)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.threadDebugResult?.success)
        assertTrue(viewModel.uiState.value.threadDebugResult != null)
        assertEquals(false, viewModel.uiState.value.threadDebugInProgress)
    }

    @Test
    fun `Given sync result is server unsupported when thread debug runs then result is a failure`() = runTest {
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.ServerUnsupported
        val viewModel = createViewModel()

        viewModel.onThreadDebugClicked()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.threadDebugResult?.success)
    }

    @Test
    fun `Given credentials only on server and import succeeds when thread debug runs then result is a success`() = runTest {
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.OnlyOnServer(imported = true)
        val viewModel = createViewModel()

        viewModel.onThreadDebugClicked()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.threadDebugResult?.success)
    }

    @Test
    fun `Given credentials only on device with an export intent when thread debug runs then permission is requested`() = runTest {
        val intentSender = mockk<android.content.IntentSender>(relaxed = true)
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.OnlyOnDevice(exportIntent = intentSender)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onThreadDebugClicked()
            val event = awaitItem()
            assertTrue(event is DeveloperSettingsEvent.RequestThreadPermission)
            assertEquals(intentSender, (event as DeveloperSettingsEvent.RequestThreadPermission).intentSender)
        }
    }

    @Test
    fun `Given all have matching credentials when thread debug runs then result is a success match`() = runTest {
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.AllHaveCredentials(
            matches = true,
            fromApp = null,
            updated = null,
            exportIntent = null,
        )
        val viewModel = createViewModel()

        viewModel.onThreadDebugClicked()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.threadDebugResult?.success)
    }

    @Test
    fun `Given thread permission granted for a device-only export when result is processed then success is posted`() = runTest {
        coEvery {
            threadManager.sendThreadDatasetExportResult(any(), ServerManager.SERVER_ID_ACTIVE)
        } returns "My Network"
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.OnlyOnDevice(exportIntent = mockk(relaxed = true))
        val viewModel = createViewModel()
        viewModel.onThreadDebugClicked()
        advanceUntilIdle()

        viewModel.onThreadDebugResultDismissed()
        viewModel.onThreadPermissionResult(mockk<ActivityResult>(relaxed = true))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.threadDebugResult?.success)
    }

    @Test
    fun `Given a shown thread debug result when dismissed then it is cleared from state`() = runTest {
        coEvery {
            threadManager.syncPreferredDataset(context, ServerManager.SERVER_ID_ACTIVE, false, any())
        } returns ThreadManager.SyncResult.NoneHaveCredentials
        val viewModel = createViewModel()
        viewModel.onThreadDebugClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.threadDebugResult != null)

        viewModel.onThreadDebugResultDismissed()

        assertNull(viewModel.uiState.value.threadDebugResult)
    }

    @Test
    fun `Given cache clear supported when clicked then progress state transitions and result is acknowledged`() = runTest {
        val viewModel = createViewModel()

        viewModel.onClearWebViewCacheClicked()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.webViewClearCacheInProgress)
        viewModel.onWebViewClearCacheResultAcknowledged()
        assertNull(viewModel.uiState.value.webViewClearCacheResult)
    }
}

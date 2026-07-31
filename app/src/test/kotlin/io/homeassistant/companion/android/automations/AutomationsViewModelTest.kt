package io.homeassistant.companion.android.automations

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit4Rule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AutomationsViewModelTest {

    @get:Rule(order = 0)
    val consoleLogRule = ConsoleLogRule()

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherJUnit4Rule()

    private val serverManager: ServerManager = mockk(relaxed = true)
    private val integrationRepository: IntegrationRepository = mockk(relaxed = true)
    private val server: Server = mockk(relaxed = true)

    private lateinit var viewModel: AutomationsViewModel

    private fun entity(entityId: String, state: String) = Entity(
        entityId = entityId,
        state = state,
        attributes = emptyMap(),
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    private val sceneSaveError = "Couldn't save the scene. Your server may not allow editing scenes here."

    @Before
    fun setUp() {
        coEvery { server.id } returns 1
        coEvery { serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) } returns server
        coEvery { serverManager.integrationRepository(any()) } returns integrationRepository

        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = AutomationsViewModel(serverManager, Clock.System, context)
    }

    private suspend fun TestScope.givenEntities(vararg entities: Entity) {
        coEvery { integrationRepository.getEntities() } returns entities.toList()
        viewModel.loadEntities()
        advanceUntilIdle()
    }

    @Test
    fun `Given entities of multiple domains when loadEntities is called then only automation and scene entities are kept`() = runTest {
        givenEntities(
            entity("automation.morning_routine", state = "on"),
            entity("scene.movie_night", state = "2026-07-05T00:00:00Z"),
            entity("light.living_room", state = "on"),
        )

        val state = viewModel.uiState.value
        assertEquals(AutomationsUiState.Success::class, state::class)
        assertEquals(
            listOf("automation.morning_routine"),
            (state as AutomationsUiState.Success).automations.map { it.entityId },
        )
        assertEquals(
            listOf("scene.movie_night"),
            state.scenes.map { it.entityId },
        )
    }

    @Test
    fun `Given a disabled automation entity when toggle is called then callAction turns it on`() = runTest {
        givenEntities(entity("automation.morning_routine", state = "off"))

        viewModel.toggle("automation.morning_routine")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "automation",
                action = "turn_on",
                actionData = mapOf("entity_id" to "automation.morning_routine"),
            )
        }
    }

    @Test
    fun `Given an enabled automation entity when toggle is called then callAction turns it off`() = runTest {
        givenEntities(entity("automation.morning_routine", state = "on"))

        viewModel.toggle("automation.morning_routine")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "automation",
                action = "turn_off",
                actionData = mapOf("entity_id" to "automation.morning_routine"),
            )
        }
    }

    @Test
    fun `Given an automation entity when triggerNow is called then callAction triggers it while skipping conditions`() = runTest {
        givenEntities(entity("automation.morning_routine", state = "on"))

        viewModel.triggerNow("automation.morning_routine")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "automation",
                action = "trigger",
                actionData = mapOf("entity_id" to "automation.morning_routine", "skip_conditions" to true),
            )
        }
    }

    @Test
    fun `Given callAction throws when toggle is called then errorEvents emits a failure message`() = runTest {
        givenEntities(entity("automation.morning_routine", state = "on"))
        coEvery { integrationRepository.callAction(domain = "automation", action = "turn_off", actionData = any()) } throws
            RuntimeException("boom")

        viewModel.errorEvents.test {
            viewModel.toggle("automation.morning_routine")
            advanceUntilIdle()

            assertEquals("Action failed. Please try again.", awaitItem())
        }
    }

    @Test
    fun `Given callAction throws when triggerNow is called then errorEvents emits a failure message`() = runTest {
        givenEntities(entity("automation.morning_routine", state = "on"))
        coEvery { integrationRepository.callAction(domain = "automation", action = "trigger", actionData = any()) } throws
            RuntimeException("boom")

        viewModel.errorEvents.test {
            viewModel.triggerNow("automation.morning_routine")
            advanceUntilIdle()

            assertEquals("Action failed. Please try again.", awaitItem())
        }
    }

    @Test
    fun `Given a scene entity when activateScene is called then callAction turns it on`() = runTest {
        givenEntities(entity("scene.movie_night", state = "2026-07-05T00:00:00Z"))

        viewModel.activateScene("scene.movie_night")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "scene",
                action = "turn_on",
                actionData = mapOf("entity_id" to "scene.movie_night"),
            )
        }
    }

    @Test
    fun `Given callAction throws when activateScene is called then errorEvents emits a failure message`() = runTest {
        givenEntities(entity("scene.movie_night", state = "2026-07-05T00:00:00Z"))
        coEvery { integrationRepository.callAction(domain = "scene", action = "turn_on", actionData = any()) } throws
            RuntimeException("boom")

        viewModel.errorEvents.test {
            viewModel.activateScene("scene.movie_night")
            advanceUntilIdle()

            assertEquals("Action failed. Please try again.", awaitItem())
        }
    }

    @Test
    fun `Given entities of mixed domains when onCreateSceneClicked then only controllable candidates are offered`() = runTest {
        coEvery { integrationRepository.getEntities() } returns listOf(
            entity("light.kitchen", state = "on"),
            entity("switch.fan", state = "off"),
            entity("sensor.temperature", state = "21"),
            entity("automation.morning_routine", state = "on"),
        )

        viewModel.onCreateSceneClicked()
        advanceUntilIdle()

        val state = viewModel.saveSceneDialogState.value
        assertEquals(SaveSceneDialogUiState.Ready::class, state::class)
        assertEquals(
            setOf("light.kitchen", "switch.fan"),
            (state as SaveSceneDialogUiState.Ready).candidates.map { it.entityId }.toSet(),
        )
    }

    @Test
    fun `Given a selected entity when saveScene succeeds then only that entity is saved and the dialog closes`() = runTest {
        coEvery { integrationRepository.getEntities() } returns listOf(
            entity("light.kitchen", state = "on"),
            entity("switch.fan", state = "on"),
        )
        coEvery { integrationRepository.saveScene(any(), any(), any()) } returns true
        viewModel.onCreateSceneClicked()
        advanceUntilIdle()

        viewModel.saveScene(name = "Movie", selectedEntityIds = setOf("light.kitchen"))
        advanceUntilIdle()

        coVerify {
            integrationRepository.saveScene(
                sceneId = any(),
                name = "Movie",
                entities = match { it.keys == setOf("light.kitchen") },
            )
        }
        assertEquals(SaveSceneDialogUiState.Hidden, viewModel.saveSceneDialogState.value)
    }

    @Test
    fun `Given the server rejects the scene when saveScene is called then an error is emitted and the dialog stays open`() = runTest {
        coEvery { integrationRepository.getEntities() } returns listOf(entity("light.kitchen", state = "on"))
        coEvery { integrationRepository.saveScene(any(), any(), any()) } returns false
        viewModel.onCreateSceneClicked()
        advanceUntilIdle()

        viewModel.errorEvents.test {
            viewModel.saveScene(name = "Movie", selectedEntityIds = setOf("light.kitchen"))
            advanceUntilIdle()

            assertEquals(sceneSaveError, awaitItem())
        }

        val state = viewModel.saveSceneDialogState.value
        assertEquals(SaveSceneDialogUiState.Ready::class, state::class)
        assertEquals(false, (state as SaveSceneDialogUiState.Ready).isSaving)
    }

    @Test
    fun `Given the saved scene is missing from the first refetch when saveScene succeeds then polling surfaces it`() = runTest {
        coEvery { integrationRepository.getEntities() } returns listOf(entity("light.kitchen", state = "on"))
        coEvery { integrationRepository.saveScene(any(), any(), any()) } returns true
        viewModel.onCreateSceneClicked()
        advanceUntilIdle()

        // Home Assistant hasn't finished reloading its scene platform on the first refetch, so the
        // new scene only appears on the second — the poll must keep trying until it does.
        coEvery { integrationRepository.getEntities() } returnsMany listOf(
            listOf(entity("light.kitchen", state = "on")),
            listOf(entity("light.kitchen", state = "on"), entity("scene.movie", state = "on")),
        )

        viewModel.saveScene(name = "Movie", selectedEntityIds = setOf("light.kitchen"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AutomationsUiState.Success::class, state::class)
        assertEquals(
            listOf("scene.movie"),
            (state as AutomationsUiState.Success).scenes.map { it.entityId },
        )
    }
}

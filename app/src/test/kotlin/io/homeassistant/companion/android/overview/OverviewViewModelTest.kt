package io.homeassistant.companion.android.overview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class OverviewViewModelTest {

    @get:Rule(order = 0)
    val consoleLogRule = ConsoleLogRule()

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherJUnit4Rule()

    private val serverManager: ServerManager = mockk(relaxed = true)
    private val integrationRepository: IntegrationRepository = mockk(relaxed = true)
    private val server: Server = mockk(relaxed = true)

    private lateinit var viewModel: OverviewViewModel

    private fun entityOf(entityId: String, state: String) = Entity(
        entityId = entityId,
        state = state,
        attributes = emptyMap(),
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    private fun vacuumEntityOf(entityId: String, state: String, supportsTurnOn: Boolean) = Entity(
        entityId = entityId,
        state = state,
        attributes = mapOf("supported_features" to (if (supportsTurnOn) 1 else 0)),
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    @Before
    fun setUp() {
        coEvery { server.id } returns 1
        coEvery { serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) } returns server
        coEvery { serverManager.integrationRepository(any()) } returns integrationRepository

        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = OverviewViewModel(serverManager, context)
    }

    private suspend fun TestScope.givenEntities(vararg entities: Entity) {
        coEvery { integrationRepository.getEntities() } returns entities.toList()
        viewModel.loadEntities()
        advanceUntilIdle()
    }

    @Test
    fun `Given an automation entity when triggerAutomation is called then callAction triggers it while skipping conditions`() = runTest {
        givenEntities(entityOf("automation.morning_routine", state = "on"))

        viewModel.triggerAutomation("automation.morning_routine")
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
    fun `Given a disabled automation entity when toggleEntity is called then callAction turns it on`() = runTest {
        givenEntities(entityOf("automation.morning_routine", state = "off"))

        viewModel.toggleEntity("automation.morning_routine")
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
    fun `Given an enabled automation entity when toggleEntity is called then callAction turns it off`() = runTest {
        givenEntities(entityOf("automation.morning_routine", state = "on"))

        viewModel.toggleEntity("automation.morning_routine")
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
    fun `Given no entities loaded when triggerAutomation is called then no exception is thrown`() = runTest {
        viewModel.triggerAutomation("automation.morning_routine")
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
    fun `Given an unknown entity when toggleEntity is called then no action is sent`() = runTest {
        givenEntities(entityOf("automation.morning_routine", state = "on"))

        viewModel.toggleEntity("automation.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a locked lock entity when toggleEntity is called then callAction unlocks it`() = runTest {
        givenEntities(entityOf("lock.front_door", state = "locked"))

        viewModel.toggleEntity("lock.front_door")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "lock",
                action = "unlock",
                actionData = mapOf("entity_id" to "lock.front_door"),
            )
        }
    }

    @Test
    fun `Given an unlocked lock entity when toggleEntity is called then callAction locks it`() = runTest {
        givenEntities(entityOf("lock.front_door", state = "unlocked"))

        viewModel.toggleEntity("lock.front_door")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "lock",
                action = "lock",
                actionData = mapOf("entity_id" to "lock.front_door"),
            )
        }
    }

    @Test
    fun `Given an armed alarm_control_panel entity when toggleEntity is called then callAction disarms it`() = runTest {
        givenEntities(entityOf("alarm_control_panel.home", state = "armed_away"))

        viewModel.toggleEntity("alarm_control_panel.home")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "alarm_control_panel",
                action = "alarm_disarm",
                actionData = mapOf("entity_id" to "alarm_control_panel.home"),
            )
        }
    }

    @Test
    fun `Given a disarmed alarm_control_panel entity when toggleEntity is called then callAction arms it away`() = runTest {
        givenEntities(entityOf("alarm_control_panel.home", state = "disarmed"))

        viewModel.toggleEntity("alarm_control_panel.home")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "alarm_control_panel",
                action = "alarm_arm_away",
                actionData = mapOf("entity_id" to "alarm_control_panel.home"),
            )
        }
    }

    @Test
    fun `Given an open cover entity when toggleEntity is called then callAction closes it`() = runTest {
        givenEntities(entityOf("cover.garage_door", state = "open"))

        viewModel.toggleEntity("cover.garage_door")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "cover",
                action = "close_cover",
                actionData = mapOf("entity_id" to "cover.garage_door"),
            )
        }
    }

    @Test
    fun `Given a closed cover entity when toggleEntity is called then callAction opens it`() = runTest {
        givenEntities(entityOf("cover.garage_door", state = "closed"))

        viewModel.toggleEntity("cover.garage_door")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "cover",
                action = "open_cover",
                actionData = mapOf("entity_id" to "cover.garage_door"),
            )
        }
    }

    @Test
    fun `Given a button entity when toggleEntity is called then callAction presses it`() = runTest {
        givenEntities(entityOf("button.doorbell", state = "unavailable"))

        viewModel.toggleEntity("button.doorbell")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "button",
                action = "press",
                actionData = mapOf("entity_id" to "button.doorbell"),
            )
        }
    }

    @Test
    fun `Given an input_button entity when toggleEntity is called then callAction presses it`() = runTest {
        givenEntities(entityOf("input_button.reset", state = "unavailable"))

        viewModel.toggleEntity("input_button.reset")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "input_button",
                action = "press",
                actionData = mapOf("entity_id" to "input_button.reset"),
            )
        }
    }

    @Test
    fun `Given a cleaning vacuum without turn_on support when toggleEntity is called then callAction returns it to base`() = runTest {
        givenEntities(vacuumEntityOf("vacuum.roomba", state = "cleaning", supportsTurnOn = false))

        viewModel.toggleEntity("vacuum.roomba")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "vacuum",
                action = "return_to_base",
                actionData = mapOf("entity_id" to "vacuum.roomba"),
            )
        }
    }

    @Test
    fun `Given a docked vacuum without turn_on support when toggleEntity is called then callAction starts it`() = runTest {
        givenEntities(vacuumEntityOf("vacuum.roomba", state = "docked", supportsTurnOn = false))

        viewModel.toggleEntity("vacuum.roomba")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "vacuum",
                action = "start",
                actionData = mapOf("entity_id" to "vacuum.roomba"),
            )
        }
    }

    @Test
    fun `Given a cleaning vacuum with turn_on support when toggleEntity is called then callAction turns it off`() = runTest {
        givenEntities(vacuumEntityOf("vacuum.roomba", state = "cleaning", supportsTurnOn = true))

        viewModel.toggleEntity("vacuum.roomba")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "vacuum",
                action = "turn_off",
                actionData = mapOf("entity_id" to "vacuum.roomba"),
            )
        }
    }

    @Test
    fun `Given a docked vacuum with turn_on support when toggleEntity is called then callAction turns it on`() = runTest {
        givenEntities(vacuumEntityOf("vacuum.roomba", state = "docked", supportsTurnOn = true))

        viewModel.toggleEntity("vacuum.roomba")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "vacuum",
                action = "turn_on",
                actionData = mapOf("entity_id" to "vacuum.roomba"),
            )
        }
    }

    @Test
    fun `Given a fan entity when setFanSpeed is called then callAction sets its percentage`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.setFanSpeed("fan.living_room", 42f, immediate = true)
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "fan",
                action = "set_percentage",
                actionData = mapOf("entity_id" to "fan.living_room", "percentage" to 42),
            )
        }
    }

    @Test
    fun `Given an unknown entity when setFanSpeed is called then no action is sent`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.setFanSpeed("fan.unknown", 42f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a cover entity when setCoverPosition is called then callAction sets its position`() = runTest {
        givenEntities(entityOf("cover.garage_door", state = "open"))

        viewModel.setCoverPosition("cover.garage_door", 55f, immediate = true)
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "cover",
                action = "set_cover_position",
                actionData = mapOf("entity_id" to "cover.garage_door", "position" to 55),
            )
        }
    }

    @Test
    fun `Given an unknown entity when setCoverPosition is called then no action is sent`() = runTest {
        givenEntities(entityOf("cover.garage_door", state = "open"))

        viewModel.setCoverPosition("cover.unknown", 55f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a cover entity when stopCover is called then callAction stops it`() = runTest {
        givenEntities(entityOf("cover.garage_door", state = "open"))

        viewModel.stopCover("cover.garage_door")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "cover",
                action = "stop_cover",
                actionData = mapOf("entity_id" to "cover.garage_door"),
            )
        }
    }

    @Test
    fun `Given an unknown entity when stopCover is called then no exception is thrown`() = runTest {
        givenEntities(entityOf("cover.garage_door", state = "open"))

        viewModel.stopCover("cover.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    private fun climateEntityOf(entityId: String, state: String, hvacModes: List<String> = listOf("off", "heat", "cool")) = Entity(
        entityId = entityId,
        state = state,
        attributes = mapOf(
            "supported_features" to 1,
            "min_temp" to 7.0,
            "max_temp" to 35.0,
            "temperature" to 21.0,
            "hvac_modes" to hvacModes,
        ),
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    @Test
    fun `Given a climate entity when setClimateTemperature is called then callAction sets its temperature`() = runTest {
        givenEntities(climateEntityOf("climate.living_room", state = "heat"))

        viewModel.setClimateTemperature("climate.living_room", 22.5f, immediate = true)
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "climate",
                action = "set_temperature",
                actionData = mapOf("entity_id" to "climate.living_room", "temperature" to 22.5f),
            )
        }
    }

    @Test
    fun `Given an unknown entity when setClimateTemperature is called then no action is sent`() = runTest {
        givenEntities(climateEntityOf("climate.living_room", state = "heat"))

        viewModel.setClimateTemperature("climate.unknown", 22.5f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a non-climate entity when setClimateTemperature is called then no action is sent`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.setClimateTemperature("fan.living_room", 22.5f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a climate entity in heat mode when cycleClimateHvacMode is called then callAction sets the next mode`() = runTest {
        givenEntities(climateEntityOf("climate.living_room", state = "heat"))

        viewModel.cycleClimateHvacMode("climate.living_room")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "climate",
                action = "set_hvac_mode",
                actionData = mapOf("entity_id" to "climate.living_room", "hvac_mode" to "cool"),
            )
        }
    }

    @Test
    fun `Given a climate entity in the last hvac mode when cycleClimateHvacMode is called then callAction wraps to the first mode`() = runTest {
        givenEntities(climateEntityOf("climate.living_room", state = "cool"))

        viewModel.cycleClimateHvacMode("climate.living_room")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "climate",
                action = "set_hvac_mode",
                actionData = mapOf("entity_id" to "climate.living_room", "hvac_mode" to "off"),
            )
        }
    }

    @Test
    fun `Given an unknown entity when cycleClimateHvacMode is called then no exception is thrown`() = runTest {
        givenEntities(climateEntityOf("climate.living_room", state = "heat"))

        viewModel.cycleClimateHvacMode("climate.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a climate entity with no hvac modes when cycleClimateHvacMode is called then no action is sent`() = runTest {
        givenEntities(climateEntityOf("climate.living_room", state = "heat", hvacModes = emptyList()))

        viewModel.cycleClimateHvacMode("climate.living_room")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a media player entity when toggleMediaPlayback is called then callAction toggles playback`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "paused"))

        viewModel.toggleMediaPlayback("media_player.living_room")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "media_player",
                action = "media_play_pause",
                actionData = mapOf("entity_id" to "media_player.living_room"),
            )
        }
    }

    @Test
    fun `Given an unknown entity when toggleMediaPlayback is called then no exception is thrown`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "paused"))

        viewModel.toggleMediaPlayback("media_player.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a non-media player entity when toggleMediaPlayback is called then no action is sent`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.toggleMediaPlayback("fan.living_room")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a media player entity when skipToPreviousTrack is called then callAction skips to the previous track`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "playing"))

        viewModel.skipToPreviousTrack("media_player.living_room")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "media_player",
                action = "media_previous_track",
                actionData = mapOf("entity_id" to "media_player.living_room"),
            )
        }
    }

    @Test
    fun `Given an unknown entity when skipToPreviousTrack is called then no exception is thrown`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "playing"))

        viewModel.skipToPreviousTrack("media_player.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a media player entity when skipToNextTrack is called then callAction skips to the next track`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "playing"))

        viewModel.skipToNextTrack("media_player.living_room")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "media_player",
                action = "media_next_track",
                actionData = mapOf("entity_id" to "media_player.living_room"),
            )
        }
    }

    @Test
    fun `Given an unknown entity when skipToNextTrack is called then no exception is thrown`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "playing"))

        viewModel.skipToNextTrack("media_player.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    private fun humidifierEntityOf(entityId: String, state: String) = Entity(
        entityId = entityId,
        state = state,
        attributes = mapOf(
            "humidity" to 45,
            "min_humidity" to 0,
            "max_humidity" to 100,
        ),
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    @Test
    fun `Given a humidifier entity when setHumidifierHumidity is called then callAction sets its humidity`() = runTest {
        givenEntities(humidifierEntityOf("humidifier.bedroom", state = "on"))

        viewModel.setHumidifierHumidity("humidifier.bedroom", 55f, immediate = true)
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "humidifier",
                action = "set_humidity",
                actionData = mapOf("entity_id" to "humidifier.bedroom", "humidity" to 55),
            )
        }
    }

    @Test
    fun `Given an unknown entity when setHumidifierHumidity is called then no action is sent`() = runTest {
        givenEntities(humidifierEntityOf("humidifier.bedroom", state = "on"))

        viewModel.setHumidifierHumidity("humidifier.unknown", 55f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a non-humidifier entity when setHumidifierHumidity is called then no action is sent`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.setHumidifierHumidity("fan.living_room", 55f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    private fun mediaPlayerEntityOf(entityId: String, state: String) = Entity(
        entityId = entityId,
        state = state,
        attributes = mapOf("supported_features" to 4, "volume_level" to 0.5),
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    @Test
    fun `Given a media player entity when setMediaVolume is called then callAction sets its volume`() = runTest {
        givenEntities(mediaPlayerEntityOf("media_player.living_room", state = "playing"))

        viewModel.setMediaVolume("media_player.living_room", 75f, immediate = true)
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "media_player",
                action = "volume_set",
                actionData = mapOf("entity_id" to "media_player.living_room", "volume_level" to 0.75f),
            )
        }
    }

    @Test
    fun `Given an unknown entity when setMediaVolume is called then no action is sent`() = runTest {
        givenEntities(mediaPlayerEntityOf("media_player.living_room", state = "playing"))

        viewModel.setMediaVolume("media_player.unknown", 75f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a non-media player entity when setMediaVolume is called then no action is sent`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.setMediaVolume("fan.living_room", 75f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a media player entity without volume support when setMediaVolume is called then no action is sent`() = runTest {
        givenEntities(entityOf("media_player.living_room", state = "playing"))

        viewModel.setMediaVolume("media_player.living_room", 75f, immediate = true)
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    private fun humidifierWithModesEntityOf(entityId: String, state: String, mode: String?) = Entity(
        entityId = entityId,
        state = state,
        attributes = buildMap {
            put("supported_features", 8)
            put("available_modes", listOf("auto", "away", "boost"))
            if (mode != null) put("mode", mode)
        },
        lastChanged = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
    )

    @Test
    fun `Given a humidifier entity when setHumidifierMode is called then callAction sets its mode`() = runTest {
        givenEntities(humidifierWithModesEntityOf("humidifier.bedroom", state = "on", mode = "auto"))

        viewModel.setHumidifierMode("humidifier.bedroom", "boost")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "humidifier",
                action = "set_mode",
                actionData = mapOf("entity_id" to "humidifier.bedroom", "mode" to "boost"),
            )
        }
    }

    @Test
    fun `Given an unknown entity when setHumidifierMode is called then no action is sent`() = runTest {
        givenEntities(humidifierWithModesEntityOf("humidifier.bedroom", state = "on", mode = "auto"))

        viewModel.setHumidifierMode("humidifier.unknown", "boost")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a non-humidifier entity when setHumidifierMode is called then no action is sent`() = runTest {
        givenEntities(entityOf("fan.living_room", state = "on"))

        viewModel.setHumidifierMode("fan.living_room", "boost")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given a humidifier on a mode when cycleHumidifierMode is called then callAction sets the next mode`() = runTest {
        givenEntities(humidifierWithModesEntityOf("humidifier.bedroom", state = "on", mode = "auto"))

        viewModel.cycleHumidifierMode("humidifier.bedroom")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "humidifier",
                action = "set_mode",
                actionData = mapOf("entity_id" to "humidifier.bedroom", "mode" to "away"),
            )
        }
    }

    @Test
    fun `Given a humidifier on the last mode when cycleHumidifierMode is called then callAction wraps to the first mode`() = runTest {
        givenEntities(humidifierWithModesEntityOf("humidifier.bedroom", state = "on", mode = "boost"))

        viewModel.cycleHumidifierMode("humidifier.bedroom")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "humidifier",
                action = "set_mode",
                actionData = mapOf("entity_id" to "humidifier.bedroom", "mode" to "auto"),
            )
        }
    }

    @Test
    fun `Given a humidifier without a current mode when cycleHumidifierMode is called then callAction sets the first mode`() = runTest {
        givenEntities(humidifierWithModesEntityOf("humidifier.bedroom", state = "on", mode = null))

        viewModel.cycleHumidifierMode("humidifier.bedroom")
        advanceUntilIdle()

        coVerify {
            integrationRepository.callAction(
                domain = "humidifier",
                action = "set_mode",
                actionData = mapOf("entity_id" to "humidifier.bedroom", "mode" to "auto"),
            )
        }
    }

    @Test
    fun `Given a humidifier without modes support when cycleHumidifierMode is called then no action is sent`() = runTest {
        givenEntities(humidifierEntityOf("humidifier.bedroom", state = "on"))

        viewModel.cycleHumidifierMode("humidifier.bedroom")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun `Given an unknown entity when cycleHumidifierMode is called then no action is sent`() = runTest {
        givenEntities(humidifierWithModesEntityOf("humidifier.bedroom", state = "on", mode = "auto"))

        viewModel.cycleHumidifierMode("humidifier.unknown")
        advanceUntilIdle()

        assertEquals(OverviewUiState.Success::class, viewModel.uiState.value::class)
    }
}

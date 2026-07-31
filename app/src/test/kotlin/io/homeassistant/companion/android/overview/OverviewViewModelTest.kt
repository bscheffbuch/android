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
        // Robolectric keeps SharedPreferences for the lifetime of the JVM, so light groups / item
        // order saved by one test would leak into the next. Clear them so each test starts clean.
        context.getSharedPreferences("overview_preferences", Context.MODE_PRIVATE).edit().clear().commit()
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

    private fun currentItemOrder(): List<String> = (viewModel.uiState.value as OverviewUiState.Success).itemOrder

    @Test
    fun `Given three items when moveItem drags the first onto the last then it is re-inserted after the last`() = runTest {
        givenEntities(
            entityOf("light.a", state = "on"),
            entityOf("light.b", state = "on"),
            entityOf("light.c", state = "on"),
        )
        val order = currentItemOrder()
        assertEquals(3, order.size)

        viewModel.moveItem(fromKey = order[0], toKey = order[2])
        advanceUntilIdle()

        assertEquals(listOf(order[1], order[2], order[0]), currentItemOrder())
    }

    @Test
    fun `Given three items when moveItem drags the last onto the first then it is re-inserted before the first`() = runTest {
        givenEntities(
            entityOf("light.a", state = "on"),
            entityOf("light.b", state = "on"),
            entityOf("light.c", state = "on"),
        )
        val order = currentItemOrder()
        assertEquals(3, order.size)

        viewModel.moveItem(fromKey = order[2], toKey = order[0])
        advanceUntilIdle()

        assertEquals(listOf(order[2], order[0], order[1]), currentItemOrder())
    }

    @Test
    fun `Given three items when moveItem drags an item onto itself then the order is unchanged`() = runTest {
        givenEntities(
            entityOf("light.a", state = "on"),
            entityOf("light.b", state = "on"),
            entityOf("light.c", state = "on"),
        )
        val order = currentItemOrder()

        viewModel.moveItem(fromKey = order[1], toKey = order[1])
        advanceUntilIdle()

        assertEquals(order, currentItemOrder())
    }

    // Each group test uses its own light ids so a group persisted by another test is sanitized away
    // when the current test loads a different entity set (its members are no longer present).
    private fun groupContaining(entityId: String): OverviewLightGroup = (viewModel.uiState.value as OverviewUiState.Success).lightGroups.first { entityId in it.entityIds }

    private suspend fun TestScope.givenGroupOf(vararg lightIds: String): OverviewLightGroup {
        givenEntities(*lightIds.map { entityOf(it, state = "on") }.toTypedArray())
        viewModel.saveLightGroup(groupId = null, name = "Group", entityIds = lightIds.toList(), colorArgb = null)
        advanceUntilIdle()
        return groupContaining(lightIds.first())
    }

    @Test
    fun `Given a group of three members when moveGroupMember drags the first onto the last then it is re-inserted after the last`() = runTest {
        val group = givenGroupOf("light.ga1", "light.gb1", "light.gc1")
        val members = group.entityIds
        assertEquals(3, members.size)

        viewModel.moveGroupMember(group.id, fromEntityId = members[0], toEntityId = members[2])
        advanceUntilIdle()

        assertEquals(listOf(members[1], members[2], members[0]), groupContaining(members[0]).entityIds)
    }

    @Test
    fun `Given a group of three members when moveGroupMember drags the last onto the first then it is re-inserted before the first`() = runTest {
        val group = givenGroupOf("light.ga2", "light.gb2", "light.gc2")
        val members = group.entityIds
        assertEquals(3, members.size)

        viewModel.moveGroupMember(group.id, fromEntityId = members[2], toEntityId = members[0])
        advanceUntilIdle()

        assertEquals(listOf(members[2], members[0], members[1]), groupContaining(members[0]).entityIds)
    }

    @Test
    fun `Given a group when moveGroupMember drags a member onto itself then the member order is unchanged`() = runTest {
        val group = givenGroupOf("light.ga3", "light.gb3", "light.gc3")
        val members = group.entityIds

        viewModel.moveGroupMember(group.id, fromEntityId = members[1], toEntityId = members[1])
        advanceUntilIdle()

        assertEquals(members, groupContaining(members[0]).entityIds)
    }

    @Test
    fun `Given an unknown group when moveGroupMember is called then no exception is thrown`() = runTest {
        val group = givenGroupOf("light.ga4", "light.gb4", "light.gc4")
        val members = group.entityIds

        viewModel.moveGroupMember("group_does_not_exist", fromEntityId = members[0], toEntityId = members[1])
        advanceUntilIdle()

        assertEquals(members, groupContaining(members[0]).entityIds)
    }

    private fun entityKey(entityId: String) = "${OverviewViewModel.ENTITY_ITEM_PREFIX}$entityId"

    private suspend fun TestScope.givenEntitiesAndGroup(
        groupLightIds: List<String>,
        extraLightIds: List<String>,
    ): OverviewLightGroup {
        val all = (groupLightIds + extraLightIds).map { entityOf(it, state = "on") }
        givenEntities(*all.toTypedArray())
        viewModel.saveLightGroup(groupId = null, name = "Group", entityIds = groupLightIds, colorArgb = null)
        advanceUntilIdle()
        return groupContaining(groupLightIds.first())
    }

    @Test
    fun `Given a top-level light and a group when moveEntityIntoGroup targets a member then the light joins before that member`() = runTest {
        val group = givenEntitiesAndGroup(groupLightIds = listOf("light.gi1", "light.gi2"), extraLightIds = listOf("light.ei1"))

        viewModel.moveEntityIntoGroup(entityId = "light.ei1", groupId = group.id, targetEntityId = "light.gi2")
        advanceUntilIdle()

        assertEquals(listOf("light.gi1", "light.ei1", "light.gi2"), groupContaining("light.ei1").entityIds)
    }

    @Test
    fun `Given a non-light entity when moveEntityIntoGroup is called then the group is unchanged`() = runTest {
        givenEntities(entityOf("light.gn1", state = "on"), entityOf("light.gn2", state = "on"), entityOf("switch.sn1", state = "on"))
        viewModel.saveLightGroup(groupId = null, name = "Group", entityIds = listOf("light.gn1", "light.gn2"), colorArgb = null)
        advanceUntilIdle()
        val group = groupContaining("light.gn1")

        viewModel.moveEntityIntoGroup(entityId = "switch.sn1", groupId = group.id, targetEntityId = "light.gn2")
        advanceUntilIdle()

        assertEquals(listOf("light.gn1", "light.gn2"), groupContaining("light.gn1").entityIds)
    }

    @Test
    fun `Given a light already in the group when moveEntityIntoGroup targets another member then it is reordered`() = runTest {
        val group = givenEntitiesAndGroup(groupLightIds = listOf("light.gr1", "light.gr2", "light.gr3"), extraLightIds = emptyList())
        val members = group.entityIds

        viewModel.moveEntityIntoGroup(entityId = members[0], groupId = group.id, targetEntityId = members[2])
        advanceUntilIdle()

        assertEquals(listOf(members[1], members[2], members[0]), groupContaining(members[0]).entityIds)
    }

    @Test
    fun `Given an unknown group when moveEntityIntoGroup is called then the source group is unchanged`() = runTest {
        val group = givenEntitiesAndGroup(groupLightIds = listOf("light.gu1", "light.gu2"), extraLightIds = listOf("light.eu1"))

        viewModel.moveEntityIntoGroup(entityId = "light.eu1", groupId = "group_does_not_exist", targetEntityId = "light.gu1")
        advanceUntilIdle()

        assertEquals(listOf("light.gu1", "light.gu2"), groupContaining("light.gu1").entityIds)
    }

    @Test
    fun `Given a group of three and a top-level light when moveEntityOutOfGroup targets the light then the member leaves and lands next to it`() = runTest {
        val group = givenEntitiesAndGroup(groupLightIds = listOf("light.go1", "light.go2", "light.go3"), extraLightIds = listOf("light.eo1"))
        val members = group.entityIds

        viewModel.moveEntityOutOfGroup(groupId = group.id, entityId = members[0], targetKey = entityKey("light.eo1"))
        advanceUntilIdle()

        assertEquals(listOf(members[1], members[2]), groupContaining(members[1]).entityIds)
        val order = currentItemOrder()
        val movedIndex = order.indexOf(entityKey(members[0]))
        val targetIndex = order.indexOf(entityKey("light.eo1"))
        assertEquals(1, kotlin.math.abs(movedIndex - targetIndex))
    }

    @Test
    fun `Given a group of exactly two when moveEntityOutOfGroup removes one then the group dissolves`() = runTest {
        val group = givenEntitiesAndGroup(groupLightIds = listOf("light.gd1", "light.gd2"), extraLightIds = listOf("light.ed1"))

        viewModel.moveEntityOutOfGroup(groupId = group.id, entityId = "light.gd1", targetKey = entityKey("light.ed1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as OverviewUiState.Success
        assertEquals(emptyList<OverviewLightGroup>(), state.lightGroups)
    }

    @Test
    fun `Given an entity not in the group when moveEntityOutOfGroup is called then no exception is thrown`() = runTest {
        val group = givenEntitiesAndGroup(groupLightIds = listOf("light.gx1", "light.gx2", "light.gx3"), extraLightIds = listOf("light.ex1"))

        viewModel.moveEntityOutOfGroup(groupId = group.id, entityId = "light.ex1", targetKey = entityKey("light.gx1"))
        advanceUntilIdle()

        assertEquals(listOf("light.gx1", "light.gx2", "light.gx3"), groupContaining("light.gx1").entityIds)
    }
}

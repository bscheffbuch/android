package io.homeassistant.companion.android.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.servers.ServerManager
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val serverManager: ServerManager,
) : ViewModel() {

    companion object {
        val DISPLAY_DOMAINS = setOf(
            "light", "switch", "input_boolean", "fan", "cover",
            "lock", "climate", "media_player", "vacuum", "humidifier",
            "alarm_control_panel", "script", "automation", "button", "input_button",
        )
    }

    private val _uiState = MutableStateFlow<OverviewUiState>(OverviewUiState.Loading)
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    private val entityMap = mutableMapOf<String, Entity>()
    private var entityUpdatesJob: Job? = null

    init {
        loadEntities()
    }

    fun loadEntities() {
        _uiState.value = OverviewUiState.Loading
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE)
                    ?: run {
                        _uiState.value = OverviewUiState.Error("No active server configured")
                        return@launch
                    }
                val repository = serverManager.integrationRepository(server.id)
                val entities = repository.getEntities()
                if (entities == null) {
                    _uiState.value = OverviewUiState.Error("Failed to load entities")
                    return@launch
                }
                entityMap.clear()
                entities.forEach { entityMap[it.entityId] = it }
                _uiState.value = OverviewUiState.Success(sortedEntities())
                subscribeToEntityUpdates(server.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load entities for overview")
                _uiState.value = OverviewUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun subscribeToEntityUpdates(serverId: Int) {
        entityUpdatesJob?.cancel()
        entityUpdatesJob = viewModelScope.launch {
            try {
                val repository = serverManager.integrationRepository(serverId)
                repository.getEntityUpdates()?.collect { entity ->
                    entityMap[entity.entityId] = entity
                    _uiState.update { current ->
                        if (current is OverviewUiState.Success) {
                            current.copy(entities = sortedEntities())
                        } else {
                            current
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Entity updates stream ended")
            }
        }
    }

    private fun sortedEntities(): List<Entity> =
        entityMap.values
            .filter { it.domain in DISPLAY_DOMAINS }
            .sortedWith(compareBy({ domainOrder(it.domain) }, { it.entityId }))

    private fun domainOrder(domain: String): Int = when (domain) {
        "light" -> 0
        "switch", "input_boolean" -> 1
        "fan" -> 2
        "climate" -> 3
        "cover" -> 4
        "lock" -> 5
        "media_player" -> 6
        "vacuum" -> 7
        else -> 99
    }

    fun toggleEntity(entityId: String) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                val entity = entityMap[entityId] ?: return@launch
                val action = if (entity.isActive()) "turn_off" else "turn_on"
                repository.callAction(
                    domain = entity.domain,
                    action = action,
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle entity $entityId")
            }
        }
    }

    fun setBrightness(entityId: String, brightnessPercent: Float) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                val brightnessByte = (brightnessPercent / 100f * 255f).toInt().coerceIn(0, 255)
                repository.callAction(
                    domain = "light",
                    action = if (brightnessByte > 0) "turn_on" else "turn_off",
                    actionData = if (brightnessByte > 0) {
                        mapOf("entity_id" to entityId, "brightness" to brightnessByte)
                    } else {
                        mapOf("entity_id" to entityId)
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to set brightness for $entityId")
            }
        }
    }
}

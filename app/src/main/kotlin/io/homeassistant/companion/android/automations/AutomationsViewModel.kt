package io.homeassistant.companion.android.automations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.servers.ServerManager
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

private const val AUTOMATION_DOMAIN = "automation"
private const val SCENE_DOMAIN = "scene"
private val AUTOMATIONS_AND_SCENES_DOMAINS = setOf(AUTOMATION_DOMAIN, SCENE_DOMAIN)

@HiltViewModel
class AutomationsViewModel @Inject constructor(
    private val serverManager: ServerManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutomationsUiState>(AutomationsUiState.Loading)
    val uiState: StateFlow<AutomationsUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val entityMap = mutableMapOf<String, Entity>()
    private var entityUpdatesJob: Job? = null

    init {
        loadEntities()
    }

    fun loadEntities() {
        val current = _uiState.value
        _uiState.value = if (current is AutomationsUiState.Success) {
            current.copy(isRefreshing = true)
        } else {
            AutomationsUiState.Loading
        }
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE)
                    ?: run {
                        _uiState.value = AutomationsUiState.Error("No active server configured")
                        return@launch
                    }
                val repository = serverManager.integrationRepository(server.id)
                val entities = repository.getEntities()
                if (entities == null) {
                    _uiState.value = AutomationsUiState.Error(
                        "Home Assistant did not return entity data. Check that the active server is reachable and your session is still valid, then pull down to refresh.",
                    )
                    return@launch
                }
                entityMap.clear()
                entities.filter { it.domain in AUTOMATIONS_AND_SCENES_DOMAINS }.forEach { entityMap[it.entityId] = it }
                emitSuccess(isRefreshing = false)
                subscribeToEntityUpdates(server.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load automations")
                _uiState.value = AutomationsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun subscribeToEntityUpdates(serverId: Int) {
        entityUpdatesJob?.cancel()
        entityUpdatesJob = viewModelScope.launch {
            try {
                val repository = serverManager.integrationRepository(serverId)
                repository.getEntityUpdates()?.collect { entity ->
                    if (entity.domain !in AUTOMATIONS_AND_SCENES_DOMAINS) return@collect
                    entityMap[entity.entityId] = entity
                    emitSuccess(isRefreshing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Automation updates stream ended")
            }
        }
    }

    private fun emitSuccess(
        isRefreshing: Boolean = (_uiState.value as? AutomationsUiState.Success)?.isRefreshing ?: false,
    ) {
        val entities = entityMap.values
        _uiState.value = AutomationsUiState.Success(
            automations = entities.filter { it.domain == AUTOMATION_DOMAIN }.sortedBy { it.entityId },
            scenes = entities.filter { it.domain == SCENE_DOMAIN }.sortedBy { it.entityId },
            isRefreshing = isRefreshing,
        )
    }

    fun toggle(entityId: String) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                val entity = entityMap[entityId] ?: return@launch
                val action = if (entity.isActive()) "turn_off" else "turn_on"
                entityMap[entityId] = entity.copy(state = if (action == "turn_on") "on" else "off")
                emitSuccess()
                repository.callAction(
                    domain = AUTOMATION_DOMAIN,
                    action = action,
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle automation $entityId")
                _errorEvents.tryEmit(context.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Triggers an automation to run immediately, bypassing its configured conditions.
     *
     * `skip_conditions` is required here: without it, "Run now" silently no-ops whenever the
     * automation's conditions aren't currently met, which would read as a bug to the user.
     */
    fun triggerNow(entityId: String) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = AUTOMATION_DOMAIN,
                    action = "trigger",
                    actionData = mapOf("entity_id" to entityId, "skip_conditions" to true),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger automation $entityId")
                _errorEvents.tryEmit(context.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Activates a scene, calling the `scene.turn_on` service. Scenes are stateless triggers with
     * no persistent on/off value, so unlike [toggle] there is no optimistic state to update.
     */
    fun activateScene(entityId: String) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = SCENE_DOMAIN,
                    action = "turn_on",
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to activate scene $entityId")
                _errorEvents.tryEmit(context.getString(commonR.string.overview_action_failed))
            }
        }
    }
}

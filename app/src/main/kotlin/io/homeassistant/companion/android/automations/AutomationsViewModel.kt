package io.homeassistant.companion.android.automations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.friendlyName
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.servers.ServerManager
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

// Home Assistant's scene-config endpoint returns before its scene platform has finished reloading,
// so the freshly-saved scene isn't in the very next state fetch. Poll a few times (stopping the
// moment it shows up) so the new scene appears on its own instead of needing a manual refresh.
private const val SCENE_APPEAR_MAX_ATTEMPTS = 6
private val SCENE_APPEAR_RETRY_DELAY = 500.milliseconds

// Domains offered when saving the current state as a scene — the entity types whose state can be
// meaningfully captured and restored. Sensors, automations and scenes themselves are excluded.
private val SCENE_CANDIDATE_DOMAINS = setOf(
    "light",
    "switch",
    "input_boolean",
    "fan",
    "cover",
    "climate",
    "media_player",
    "lock",
    "humidifier",
)

@HiltViewModel
class AutomationsViewModel @Inject constructor(
    private val serverManager: ServerManager,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutomationsUiState>(AutomationsUiState.Loading)
    val uiState: StateFlow<AutomationsUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val entityMap = mutableMapOf<String, Entity>()
    private var entityUpdatesJob: Job? = null

    private val _saveSceneDialogState = MutableStateFlow<SaveSceneDialogUiState>(SaveSceneDialogUiState.Hidden)
    val saveSceneDialogState: StateFlow<SaveSceneDialogUiState> = _saveSceneDialogState.asStateFlow()

    // Snapshot of the entities shown in the save-scene dialog, so the scene captures their state as
    // it was when the dialog opened rather than re-reading it at save time.
    private var sceneCandidateEntities: Map<String, Entity> = emptyMap()

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

    /**
     * Opens the save-as-scene dialog, loading the current controllable entities to offer for
     * capture. The fetched entities are remembered so the scene reflects their state at this moment.
     */
    fun onCreateSceneClicked() {
        _saveSceneDialogState.value = SaveSceneDialogUiState.Loading
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: run {
                    _saveSceneDialogState.value = SaveSceneDialogUiState.Hidden
                    _errorEvents.tryEmit(context.getString(commonR.string.overview_action_failed))
                    return@launch
                }
                val repository = serverManager.integrationRepository(server.id)
                val entities = repository.getEntities().orEmpty()
                    .filter { it.domain in SCENE_CANDIDATE_DOMAINS }
                    .sortedBy { it.friendlyName.lowercase() }
                sceneCandidateEntities = entities.associateBy { it.entityId }
                _saveSceneDialogState.value = SaveSceneDialogUiState.Ready(
                    candidates = entities.map {
                        SceneEntityCandidate(
                            entityId = it.entityId,
                            friendlyName = it.friendlyName,
                            domain = it.domain,
                        )
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load entities for scene creation")
                _saveSceneDialogState.value = SaveSceneDialogUiState.Hidden
                _errorEvents.tryEmit(context.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /** Dismisses the save-as-scene dialog without saving. */
    fun onDismissCreateScene() {
        _saveSceneDialogState.value = SaveSceneDialogUiState.Hidden
    }

    /**
     * Captures the current state of the [selectedEntityIds] into a new persistent scene named
     * [name]. On success the dialog closes and the scene list refreshes; on failure the dialog stays
     * open so the user can retry, and a message explains that the server may not allow editing
     * scenes here.
     */
    fun saveScene(name: String, selectedEntityIds: Set<String>) {
        val current = _saveSceneDialogState.value
        if (current !is SaveSceneDialogUiState.Ready) return
        _saveSceneDialogState.value = current.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: run {
                    _saveSceneDialogState.value = current.copy(isSaving = false)
                    _errorEvents.tryEmit(context.getString(commonR.string.overview_save_scene_error))
                    return@launch
                }
                val repository = serverManager.integrationRepository(server.id)
                val selectedEntities = selectedEntityIds.mapNotNull { sceneCandidateEntities[it] }
                val sceneId = clock.now().toEpochMilliseconds().toString()
                val scenesBefore = entityMap.values.count { it.domain == SCENE_DOMAIN }
                val saved = repository.saveScene(
                    sceneId = sceneId,
                    name = name.trim(),
                    entities = buildSceneEntities(selectedEntities),
                )
                if (saved) {
                    _saveSceneDialogState.value = SaveSceneDialogUiState.Hidden
                    _errorEvents.tryEmit(context.getString(commonR.string.overview_save_scene_success, name.trim()))
                    refreshUntilSceneCountReaches(server.id, minSceneCount = scenesBefore + 1)
                } else {
                    _saveSceneDialogState.value = current.copy(isSaving = false)
                    _errorEvents.tryEmit(context.getString(commonR.string.overview_save_scene_error))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to save scene")
                _saveSceneDialogState.value = current.copy(isSaving = false)
                _errorEvents.tryEmit(context.getString(commonR.string.overview_save_scene_error))
            }
        }
    }

    /**
     * Re-fetches entities until the scene list contains at least [minSceneCount] scenes, or
     * [SCENE_APPEAR_MAX_ATTEMPTS] is reached. Bridges the brief window in which Home Assistant has
     * accepted a new scene but not yet reloaded its scene platform, so the just-saved scene surfaces
     * on its own rather than only after a manual refresh. Returns early the moment the scene appears.
     */
    private suspend fun refreshUntilSceneCountReaches(serverId: Int, minSceneCount: Int) {
        val repository = serverManager.integrationRepository(serverId)
        repeat(SCENE_APPEAR_MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(SCENE_APPEAR_RETRY_DELAY)
            val entities = repository.getEntities()
            if (entities != null) {
                entityMap.clear()
                entities.filter { it.domain in AUTOMATIONS_AND_SCENES_DOMAINS }
                    .forEach { entityMap[it.entityId] = it }
                emitSuccess(isRefreshing = false)
                if (entityMap.values.count { it.domain == SCENE_DOMAIN } >= minSceneCount) return
            }
        }
    }
}

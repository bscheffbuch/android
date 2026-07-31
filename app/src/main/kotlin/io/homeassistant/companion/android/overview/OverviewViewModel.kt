package io.homeassistant.companion.android.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getClimateHvacModes
import io.homeassistant.companion.android.common.data.integration.getClimateTargetTemperature
import io.homeassistant.companion.android.common.data.integration.getHumidifierMode
import io.homeassistant.companion.android.common.data.integration.getHumidifierModes
import io.homeassistant.companion.android.common.data.integration.getHumidifierTargetHumidity
import io.homeassistant.companion.android.common.data.integration.getVolumeLevel
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsVacuumTurnOn
import io.homeassistant.companion.android.common.data.servers.ServerManager
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val serverManager: ServerManager,
    @ApplicationContext context: Context,
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "overview_preferences"
        private const val KEY_LIGHT_GROUPS = "light_groups"
        private const val KEY_ITEM_ORDER = "item_order"
        private const val KEY_DISPLAYED_AS_LIGHT_OVERRIDES = "displayed_as_light_overrides"
        private const val LIVE_LIGHT_UPDATE_DELAY_MS = 60L
        const val ENTITY_ITEM_PREFIX = "entity:"
        const val GROUP_ITEM_PREFIX = "group:"

        val DISPLAY_DOMAINS = setOf(
            "light", "switch", "input_boolean", "fan", "cover",
            "lock", "climate", "media_player", "vacuum", "humidifier",
            "alarm_control_panel", "script", "automation", "button", "input_button",
        )
    }

    private val _uiState = MutableStateFlow<OverviewUiState>(OverviewUiState.Loading)
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val entityMap = mutableMapOf<String, Entity>()
    private val applicationContext = context.applicationContext
    private val preferences by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private var lightGroups: List<OverviewLightGroup> = emptyList()
    private var itemOrder: List<String> = emptyList()
    private var expandedLightGroupIds: Set<String> = emptySet()
    private var displayedAsLightOverrides: Set<String> = emptySet()
    private var editMode = false
    private var entityUpdatesJob: Job? = null
    private val brightnessJobs = mutableMapOf<String, Job>()
    private val colorJobs = mutableMapOf<String, Job>()
    private val colorTemperatureJobs = mutableMapOf<String, Job>()
    private val fanSpeedJobs = mutableMapOf<String, Job>()
    private val coverPositionJobs = mutableMapOf<String, Job>()
    private val climateTemperatureJobs = mutableMapOf<String, Job>()
    private val humidifierHumidityJobs = mutableMapOf<String, Job>()
    private val mediaVolumeJobs = mutableMapOf<String, Job>()
    private val pendingLightActions = mutableMapOf<String, suspend () -> Unit>()

    init {
        viewModelScope.launch {
            loadLocalState()
            loadEntities()
        }
    }

    private suspend fun loadLocalState() {
        withContext(Dispatchers.IO) {
            lightGroups = loadLightGroups()
            itemOrder = loadItemOrder()
            displayedAsLightOverrides = loadDisplayedAsLightOverrides()
        }
    }

    fun loadEntities() {
        val current = _uiState.value
        if (current is OverviewUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true)
        } else {
            _uiState.value = OverviewUiState.Loading
        }
        viewModelScope.launch {
            try {
                Timber.d("OverviewViewModel: loadEntities() started")
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE)
                    ?: run {
                        Timber.e("OverviewViewModel: No active server found!")
                        _uiState.value = OverviewUiState.Error("No active server configured")
                        return@launch
                    }
                Timber.d("OverviewViewModel: Using server id=${server.id}")
                val repository = serverManager.integrationRepository(server.id)
                Timber.d("OverviewViewModel: Calling getEntities()")
                val entities = repository.getEntities()
                Timber.d("OverviewViewModel: getEntities() returned ${entities?.size ?: "null"} entities")
                if (entities == null) {
                    _uiState.value = OverviewUiState.Error(
                        "Home Assistant did not return entity data. Check that the active server is reachable and your session is still valid, then pull down to refresh.",
                    )
                    return@launch
                }
                entityMap.clear()
                entities.forEach { entityMap[it.entityId] = it }
                val filtered = sortedEntities()
                Timber.d(
                    "OverviewViewModel: ${entities.size} total, ${filtered.size} after domain filter (domains: $DISPLAY_DOMAINS)",
                )
                emitSuccess(filtered, isRefreshing = false)
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
                    emitSuccess(isRefreshing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Entity updates stream ended")
            }
        }
    }

    private fun sortedEntities(): List<Entity> = entityMap.values
        .filter { it.domain in DISPLAY_DOMAINS }
        .sortedWith(compareBy({ domainOrder(it.domain) }, { it.entityId }))

    private fun emitSuccess(
        entities: List<Entity> = sortedEntities(),
        isRefreshing: Boolean = (_uiState.value as? OverviewUiState.Success)?.isRefreshing ?: false,
    ) {
        val validGroups = lightGroups.sanitizeLightGroups()
        if (validGroups != lightGroups) {
            lightGroups = validGroups
            saveLightGroups()
        }
        val validDisplayedAsLightOverrides = displayedAsLightOverrides.sanitizeDisplayedAsLightOverrides()
        if (validDisplayedAsLightOverrides != displayedAsLightOverrides) {
            displayedAsLightOverrides = validDisplayedAsLightOverrides
            saveDisplayedAsLightOverrides()
        }
        expandedLightGroupIds = expandedLightGroupIds.intersect(validGroups.map { it.id }.toSet())
        itemOrder = normalizedItemOrder(entities, validGroups, itemOrder)
        _uiState.value = OverviewUiState.Success(
            entities = entities,
            lightGroups = validGroups,
            itemOrder = itemOrder,
            expandedLightGroupIds = expandedLightGroupIds,
            displayedAsLightEntityIds = validDisplayedAsLightOverrides,
            isRefreshing = isRefreshing,
            isEditMode = editMode,
        )
    }

    private fun List<OverviewLightGroup>.sanitizeLightGroups(): List<OverviewLightGroup> = map { group ->
        group.copy(entityIds = group.entityIds.distinct().filter { entityMap[it]?.domain == "light" })
    }.filter { it.entityIds.size >= 2 }

    private fun Set<String>.sanitizeDisplayedAsLightOverrides(): Set<String> = filter { entityId ->
        entityMap[entityId]?.supportsDisplayAsLight() == true
    }
        .toSet()

    private fun normalizedItemOrder(
        entities: List<Entity>,
        groups: List<OverviewLightGroup>,
        currentOrder: List<String>,
    ): List<String> {
        val validKeys = groups.map { groupKey(it.id) } + entities.map { entityKey(it.entityId) }
        return currentOrder.filter { it in validKeys } + validKeys.filter { it !in currentOrder }
    }

    private fun domainOrder(domain: String): Int = when (domain) {
        "light" -> 0
        "switch", "input_boolean" -> 1
        "fan" -> 2
        "climate" -> 3
        "cover" -> 4
        "lock" -> 5
        "media_player" -> 6
        "vacuum" -> 7
        // Automations are controls rather than devices, so they are grouped after the
        // physical/device domains but before purely presentational fallbacks.
        "automation", "script" -> 8
        else -> 99
    }

    /**
     * Toggles an entity's on/off-equivalent state, calling the Home Assistant service that
     * actually implements that toggle for its domain. Most domains support generic
     * `turn_on`/`turn_off` services, but a few do not: `lock`/`alarm_control_panel` use their own
     * named actions, `cover` has no `turn_on`/`turn_off` service at all (only `open_cover`/
     * `close_cover`), and `button`/`input_button` are stateless and only support `press`.
     */
    fun toggleEntity(entityId: String) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                val entity = entityMap[entityId] ?: return@launch
                val wasActive = entity.isActive()
                val (action, optimisticState) = when (entity.domain) {
                    "lock" -> (if (wasActive) "unlock" else "lock") to (if (wasActive) "unlocked" else "locked")
                    "alarm_control_panel" -> {
                        (if (wasActive) "alarm_disarm" else "alarm_arm_away") to
                            (if (wasActive) "disarmed" else "armed_away")
                    }
                    "cover" -> (if (wasActive) "close_cover" else "open_cover") to null
                    "button", "input_button" -> "press" to null
                    "vacuum" -> {
                        if (entity.supportsVacuumTurnOn()) {
                            (if (wasActive) "turn_off" else "turn_on") to (if (wasActive) "off" else "on")
                        } else {
                            (if (wasActive) "return_to_base" else "start") to null
                        }
                    }
                    else -> (if (wasActive) "turn_off" else "turn_on") to (if (wasActive) "off" else "on")
                }
                optimisticState?.let { updateOptimisticEntities(listOf(entityId), it) }
                repository.callAction(
                    domain = entity.domain,
                    action = action,
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle entity $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Triggers an automation to run immediately, bypassing its configured conditions.
     *
     * `skip_conditions` is required here: without it, "Run now" silently no-ops whenever the
     * automation's conditions aren't currently met, which would read as a bug to the user.
     */
    fun triggerAutomation(entityId: String) {
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = "automation",
                    action = "trigger",
                    actionData = mapOf("entity_id" to entityId, "skip_conditions" to true),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger automation $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    fun toggleLightGroup(groupId: String) {
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        val anyActive = group.entityIds.any { entityMap[it]?.isActive() == true }
        updateOptimisticEntities(group.entityIds, if (anyActive) "off" else "on")
        viewModelScope.launch {
            try {
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = "light",
                    action = if (anyActive) "turn_off" else "turn_on",
                    actionData = mapOf("entity_id" to group.entityIds),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle light group $groupId")
            }
        }
    }

    fun setBrightness(entityId: String, brightnessPercent: Float, immediate: Boolean = true) {
        setBrightness(listOf(entityId), brightnessPercent, immediate)
    }

    fun setGroupBrightness(groupId: String, brightnessPercent: Float, immediate: Boolean = true) {
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        setBrightness(group.entityIds, brightnessPercent, immediate)
    }

    private fun setBrightness(entityIds: List<String>, brightnessPercent: Float, immediate: Boolean) {
        val validEntityIds = entityIds.filter { entityMap[it]?.domain == "light" }
        if (validEntityIds.isEmpty()) return
        val brightnessByte = (brightnessPercent / 100f * 255f).toInt().coerceIn(0, 255)
        updateOptimisticLights(
            entityIds = validEntityIds,
            state = if (brightnessByte > 0) "on" else "off",
        ) { attributes ->
            if (brightnessByte > 0) attributes + ("brightness" to brightnessByte) else attributes - "brightness"
        }
        scheduleLightAction(
            key = "brightness:${validEntityIds.joinToString(",")}",
            jobs = brightnessJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "light",
                action = if (brightnessByte > 0) "turn_on" else "turn_off",
                actionData = if (brightnessByte > 0) {
                    mapOf("entity_id" to validEntityIds, "brightness" to brightnessByte)
                } else {
                    mapOf("entity_id" to validEntityIds)
                },
            )
        }
    }

    fun setLightColor(entityId: String, rgbColor: List<Int>) {
        setLightColor(listOf(entityId), rgbColor)
    }

    fun setGroupColor(groupId: String, rgbColor: List<Int>) {
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        setLightColor(group.entityIds, rgbColor)
    }

    private fun setLightColor(entityIds: List<String>, rgbColor: List<Int>) {
        val validEntityIds = entityIds.filter { entityMap[it]?.domain == "light" }
        if (validEntityIds.isEmpty()) return
        val safeRgbColor = rgbColor.take(3).map { it.coerceIn(0, 255) }
        updateOptimisticLights(validEntityIds) {
            it + ("rgb_color" to safeRgbColor) + ("color_mode" to "rgb")
        }
        scheduleLightAction(
            key = "color:${validEntityIds.joinToString(",")}",
            jobs = colorJobs,
            immediate = false,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "light",
                action = "turn_on",
                actionData = mapOf("entity_id" to validEntityIds, "rgb_color" to safeRgbColor),
            )
        }
    }

    fun setLightColorTemperature(entityId: String, kelvin: Int, immediate: Boolean = true) {
        setLightColorTemperature(listOf(entityId), kelvin, immediate)
    }

    fun setGroupColorTemperature(groupId: String, kelvin: Int, immediate: Boolean = true) {
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        setLightColorTemperature(group.entityIds, kelvin, immediate)
    }

    private fun setLightColorTemperature(entityIds: List<String>, kelvin: Int, immediate: Boolean) {
        val validEntityIds = entityIds.filter { entityMap[it]?.domain == "light" }
        if (validEntityIds.isEmpty()) return
        val safeKelvin = kelvin.coerceIn(1000, 12000)
        updateOptimisticLights(validEntityIds) {
            it + ("color_temp_kelvin" to safeKelvin) + ("color_mode" to "color_temp")
        }
        scheduleLightAction(
            key = "temperature:${validEntityIds.joinToString(",")}",
            jobs = colorTemperatureJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "light",
                action = "turn_on",
                actionData = mapOf("entity_id" to validEntityIds, "color_temp_kelvin" to safeKelvin),
            )
        }
    }

    fun setFanSpeed(entityId: String, percentage: Float, immediate: Boolean = true) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "fan") return
        val safePercentage = percentage.coerceIn(0f, 100f).toInt()
        updateOptimisticLights(
            entityIds = listOf(entityId),
            state = if (safePercentage > 0) "on" else "off",
        ) { attributes -> attributes + ("percentage" to safePercentage) }
        scheduleLightAction(
            key = "fan_speed:$entityId",
            jobs = fanSpeedJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "fan",
                action = "set_percentage",
                actionData = mapOf("entity_id" to entityId, "percentage" to safePercentage),
            )
        }
    }

    /**
     * Adjusts a cover's position, calling the `set_cover_position` service. Only meaningful for
     * covers that support [io.homeassistant.companion.android.common.data.integration.supportsCoverSetPosition];
     * covers without that support only expose the open/close toggle handled by [toggleEntity].
     */
    fun setCoverPosition(entityId: String, percentage: Float, immediate: Boolean = true) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "cover") return
        val safePosition = percentage.coerceIn(0f, 100f).toInt()
        updateOptimisticLights(
            entityIds = listOf(entityId),
            state = if (safePosition > 0) "open" else "closed",
        ) { attributes -> attributes + ("current_position" to safePosition) }
        scheduleLightAction(
            key = "cover_position:$entityId",
            jobs = coverPositionJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "cover",
                action = "set_cover_position",
                actionData = mapOf("entity_id" to entityId, "position" to safePosition),
            )
        }
    }

    /**
     * Stops a cover mid-movement, calling the `stop_cover` service. Only meaningful for covers
     * that support [io.homeassistant.companion.android.common.data.integration.supportsCoverStop].
     */
    fun stopCover(entityId: String) {
        viewModelScope.launch {
            try {
                val entity = entityMap[entityId] ?: return@launch
                if (entity.domain != "cover") return@launch
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = "cover",
                    action = "stop_cover",
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop cover $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Adjusts a climate entity's target temperature, calling the `set_temperature` service. Only
     * meaningful for entities that support
     * [io.homeassistant.companion.android.common.data.integration.supportsClimateSetTemperature].
     */
    fun setClimateTemperature(entityId: String, temperature: Float, immediate: Boolean = true) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "climate") return
        val position = entity.getClimateTargetTemperature() ?: return
        val safeTemperature = temperature.coerceIn(position.min, position.max)
        updateOptimisticLights(
            entityIds = listOf(entityId),
            state = entity.state,
        ) { attributes -> attributes + ("temperature" to safeTemperature) }
        scheduleLightAction(
            key = "climate_temperature:$entityId",
            jobs = climateTemperatureJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "climate",
                action = "set_temperature",
                actionData = mapOf("entity_id" to entityId, "temperature" to safeTemperature),
            )
        }
    }

    /**
     * Switches a climate entity to the next HVAC mode in its supported
     * [io.homeassistant.companion.android.common.data.integration.getClimateHvacModes] list,
     * wrapping back to the first mode after the last. Mirrors the cycling behavior of
     * [io.homeassistant.companion.android.controls.ClimateControl]'s Device Controls tile.
     */
    fun cycleClimateHvacMode(entityId: String) {
        viewModelScope.launch {
            try {
                val entity = entityMap[entityId] ?: return@launch
                if (entity.domain != "climate") return@launch
                val modes = entity.getClimateHvacModes()
                if (modes.isEmpty()) return@launch
                val nextMode = modes[(modes.indexOf(entity.state) + 1) % modes.size]
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                updateOptimisticEntities(listOf(entityId), nextMode)
                repository.callAction(
                    domain = "climate",
                    action = "set_hvac_mode",
                    actionData = mapOf("entity_id" to entityId, "hvac_mode" to nextMode),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to cycle HVAC mode for $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Adjusts a humidifier's target humidity, calling the `set_humidity` service.
     */
    fun setHumidifierHumidity(entityId: String, humidity: Float, immediate: Boolean = true) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "humidifier") return
        val position = entity.getHumidifierTargetHumidity() ?: return
        val safeHumidity = humidity.coerceIn(position.min, position.max).roundToInt()
        updateOptimisticLights(
            entityIds = listOf(entityId),
            state = entity.state,
        ) { attributes -> attributes + ("humidity" to safeHumidity) }
        scheduleLightAction(
            key = "humidifier_humidity:$entityId",
            jobs = humidifierHumidityJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "humidifier",
                action = "set_humidity",
                actionData = mapOf("entity_id" to entityId, "humidity" to safeHumidity),
            )
        }
    }

    /**
     * Cycles a humidifier to its next available preset mode, calling
     * [setHumidifierMode] with the mode that follows the entity's current one in
     * [io.homeassistant.companion.android.common.data.integration.getHumidifierModes], wrapping
     * back to the first mode after the last. Only meaningful for entities that support
     * [io.homeassistant.companion.android.common.data.integration.supportsHumidifierModes].
     */
    fun cycleHumidifierMode(entityId: String) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "humidifier") return
        val modes = entity.getHumidifierModes()
        if (modes.isEmpty()) return
        val currentMode = entity.getHumidifierMode()
        val nextMode = modes[(modes.indexOf(currentMode) + 1) % modes.size]
        setHumidifierMode(entityId, nextMode)
    }

    /**
     * Sets a humidifier's preset mode, calling the `set_mode` service. Only meaningful for
     * entities that support
     * [io.homeassistant.companion.android.common.data.integration.supportsHumidifierModes].
     */
    fun setHumidifierMode(entityId: String, mode: String) {
        viewModelScope.launch {
            try {
                val entity = entityMap[entityId] ?: return@launch
                if (entity.domain != "humidifier") return@launch
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                updateOptimisticLights(
                    entityIds = listOf(entityId),
                    state = entity.state,
                ) { attributes -> attributes + ("mode" to mode) }
                repository.callAction(
                    domain = "humidifier",
                    action = "set_mode",
                    actionData = mapOf("entity_id" to entityId, "mode" to mode),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to set humidifier mode for $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Toggles a media player between playing and paused, calling the `media_play_pause` service.
     */
    fun toggleMediaPlayback(entityId: String) {
        viewModelScope.launch {
            try {
                val entity = entityMap[entityId] ?: return@launch
                if (entity.domain != "media_player") return@launch
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = "media_player",
                    action = "media_play_pause",
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle media playback for $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Skips a media player to the previous track, calling the `media_previous_track` service.
     * Only meaningful for entities that support
     * [io.homeassistant.companion.android.common.data.integration.supportsMediaPreviousTrack].
     */
    fun skipToPreviousTrack(entityId: String) {
        viewModelScope.launch {
            try {
                val entity = entityMap[entityId] ?: return@launch
                if (entity.domain != "media_player") return@launch
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = "media_player",
                    action = "media_previous_track",
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to skip to previous track for $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Skips a media player to the next track, calling the `media_next_track` service. Only
     * meaningful for entities that support
     * [io.homeassistant.companion.android.common.data.integration.supportsMediaNextTrack].
     */
    fun skipToNextTrack(entityId: String) {
        viewModelScope.launch {
            try {
                val entity = entityMap[entityId] ?: return@launch
                if (entity.domain != "media_player") return@launch
                val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@launch
                val repository = serverManager.integrationRepository(server.id)
                repository.callAction(
                    domain = "media_player",
                    action = "media_next_track",
                    actionData = mapOf("entity_id" to entityId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to skip to next track for $entityId")
                _errorEvents.tryEmit(applicationContext.getString(commonR.string.overview_action_failed))
            }
        }
    }

    /**
     * Adjusts a media player's volume, calling the `volume_set` service. Only meaningful for
     * entities that support
     * [io.homeassistant.companion.android.common.data.integration.supportsVolumeSet].
     */
    fun setMediaVolume(entityId: String, volume: Float, immediate: Boolean = true) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "media_player") return
        val position = entity.getVolumeLevel() ?: return
        val safeVolume = volume.coerceIn(position.min, position.max)
        updateOptimisticLights(
            entityIds = listOf(entityId),
            state = entity.state,
        ) { attributes -> attributes + ("volume_level" to safeVolume / 100f) }
        scheduleLightAction(
            key = "media_volume:$entityId",
            jobs = mediaVolumeJobs,
            immediate = immediate,
        ) {
            val server = serverManager.getServer(ServerManager.SERVER_ID_ACTIVE) ?: return@scheduleLightAction
            val repository = serverManager.integrationRepository(server.id)
            repository.callAction(
                domain = "media_player",
                action = "volume_set",
                actionData = mapOf("entity_id" to entityId, "volume_level" to safeVolume / 100f),
            )
        }
    }

    private fun scheduleLightAction(
        key: String,
        jobs: MutableMap<String, Job>,
        immediate: Boolean,
        action: suspend () -> Unit,
    ) {
        if (immediate) {
            pendingLightActions.remove(key)
            jobs.remove(key)?.cancel()
            jobs[key] = viewModelScope.launch {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to send live light action $key")
                } finally {
                    jobs.remove(key)
                }
            }
            return
        }

        pendingLightActions[key] = action
        if (jobs[key]?.isActive == true) return

        jobs[key] = viewModelScope.launch {
            try {
                while (true) {
                    val pendingAction = pendingLightActions.remove(key) ?: break
                    pendingAction()
                    delay(LIVE_LIGHT_UPDATE_DELAY_MS)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to send live light action $key")
            } finally {
                jobs.remove(key)
            }
        }
    }

    private fun updateOptimisticLights(
        entityIds: List<String>,
        state: String = "on",
        attributesUpdate: (Map<String, Any?>) -> Map<String, Any?>,
    ) {
        val ids = entityIds.toSet()
        ids.forEach { entityId ->
            val entity = entityMap[entityId] ?: return@forEach
            entityMap[entityId] = entity.copy(
                state = state,
                attributes = attributesUpdate(entity.attributes),
            )
        }
        _uiState.update { current ->
            if (current is OverviewUiState.Success) {
                current.copy(entities = sortedEntities())
            } else {
                current
            }
        }
    }

    private fun updateOptimisticEntities(entityIds: List<String>, state: String) {
        entityIds.forEach { entityId ->
            val entity = entityMap[entityId] ?: return@forEach
            entityMap[entityId] = entity.copy(state = state)
        }
        emitSuccess()
    }

    fun setEditMode(enabled: Boolean) {
        editMode = enabled
        emitSuccess()
    }

    fun setDisplayedAsLight(entityId: String, asLight: Boolean) {
        val entity = entityMap[entityId] ?: return
        if (!entity.supportsDisplayAsLight()) return
        displayedAsLightOverrides = if (asLight) {
            displayedAsLightOverrides + entityId
        } else {
            displayedAsLightOverrides - entityId
        }
        saveDisplayedAsLightOverrides()
        emitSuccess()
    }

    fun setLightGroupExpanded(groupId: String, expanded: Boolean) {
        expandedLightGroupIds = if (expanded) {
            expandedLightGroupIds + groupId
        } else {
            expandedLightGroupIds - groupId
        }
        emitSuccess()
    }

    fun saveLightGroup(groupId: String?, name: String, entityIds: List<String>, colorArgb: Long? = null) {
        val validIds = entityIds.distinct().filter { entityMap[it]?.domain == "light" }
        val trimmedName = name.trim()
        if (validIds.size < 2) {
            if (groupId != null) deleteLightGroup(groupId)
            return
        }
        if (trimmedName.isBlank()) return
        val id = groupId ?: "local_${System.currentTimeMillis()}"
        val group = OverviewLightGroup(
            id = id,
            name = trimmedName,
            entityIds = validIds,
            colorArgb = colorArgb,
        )
        lightGroups = lightGroups.filterNot { it.id == id } + group
        expandedLightGroupIds = expandedLightGroupIds + id
        saveLightGroups()
        emitSuccess()
    }

    fun addLightToGroup(groupId: String, entityId: String) {
        val entity = entityMap[entityId] ?: return
        if (entity.domain != "light") return
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        saveLightGroup(group.id, group.name, group.entityIds + entityId, group.colorArgb)
    }

    /**
     * Removes a single member entity from a light group, dragged out of its expanded card.
     * Relies on [saveLightGroup]'s existing auto-dissolve behavior: if fewer than two valid
     * entities remain, the group itself is deleted instead of being saved with one member.
     */
    fun removeLightFromGroup(groupId: String, entityId: String) {
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        saveLightGroup(group.id, group.name, group.entityIds - entityId, group.colorArgb)
    }

    /**
     * Reorders a light group's members live as one is dragged over another inside the expanded
     * group card, using the same insertion (move) semantics as the top-level grid's
     * [moveItem]: [fromEntityId] is lifted out of the group's ordered [OverviewLightGroup.entityIds]
     * and re-inserted next to [toEntityId], shifting the members in between by one so the displaced
     * cards cascade past the dragged one instead of a single pair swapping. Direction is inferred
     * from the original positions so the member always lands on the side the drag came from.
     *
     * The new order is persisted (the group's member order is part of its stored definition), so a
     * reorder survives collapse/expand and app restarts just like a top-level reorder.
     */
    fun moveGroupMember(groupId: String, fromEntityId: String, toEntityId: String) {
        if (fromEntityId == toEntityId) return
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        val updated = group.entityIds.toMutableList()
        val fromIndex = updated.indexOf(fromEntityId)
        val toIndex = updated.indexOf(toEntityId)
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return
        updated.removeAt(fromIndex)
        val insertIndex = updated.indexOf(toEntityId).let { if (fromIndex < toIndex) it + 1 else it }
        updated.add(insertIndex, fromEntityId)
        lightGroups = lightGroups.map { if (it.id == groupId) it.copy(entityIds = updated) else it }
        saveLightGroups()
        emitSuccess()
    }

    /** The id of the light group currently containing [entityId], or null when it is not grouped. */
    private fun currentGroupIdOf(entityId: String): String? = lightGroups.firstOrNull { entityId in it.entityIds }?.id

    /**
     * Live cross-boundary move used while an entity is dragged **into** an expanded light group from
     * the top-level grid (or from a different group): places [entityId] next to member
     * [targetEntityId] inside group [groupId]. Only lights can join a light group, so this is a no-op
     * for any other domain.
     *
     * If [entityId] is already a member of [groupId] it is simply reordered next to [targetEntityId]
     * ([moveGroupMember]); otherwise it is removed from whatever group it previously belonged to and
     * inserted immediately before [targetEntityId]. As the finger keeps moving over other members the
     * usual insertion-move ([moveGroupMember]) takes over, so that first landing point self-corrects.
     *
     * The new membership is persisted. A source group left with fewer than two lights dissolves via
     * the usual sanitize on the next emit, exactly like [removeLightFromGroup].
     */
    fun moveEntityIntoGroup(entityId: String, groupId: String, targetEntityId: String) {
        if (entityId == targetEntityId) return
        if (entityMap[entityId]?.domain != "light") return
        val targetGroup = lightGroups.firstOrNull { it.id == groupId } ?: return
        if (targetEntityId !in targetGroup.entityIds) return
        val sourceGroupId = currentGroupIdOf(entityId)
        if (sourceGroupId == groupId) {
            moveGroupMember(groupId = groupId, fromEntityId = entityId, toEntityId = targetEntityId)
            return
        }
        lightGroups = lightGroups.map { group ->
            when (group.id) {
                sourceGroupId -> group.copy(entityIds = group.entityIds - entityId)
                groupId -> {
                    val ids = group.entityIds.toMutableList().apply { remove(entityId) }
                    val insertIndex = ids.indexOf(targetEntityId).coerceAtLeast(0)
                    ids.add(insertIndex, entityId)
                    group.copy(entityIds = ids)
                }
                else -> group
            }
        }
        saveLightGroups()
        emitSuccess()
    }

    /**
     * Live cross-boundary move used while a member is dragged **out** of its expanded light group onto
     * a top-level cell: removes [entityId] from group [groupId] and repositions its top-level key next
     * to [targetKey] in [itemOrder] using the same insertion semantics as [moveItem], so the card
     * lands where the finger is instead of snapping back to its stored slot. If removing it leaves the
     * group with fewer than two lights the group dissolves via the usual sanitize on the next emit.
     */
    fun moveEntityOutOfGroup(groupId: String, entityId: String, targetKey: String) {
        val group = lightGroups.firstOrNull { it.id == groupId } ?: return
        if (entityId !in group.entityIds) return
        val sourceKey = entityKey(entityId)
        if (sourceKey == targetKey) return
        lightGroups = lightGroups.map { if (it.id == groupId) it.copy(entityIds = it.entityIds - entityId) else it }
        val updated = itemOrder.toMutableList()
        if (sourceKey !in updated) updated.add(sourceKey)
        val fromIndex = updated.indexOf(sourceKey)
        val toIndex = updated.indexOf(targetKey)
        if (toIndex >= 0 && fromIndex != toIndex) {
            updated.removeAt(fromIndex)
            val insertIndex = updated.indexOf(targetKey).let { if (fromIndex < toIndex) it + 1 else it }
            updated.add(insertIndex, sourceKey)
        }
        itemOrder = updated
        saveLightGroups()
        saveItemOrder()
        emitSuccess()
    }

    fun createLightGroupFromEntities(firstEntityId: String, secondEntityId: String) {
        val first = entityMap[firstEntityId] ?: return
        val second = entityMap[secondEntityId] ?: return
        if (first.domain != "light" || second.domain != "light") return
        val name = listOf(first, second)
            .joinToString(" + ") { it.attributes["friendly_name"]?.toString() ?: it.entityId.substringAfter('.') }
        saveLightGroup(groupId = null, name = name, entityIds = listOf(firstEntityId, secondEntityId))
    }

    fun handleItemDrop(sourceKey: String, targetKey: String) {
        if (sourceKey == targetKey) return
        val sourceEntityId = sourceKey.removePrefix(ENTITY_ITEM_PREFIX).takeIf {
            sourceKey.startsWith(ENTITY_ITEM_PREFIX)
        }
        val targetEntityId = targetKey.removePrefix(ENTITY_ITEM_PREFIX).takeIf {
            targetKey.startsWith(ENTITY_ITEM_PREFIX)
        }
        val sourceGroupId = sourceKey.removePrefix(GROUP_ITEM_PREFIX).takeIf { sourceKey.startsWith(GROUP_ITEM_PREFIX) }
        val targetGroupId = targetKey.removePrefix(GROUP_ITEM_PREFIX).takeIf { targetKey.startsWith(GROUP_ITEM_PREFIX) }

        when {
            sourceEntityId != null && targetGroupId != null -> addLightToGroup(targetGroupId, sourceEntityId)
            sourceGroupId != null && targetEntityId != null -> addLightToGroup(sourceGroupId, targetEntityId)
            sourceEntityId != null && targetEntityId != null -> createLightGroupFromEntities(
                sourceEntityId,
                targetEntityId,
            )
            else -> moveItem(sourceKey, targetKey)
        }
    }

    fun deleteLightGroup(groupId: String) {
        lightGroups = lightGroups.filterNot { it.id == groupId }
        itemOrder = itemOrder.filterNot { it == groupKey(groupId) }
        saveLightGroups()
        saveItemOrder()
        emitSuccess()
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value as? OverviewUiState.Success ?: return
        val updated = current.itemOrder.toMutableList()
        if (fromIndex !in updated.indices || toIndex !in updated.indices || fromIndex == toIndex) return
        updated[fromIndex] = updated[toIndex].also { updated[toIndex] = updated[fromIndex] }
        itemOrder = updated
        saveItemOrder()
        emitSuccess()
    }

    /**
     * Reorders [fromKey] to sit at [toKey]'s slot using insertion (move) semantics rather than a
     * two-cell swap: the source key is lifted out of the order and re-inserted next to the target,
     * shifting every key in between by one. This is what lets the grid "shuffle" live as a card is
     * dragged — repeatedly moving the dragged item one target at a time opens a gap that follows the
     * finger, with the displaced cards cascading past it instead of a single pair trading places.
     *
     * Direction is inferred from the original positions: dragging the source past a target that was
     * ahead of it inserts *after* that target, dragging it past a target that was behind inserts
     * *before*, so the dragged item always lands on the side the drag came from.
     */
    fun moveItem(fromKey: String, toKey: String) {
        if (fromKey == toKey) return
        val updated = itemOrder.toMutableList()
        val fromIndex = updated.indexOf(fromKey)
        val toIndex = updated.indexOf(toKey)
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return
        updated.removeAt(fromIndex)
        val insertIndex = updated.indexOf(toKey).let { if (fromIndex < toIndex) it + 1 else it }
        updated.add(insertIndex, fromKey)
        itemOrder = updated
        saveItemOrder()
        emitSuccess()
    }

    private fun loadLightGroups(): List<OverviewLightGroup> {
        val raw = preferences.getString(KEY_LIGHT_GROUPS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val entityArray = item.getJSONArray("entityIds")
                OverviewLightGroup(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    entityIds = List(entityArray.length()) { entityArray.getString(it) },
                    colorArgb = if (item.has("colorArgb")) item.optLong("colorArgb") else null,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Unable to load overview light groups")
            emptyList()
        }
    }

    private fun saveLightGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            lightGroups.forEach { group ->
                array.put(
                    JSONObject()
                        .put("id", group.id)
                        .put("name", group.name)
                        .put("entityIds", JSONArray(group.entityIds))
                        .also { item ->
                            group.colorArgb?.let { item.put("colorArgb", it) }
                        },
                )
            }
            preferences.edit().putString(KEY_LIGHT_GROUPS, array.toString()).apply()
        }
    }

    private fun loadItemOrder(): List<String> {
        val raw = preferences.getString(KEY_ITEM_ORDER, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            Timber.w(e, "Unable to load overview item order")
            emptyList()
        }
    }

    private fun saveItemOrder() {
        val order = itemOrder
        viewModelScope.launch(Dispatchers.IO) {
            preferences.edit().putString(KEY_ITEM_ORDER, JSONArray(order).toString()).apply()
        }
    }

    private fun loadDisplayedAsLightOverrides(): Set<String> {
        val raw = preferences.getString(KEY_DISPLAYED_AS_LIGHT_OVERRIDES, null) ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }.toSet()
        } catch (e: Exception) {
            Timber.w(e, "Unable to load overview displayed-as-light overrides")
            emptySet()
        }
    }

    private fun saveDisplayedAsLightOverrides() {
        val overrides = displayedAsLightOverrides
        viewModelScope.launch(Dispatchers.IO) {
            preferences.edit().putString(
                KEY_DISPLAYED_AS_LIGHT_OVERRIDES,
                JSONArray(overrides.toList()).toString(),
            ).apply()
        }
    }

    private fun entityKey(entityId: String) = "$ENTITY_ITEM_PREFIX$entityId"
    private fun groupKey(groupId: String) = "$GROUP_ITEM_PREFIX$groupId"
}

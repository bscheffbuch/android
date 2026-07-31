package io.homeassistant.companion.android.common.data.integration

import dagger.assisted.AssistedFactory
import io.homeassistant.companion.android.common.data.integration.impl.IntegrationRepositoryImpl
import io.homeassistant.companion.android.common.data.integration.impl.entities.RateLimitResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AssistPipelineEvent
import io.homeassistant.companion.android.common.data.websocket.impl.entities.GetConfigResponse
import kotlinx.coroutines.flow.Flow

interface IntegrationRepository {

    suspend fun registerDevice(deviceRegistration: DeviceRegistration)
    suspend fun updateRegistration(deviceRegistration: DeviceRegistration, allowReregistration: Boolean = true)
    suspend fun getRegistration(): DeviceRegistration
    suspend fun deletePreferences()

    suspend fun getNotificationRateLimits(): RateLimitResponse

    suspend fun renderTemplate(template: String, variables: Map<String, String>): String?
    suspend fun getTemplateUpdates(template: String): Flow<String?>?

    suspend fun updateLocation(updateLocation: UpdateLocation)

    suspend fun getZones(): List<Entity>

    suspend fun isAppLocked(): Boolean
    suspend fun setAppActive(active: Boolean)

    suspend fun sessionTimeOut(value: Int)
    suspend fun getSessionTimeOut(): Int

    suspend fun setSessionExpireMillis(value: Long)

    suspend fun getHomeAssistantVersion(): String
    suspend fun isHomeAssistantVersionAtLeast(year: Int, month: Int, release: Int): Boolean

    suspend fun getConfig(): GetConfigResponse
    suspend fun getServices(): List<Action>?

    suspend fun getEntities(): List<Entity>?
    suspend fun getEntity(entityId: String): Entity?
    suspend fun getEntityUpdates(): Flow<Entity>?
    suspend fun getEntityUpdates(entityIds: List<String>): Flow<Entity>?

    suspend fun callAction(domain: String, action: String, actionData: Map<String, Any?>)

    /**
     * Creates or overwrites a persistent scene on the server through Home Assistant's scene config
     * endpoint. Unlike the runtime `scene.create` action, a scene saved this way survives a server
     * restart because Home Assistant stores it alongside its other UI-editable scenes.
     *
     * @param sceneId Unique identifier used as the scene's config resource path.
     * @param name Human-readable scene name shown in Home Assistant.
     * @param entities Map of entity id to the state to restore, where each value is either a bare
     *   state string or a map of "state" plus domain-specific attributes (for example brightness
     *   for a light).
     * @return `true` when the server accepted the scene, `false` when the request failed, for
     *   example when the server does not store scenes in a UI-editable location.
     */
    suspend fun saveScene(sceneId: String, name: String, entities: Map<String, Any?>): Boolean

    suspend fun scanTag(data: Map<String, String>)

    suspend fun fireEvent(eventType: String, eventData: Map<String, Any>)

    suspend fun registerSensor(sensorRegistration: SensorRegistration<Any>)
    suspend fun updateSensors(sensors: List<SensorRegistration<Any>>): Boolean

    suspend fun isTrusted(): Boolean

    suspend fun setTrusted(trusted: Boolean)

    suspend fun shouldNotifySecurityWarning(): Boolean

    suspend fun getAssistResponse(
        text: String,
        pipelineId: String? = null,
        conversationId: String? = null,
    ): Flow<AssistPipelineEvent>?

    suspend fun getLastUsedPipelineId(): String?

    suspend fun getLastUsedPipelineSttSupport(): Boolean

    suspend fun setLastUsedPipeline(pipelineId: String, supportsStt: Boolean)

    /** @return List of border agent IDs added to this device from the server */
    suspend fun getThreadBorderAgentIds(): List<String>

    /** Set the list of border agent IDs added to this device from the server */
    suspend fun setThreadBorderAgentIds(ids: List<String>)

    /** @return List of border agent IDs added to this device from a server that no longer exists */
    suspend fun getOrphanedThreadBorderAgentIds(): List<String>

    /** Clear the list of orphaned border agent IDs, to use after removing them from storage */
    suspend fun clearOrphanedThreadBorderAgentIds()

    suspend fun shouldAskNotificationPermission(): Boolean?

    suspend fun setAskNotificationPermission(shouldAsk: Boolean)
}

@AssistedFactory
internal interface IntegrationRepositoryFactory {
    fun create(serverId: Int): IntegrationRepositoryImpl
}

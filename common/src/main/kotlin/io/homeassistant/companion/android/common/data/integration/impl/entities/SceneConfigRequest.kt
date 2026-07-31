package io.homeassistant.companion.android.common.data.integration.impl.entities

import io.homeassistant.companion.android.common.util.MapAnySerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * Body for Home Assistant's scene config endpoint (POST /api/config/scene/config/<id>), which
 * creates or overwrites a persistent scene that survives a server restart.
 *
 * @property name Human-readable scene name shown in Home Assistant.
 * @property entities Map of entity id to the state to restore. Each value is either a bare state
 *   string (for example "on") or a map holding "state" plus domain-specific attributes such as
 *   brightness or color for a light.
 */
@Serializable
data class SceneConfigRequest(
    val name: String,
    @Serializable(with = MapAnySerializer::class)
    val entities: Map<String, @Polymorphic Any?>,
)

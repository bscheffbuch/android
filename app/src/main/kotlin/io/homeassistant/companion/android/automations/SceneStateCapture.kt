package io.homeassistant.companion.android.automations

import io.homeassistant.companion.android.common.data.integration.Entity

// Attributes that describe an entity (how it looks or what it can do) rather than a state that can
// be restored. They are dropped from the generic capture so a saved scene stays lean and never asks
// Home Assistant to write a read-only value back onto an entity.
private val NON_STATE_ATTRIBUTES = setOf(
    "friendly_name",
    "icon",
    "entity_picture",
    "supported_features",
    "supported_color_modes",
    "color_mode",
    "device_class",
    "assumed_state",
    "attribution",
    "restored",
    "editable",
    "id",
)

/**
 * Builds the `entities` map for a Home Assistant scene from the live state of [entities].
 *
 * Each entity becomes an entry mapping its id to `{"state": ..., <attributes>}`. Lights are captured
 * with intent — brightness plus the single color attribute matching their current `color_mode` — so
 * a restored scene never fights itself by sending, for example, both a color temperature and an RGB
 * value. Every other domain keeps its current state plus its settable attributes, letting Home
 * Assistant's own per-domain restore logic pick what it needs.
 *
 * The result is safe to hand to [io.homeassistant.companion.android.common.data.integration.IntegrationRepository.saveScene].
 */
internal fun buildSceneEntities(entities: List<Entity>): Map<String, Any> =
    entities.associate { entity -> entity.entityId to sceneStateOf(entity) }

private fun sceneStateOf(entity: Entity): Map<String, Any> = when (entity.domain) {
    "light" -> lightSceneState(entity)
    else -> buildMap {
        put("state", entity.state)
        putAll(settableAttributes(entity.attributes))
    }
}

private fun lightSceneState(entity: Entity): Map<String, Any> {
    if (entity.state != "on") return mapOf("state" to entity.state)
    val attributes = entity.attributes
    return buildMap {
        put("state", "on")
        (attributes["brightness"] as? Number)?.let { put("brightness", it.toInt()) }
        // Capture only the color attribute the light is actually using so the restore is unambiguous.
        when (attributes["color_mode"] as? String) {
            "color_temp" -> {
                val mireds = attributes["color_temp"] as? Number
                val kelvin = attributes["color_temp_kelvin"] as? Number
                when {
                    mireds != null -> put("color_temp", mireds.toInt())
                    kelvin != null -> put("color_temp_kelvin", kelvin.toInt())
                }
            }

            "hs" -> (attributes["hs_color"] as? List<*>)?.filterNotNull()?.takeIf { it.isNotEmpty() }
                ?.let { put("hs_color", it) }

            "rgb", "rgbw", "rgbww" ->
                (attributes["rgb_color"] as? List<*>)?.filterNotNull()?.takeIf { it.isNotEmpty() }
                    ?.let { put("rgb_color", it) }

            "xy" -> (attributes["xy_color"] as? List<*>)?.filterNotNull()?.takeIf { it.isNotEmpty() }
                ?.let { put("xy_color", it) }
        }
    }
}

private fun settableAttributes(attributes: Map<String, Any?>): Map<String, Any> = attributes
    .filterKeys { it !in NON_STATE_ATTRIBUTES }
    .mapNotNull { (key, value) -> value?.let { key to it } }
    .toMap()

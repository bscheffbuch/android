package io.homeassistant.companion.android.automations

import io.homeassistant.companion.android.common.data.integration.Entity
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val FIXED_TIME: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

private fun entityOf(entityId: String, state: String, attributes: Map<String, Any?> = emptyMap()) = Entity(
    entityId = entityId,
    state = state,
    attributes = attributes,
    lastChanged = FIXED_TIME,
    lastUpdated = FIXED_TIME,
)

class SceneStateCaptureTest {

    @Test
    fun `Given a light on in color_temp mode when capturing then keeps brightness and only the color temperature`() {
        val entities = listOf(
            entityOf(
                entityId = "light.kitchen",
                state = "on",
                attributes = mapOf(
                    "friendly_name" to "Kitchen",
                    "supported_features" to 44,
                    "color_mode" to "color_temp",
                    "brightness" to 254,
                    "color_temp_kelvin" to 3000,
                    "rgb_color" to listOf(255, 197, 143),
                    "hs_color" to listOf(28.0, 43.0),
                ),
            ),
        )

        val captured = buildSceneEntities(entities)["light.kitchen"] as Map<*, *>

        assertEquals("on", captured["state"])
        assertEquals(254, captured["brightness"])
        assertEquals(3000, captured["color_temp_kelvin"])
        // The competing color attributes for other modes must not leak in.
        assertFalse(captured.containsKey("rgb_color"))
        assertFalse(captured.containsKey("hs_color"))
        // Descriptive attributes are never written into a scene.
        assertFalse(captured.containsKey("friendly_name"))
        assertFalse(captured.containsKey("supported_features"))
    }

    @Test
    fun `Given a light on in rgb mode when capturing then keeps only the rgb color`() {
        val entities = listOf(
            entityOf(
                entityId = "light.strip",
                state = "on",
                attributes = mapOf(
                    "color_mode" to "rgb",
                    "brightness" to 120,
                    "rgb_color" to listOf(10, 20, 30),
                    "color_temp_kelvin" to 3000,
                ),
            ),
        )

        val captured = buildSceneEntities(entities)["light.strip"] as Map<*, *>

        assertEquals(listOf(10, 20, 30), captured["rgb_color"])
        assertEquals(120, captured["brightness"])
        assertFalse(captured.containsKey("color_temp_kelvin"))
    }

    @Test
    fun `Given a light that is off when capturing then only the off state is kept`() {
        val entities = listOf(
            entityOf(
                entityId = "light.bedroom",
                state = "off",
                attributes = mapOf("friendly_name" to "Bedroom", "supported_color_modes" to listOf("brightness")),
            ),
        )

        val captured = buildSceneEntities(entities)["light.bedroom"] as Map<*, *>

        assertEquals(mapOf("state" to "off"), captured)
    }

    @Test
    fun `Given a switch when capturing then keeps its state`() {
        val entities = listOf(
            entityOf(
                entityId = "switch.fan",
                state = "on",
                attributes = mapOf("friendly_name" to "Fan", "device_class" to "outlet"),
            ),
        )

        val captured = buildSceneEntities(entities)["switch.fan"] as Map<*, *>

        assertEquals("on", captured["state"])
        assertFalse(captured.containsKey("friendly_name"))
        assertFalse(captured.containsKey("device_class"))
    }

    @Test
    fun `Given a climate entity when capturing then keeps its settable attributes and drops null values`() {
        val entities = listOf(
            entityOf(
                entityId = "climate.living_room",
                state = "heat",
                attributes = mapOf(
                    "friendly_name" to "Living Room",
                    "temperature" to 21.5,
                    "hvac_action" to null,
                ),
            ),
        )

        val captured = buildSceneEntities(entities)["climate.living_room"] as Map<*, *>

        assertEquals("heat", captured["state"])
        assertEquals(21.5, captured["temperature"])
        assertFalse(captured.containsKey("hvac_action"))
        assertFalse(captured.containsKey("friendly_name"))
    }

    @Test
    fun `Given multiple entities when capturing then every entity is represented`() {
        val entities = listOf(
            entityOf("light.a", "on", mapOf("color_mode" to "onoff")),
            entityOf("switch.b", "off"),
        )

        val captured = buildSceneEntities(entities)

        assertEquals(setOf("light.a", "switch.b"), captured.keys)
        assertTrue((captured["light.a"] as Map<*, *>).containsKey("state"))
    }
}

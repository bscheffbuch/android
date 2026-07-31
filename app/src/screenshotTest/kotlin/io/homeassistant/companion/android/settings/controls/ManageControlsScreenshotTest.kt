package io.homeassistant.companion.android.settings.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.integration.ControlsAuthRequiredSetting
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.settings.controls.views.ManageControlsView
import java.time.LocalDateTime

private val FIXED_DATE_TIME: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

private fun previewEntity(entityId: String, friendlyName: String) = Entity(
    entityId = entityId,
    state = "on",
    attributes = mapOf("friendly_name" to friendlyName),
    lastChanged = FIXED_DATE_TIME,
    lastUpdated = FIXED_DATE_TIME,
)

private fun createServer(id: Int, name: String) = Server(
    id = id,
    _name = name,
    connection = ServerConnectionInfo(externalUrl = "https://$name.example.com"),
    session = ServerSessionInfo(),
    user = ServerUserInfo(id = null, name = null, isOwner = false, isAdmin = false),
)

private val previewServer = createServer(id = 0, name = "Home")

class ManageControlsScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Built-in mode with entities available`() {
        HAThemeForPreview {
            ManageControlsView(
                panelEnabled = false,
                authSetting = ControlsAuthRequiredSetting.SELECTION,
                authRequiredList = listOf("0.switch.fan"),
                entitiesLoaded = true,
                entitiesList = mapOf(
                    0 to listOf(
                        previewEntity("light.living_room", "Living Room"),
                        previewEntity("switch.fan", "Fan"),
                    ),
                ),
                panelSetting = null,
                serversList = listOf(previewServer),
                structureEnabled = false,
                defaultServer = 0,
                onSetPanelEnabled = {},
                onSelectAll = {},
                onSelectNone = {},
                onSelectEntity = { _, _ -> },
                onSetPanelSetting = { _, _ -> },
                onSetStructureEnabled = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Built-in mode with multiple servers configured`() {
        HAThemeForPreview {
            ManageControlsView(
                panelEnabled = false,
                authSetting = ControlsAuthRequiredSetting.ALL,
                authRequiredList = emptyList(),
                entitiesLoaded = true,
                entitiesList = mapOf(
                    0 to listOf(previewEntity("light.living_room", "Living Room")),
                    1 to listOf(previewEntity("light.bedroom", "Bedroom")),
                ),
                panelSetting = null,
                serversList = listOf(previewServer, createServer(id = 1, name = "Holiday Home")),
                structureEnabled = true,
                defaultServer = 0,
                onSetPanelEnabled = {},
                onSelectAll = {},
                onSelectNone = {},
                onSelectEntity = { _, _ -> },
                onSetPanelSetting = { _, _ -> },
                onSetStructureEnabled = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Built-in mode with no entities available`() {
        HAThemeForPreview {
            ManageControlsView(
                panelEnabled = false,
                authSetting = ControlsAuthRequiredSetting.ALL,
                authRequiredList = emptyList(),
                entitiesLoaded = true,
                entitiesList = emptyMap(),
                panelSetting = null,
                serversList = listOf(previewServer),
                structureEnabled = false,
                defaultServer = 0,
                onSetPanelEnabled = {},
                onSelectAll = {},
                onSelectNone = {},
                onSelectEntity = { _, _ -> },
                onSetPanelSetting = { _, _ -> },
                onSetStructureEnabled = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Built-in mode while entities are still loading`() {
        HAThemeForPreview {
            ManageControlsView(
                panelEnabled = false,
                authSetting = ControlsAuthRequiredSetting.ALL,
                authRequiredList = emptyList(),
                entitiesLoaded = false,
                entitiesList = emptyMap(),
                panelSetting = null,
                serversList = listOf(previewServer),
                structureEnabled = false,
                defaultServer = 0,
                onSetPanelEnabled = {},
                onSelectAll = {},
                onSelectNone = {},
                onSelectEntity = { _, _ -> },
                onSetPanelSetting = { _, _ -> },
                onSetStructureEnabled = {},
            )
        }
    }
}

package io.homeassistant.companion.android.settings.controls.views

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.integration.ControlsAuthRequiredSetting
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ManageControlsViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private var panelEnabledResult: Boolean? = null
    private var selectAllInvoked = false
    private var selectNoneInvoked = false
    private var selectedEntity: Pair<String, Int>? = null
    private var panelSettingResult: Pair<String, Int>? = null
    private var structureEnabledResult: Boolean? = null

    private val entityOne = createEntity(entityId = "light.living_room", friendlyName = "Living Room")
    private val entityTwo = createEntity(entityId = "switch.fan", friendlyName = "Fan")
    private val serverOne = createServer(id = 0, name = "Home")
    private val serverTwo = createServer(id = 1, name = "Holiday Home")

    private fun setContent(
        panelEnabled: Boolean = false,
        authSetting: ControlsAuthRequiredSetting = ControlsAuthRequiredSetting.ALL,
        authRequiredList: List<String> = emptyList(),
        entitiesLoaded: Boolean = true,
        entitiesList: Map<Int, List<Entity>> = mapOf(0 to listOf(entityOne, entityTwo)),
        panelSetting: Pair<String?, Int>? = null,
        serversList: List<Server> = listOf(serverOne),
        structureEnabled: Boolean = false,
        defaultServer: Int = 0,
    ) {
        composeTestRule.setContent {
            var currentPanelEnabled by remember { mutableStateOf(panelEnabled) }
            HAThemeForPreview {
                ManageControlsView(
                    panelEnabled = currentPanelEnabled,
                    authSetting = authSetting,
                    authRequiredList = authRequiredList,
                    entitiesLoaded = entitiesLoaded,
                    entitiesList = entitiesList,
                    panelSetting = panelSetting,
                    serversList = serversList,
                    structureEnabled = structureEnabled,
                    defaultServer = defaultServer,
                    onSetPanelEnabled = {
                        currentPanelEnabled = it
                        panelEnabledResult = it
                    },
                    onSelectAll = { selectAllInvoked = true },
                    onSelectNone = { selectNoneInvoked = true },
                    onSelectEntity = { entityId, serverId -> selectedEntity = entityId to serverId },
                    onSetPanelSetting = { path, serverId -> panelSettingResult = path to serverId },
                    onSetStructureEnabled = { structureEnabledResult = it },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given built-in mode selected when composed then choose all and choose none buttons are shown`() {
        setContent(panelEnabled = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_all))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_none))
            .assertExists()
    }

    @Test
    fun `Given panel mode selected when composed then dashboard path field is shown`() {
        setContent(panelEnabled = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.lovelace_view_dashboard))
            .assertExists()
    }

    @Test
    fun `Given built-in mode when choose all is clicked then onSelectAll is invoked`() {
        setContent(panelEnabled = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_all))
            .performScrollTo()
            .performClick()

        assertEquals(true, selectAllInvoked)
    }

    @Test
    fun `Given built-in mode when choose none is clicked then onSelectNone is invoked`() {
        setContent(panelEnabled = false, authSetting = ControlsAuthRequiredSetting.SELECTION)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_none))
            .performScrollTo()
            .performClick()

        assertEquals(true, selectNoneInvoked)
    }

    @Test
    fun `Given multiple servers in built-in mode when composed then server dropdown is shown`() {
        setContent(panelEnabled = false, serversList = listOf(serverOne, serverTwo))

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.server_select))
            .assertExists()
    }

    @Test
    fun `Given entities loaded when an entity row is clicked then onSelectEntity is invoked`() {
        setContent(defaultServer = 0)

        composeTestRule.onNodeWithText("Living Room")
            .performScrollTo()
            .performClick()

        assertEquals("light.living_room" to 0, selectedEntity)
    }

    @Test
    fun `Given entities not loaded when composed then choose all button is not shown`() {
        setContent(entitiesLoaded = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_all))
            .assertDoesNotExist()
    }

    @Test
    fun `Given no entities available when composed then the empty message is shown`() {
        setContent(entitiesLoaded = true, entitiesList = emptyMap())

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_empty))
            .assertExists()
    }

    @Test
    fun `Given multiple servers when the structure switch is clicked then onSetStructureEnabled is invoked`() {
        setContent(serversList = listOf(serverOne, serverTwo), structureEnabled = false)

        composeTestRule.onNode(isToggleable())
            .performScrollTo()
            .performClick()

        assertEquals(true, structureEnabledResult)
    }

    @Test
    fun `Given built-in mode switched to panel mode when composed then the warning banner is shown`() {
        setContent(panelEnabled = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.lovelace))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_alert))
            .assertExists()
    }

    @Test
    fun `Given panel mode already enabled at composition when composed then the warning banner is not shown`() {
        setContent(panelEnabled = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_alert))
            .assertDoesNotExist()
    }

    @Test
    fun `Given panel mode with no existing panel setting when save is clicked then onSetPanelSetting is invoked`() {
        setContent(panelEnabled = true, panelSetting = null, defaultServer = 0)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.save))
            .performScrollTo()
            .performClick()

        assertEquals("" to 0, panelSettingResult)
    }

    @Test
    fun `Given panel mode with an unchanged panel setting when composed then the save button is disabled`() {
        setContent(panelEnabled = true, panelSetting = "existing/path" to 0, defaultServer = 0)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.save))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun `Given panel mode when the dashboard path is changed then the save button becomes enabled`() {
        setContent(panelEnabled = true, panelSetting = "existing/path" to 0, defaultServer = 0)
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.save))
            .performScrollTo()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("existing/path")
            .performScrollTo()
            .performTextReplacement("changed/path")

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.save))
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun `Given built-in mode when the panel mode button is clicked then onSetPanelEnabled is invoked with true`() {
        setContent(panelEnabled = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.lovelace))
            .performScrollTo()
            .performClick()

        assertEquals(true, panelEnabledResult)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `Given a system version below panel support when composed then the panel mode toggle is not shown`() {
        setContent(panelEnabled = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.lovelace))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.controls_setting_choose_all))
            .assertExists()
    }

    private fun createEntity(entityId: String, friendlyName: String) = Entity(
        entityId = entityId,
        state = "on",
        attributes = mapOf("friendly_name" to friendlyName),
        lastChanged = FIXED_DATE_TIME,
        lastUpdated = FIXED_DATE_TIME,
    )

    private fun createServer(id: Int, name: String) = Server(
        id = id,
        _name = name,
        connection = ServerConnectionInfo(externalUrl = "https://server-$id.example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(id = null, name = null, isOwner = false, isAdmin = false),
    )

    private companion object {
        val FIXED_DATE_TIME: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
    }
}

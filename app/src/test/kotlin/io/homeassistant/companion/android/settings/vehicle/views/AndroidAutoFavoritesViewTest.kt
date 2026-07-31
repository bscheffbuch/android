package io.homeassistant.companion.android.settings.vehicle.views

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.prefs.AutoFavorite
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.data.websocket.WebSocketRepository
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.settings.vehicle.ManageAndroidAutoViewModel
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val FAKE_SERVER_ID = 0

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class AndroidAutoFavoritesViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private val serverManager: ServerManager = mockk(relaxed = true)
    private val prefsRepository: PrefsRepository = mockk(relaxed = true)
    private val integrationRepository: IntegrationRepository = mockk(relaxed = true)
    private val webSocketRepository: WebSocketRepository = mockk(relaxed = true)

    private lateinit var viewModel: ManageAndroidAutoViewModel

    private val livingRoomLight = Entity(
        entityId = "light.living_room",
        state = "on",
        attributes = mapOf("friendly_name" to "Living Room"),
        lastChanged = FIXED_DATE_TIME,
        lastUpdated = FIXED_DATE_TIME,
    )

    private fun setup(favorites: List<AutoFavorite> = emptyList(), serversList: List<Server> = listOf(createServer(FAKE_SERVER_ID))) {
        coEvery { serverManager.servers() } returns serversList
        coEvery { serverManager.getServer() } returns serversList.first()
        coEvery { serverManager.integrationRepository(any()) } returns integrationRepository
        coEvery { serverManager.webSocketRepository(any()) } returns webSocketRepository
        coEvery { integrationRepository.getEntities() } returns listOf(livingRoomLight)
        coEvery { prefsRepository.getAutoFavorites() } returns favorites

        viewModel = ManageAndroidAutoViewModel(
            serverManager = serverManager,
            prefsRepository = prefsRepository,
            application = ApplicationProvider.getApplicationContext<Application>(),
        )
    }

    private fun setContent(serversList: List<Server> = listOf(createServer(FAKE_SERVER_ID))) {
        composeTestRule.setContent {
            HAThemeForPreview {
                AndroidAutoFavoritesSettings(
                    androidAutoViewModel = viewModel,
                    serversList = serversList,
                    defaultServer = FAKE_SERVER_ID,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given a single server when composed then the intro text is shown and the server dropdown is not shown`() {
        setup()
        setContent()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.aa_set_favorites))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.server_select))
            .assertDoesNotExist()
    }

    @Test
    fun `Given multiple servers when composed then the server dropdown is shown`() {
        val servers = listOf(createServer(0), createServer(1))
        setup(serversList = servers)
        setContent(serversList = servers)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.server_select))
            .assertExists()
    }

    @Test
    fun `Given a favorite entity when composed then its row shows the entity name and id`() {
        setup(favorites = listOf(AutoFavorite(FAKE_SERVER_ID, livingRoomLight.entityId)))
        setContent()

        composeTestRule.onNodeWithText("Living Room").assertExists()
        composeTestRule.onNodeWithText(livingRoomLight.entityId).assertExists()
    }

    @Test
    fun `Given a favorite entity when its remove icon is clicked then it is removed from the favorites list`() {
        setup(favorites = listOf(AutoFavorite(FAKE_SERVER_ID, livingRoomLight.entityId)))
        setContent()

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.delete))
            .performClick()

        assertEquals(emptyList<AutoFavorite>(), viewModel.favoritesList)
    }

    private fun createServer(id: Int) = Server(
        id = id,
        _name = "Home $id",
        connection = ServerConnectionInfo(externalUrl = "https://server-$id.example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(id = null, name = null, isOwner = false, isAdmin = false),
    )

    private companion object {
        val FIXED_DATE_TIME: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
    }
}

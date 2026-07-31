package io.homeassistant.companion.android.settings.server

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.database.server.ServerConnectionInfo
import io.homeassistant.companion.android.database.server.ServerSessionInfo
import io.homeassistant.companion.android.database.server.ServerUserInfo
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ServerChooserViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private val servers = listOf(createServer(0, "Home"), createServer(1, "Vacation House"))

    @Test
    fun `Given a list of servers when composed then all server names are shown`() {
        composeTestRule.setContent {
            ServerChooserView(servers = servers, onServerSelected = {})
        }

        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Vacation House").assertExists()
    }

    @Test
    fun `Given a list of servers when a server row is clicked then its id is reported`() {
        var selectedServerId: Int? = null
        composeTestRule.setContent {
            ServerChooserView(servers = servers, onServerSelected = { selectedServerId = it })
        }

        composeTestRule.onNodeWithText("Vacation House").performClick()

        assertEquals(1, selectedServerId)
    }

    private fun createServer(id: Int, name: String) = Server(
        id = id,
        _name = name,
        connection = ServerConnectionInfo(externalUrl = "https://server-$id.example.com"),
        session = ServerSessionInfo(),
        user = ServerUserInfo(id = null, name = null, isOwner = false, isAdmin = false),
    )
}

package io.homeassistant.companion.android.settings.shortcuts.views

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsViewModel
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val FAKE_SERVER_ID = 1

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ManageShortcutsViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private val serverManager: ServerManager = mockk(relaxed = true)
    private lateinit var viewModel: ManageShortcutsViewModel

    @Before
    fun setup() {
        val fakeServer = mockk<Server>(relaxed = true) {
            every { id } returns FAKE_SERVER_ID
            every { friendlyName } returns "Home"
        }
        coEvery { serverManager.getServer() } returns fakeServer
        coEvery { serverManager.servers() } returns listOf(fakeServer)

        viewModel = ManageShortcutsViewModel(
            serverManager = serverManager,
            application = ApplicationProvider.getApplicationContext<Application>(),
        )
    }

    private fun setContent() {
        composeTestRule.setContent {
            HAThemeForPreview {
                ManageShortcutsView(viewModel = viewModel, showIconDialog = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given a blank shortcut 1 label when composed then the add shortcut button is disabled`() {
        setContent()

        composeTestRule.onAllNodesWithText(composeTestRule.stringResource(commonR.string.add_shortcut))[0]
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun `Given the shortcut 1 label field when text is entered then viewModel shortcut label is updated`() {
        setContent()

        val labelFieldLabel = "${composeTestRule.stringResource(commonR.string.shortcut)} 1 " +
            composeTestRule.stringResource(commonR.string.label)
        composeTestRule.onNodeWithText(labelFieldLabel)
            .performScrollTo()
            .performTextInput("My Shortcut")

        assertEquals("My Shortcut", viewModel.shortcuts[0].label.value)
    }

    @Test
    fun `Given the entity type radio option when clicked then viewModel shortcut 1 type becomes entityId`() {
        setContent()

        composeTestRule.onAllNodesWithText(composeTestRule.stringResource(commonR.string.entity))[0]
            .performScrollTo()
            .performClick()

        assertEquals("entityId", viewModel.shortcuts[0].type.value)
    }
}

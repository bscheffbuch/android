package io.homeassistant.companion.android.settings.qs.views

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.qs.TileDao
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.settings.qs.ManageTilesViewModel
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class ManageTilesViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private val serverManager: ServerManager = mockk(relaxed = true)
    private val tileDao: TileDao = mockk(relaxed = true)
    private lateinit var viewModel: ManageTilesViewModel

    @Before
    fun setup() {
        val fakeServer = mockk<Server>(relaxed = true) { every { id } returns FAKE_SERVER_ID }
        coEvery { serverManager.servers() } returns listOf(fakeServer)
        coEvery { tileDao.get(any()) } returns null
        coEvery { tileDao.getAll() } returns emptyList()

        viewModel = ManageTilesViewModel(
            state = SavedStateHandle(),
            serverManager = serverManager,
            tileDao = tileDao,
            application = ApplicationProvider.getApplicationContext<Application>(),
        )
    }

    private fun setContent() {
        composeTestRule.setContent {
            HAThemeForPreview {
                ManageTilesView(viewModel = viewModel, onShowIconDialog = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given a blank tile label when composed then the submit button is disabled`() {
        setContent()

        val buttonText = composeTestRule.stringResource(viewModel.submitButtonLabel)
        composeTestRule.onNodeWithText(buttonText).assertIsNotEnabled()
    }

    @Test
    fun `Given the tile label field when text is entered then viewModel tileLabel is updated`() {
        setContent()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.tile_label))
            .performTextInput("My Tile")

        assertEquals("My Tile", viewModel.tileLabel)
    }

    @Test
    fun `Given the vibrate switch unchecked when clicked then viewModel selectedShouldVibrate becomes true`() {
        setContent()

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.tile_vibrate))
            .performScrollTo()
            .performClick()

        assertTrue(viewModel.selectedShouldVibrate)
    }

    @Test
    fun `Given the auth required switch unchecked when clicked then viewModel tileAuthRequired becomes true`() {
        setContent()

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.tile_auth_required))
            .performScrollTo()
            .performClick()

        assertTrue(viewModel.tileAuthRequired)
    }
}

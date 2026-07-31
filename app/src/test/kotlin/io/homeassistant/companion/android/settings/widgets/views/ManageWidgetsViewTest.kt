package io.homeassistant.companion.android.settings.widgets.views

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.database.widget.ButtonWidgetDao
import io.homeassistant.companion.android.database.widget.ButtonWidgetEntity
import io.homeassistant.companion.android.database.widget.CameraWidgetDao
import io.homeassistant.companion.android.database.widget.MediaPlayerControlsWidgetDao
import io.homeassistant.companion.android.database.widget.StaticWidgetDao
import io.homeassistant.companion.android.database.widget.TemplateWidgetDao
import io.homeassistant.companion.android.database.widget.TodoWidgetDao
import io.homeassistant.companion.android.settings.widgets.ManageWidgetsViewModel
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import io.homeassistant.companion.android.widgets.button.ButtonWidgetConfigureActivity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val FAKE_WIDGET_ID = 42

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class ManageWidgetsViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private val buttonWidgetDao: ButtonWidgetDao = mockk(relaxed = true)
    private val cameraWidgetDao: CameraWidgetDao = mockk(relaxed = true)
    private val staticWidgetDao: StaticWidgetDao = mockk(relaxed = true)
    private val todoWidgetDao: TodoWidgetDao = mockk(relaxed = true)
    private val mediaPlayerControlsWidgetDao: MediaPlayerControlsWidgetDao = mockk(relaxed = true)
    private val templateWidgetDao: TemplateWidgetDao = mockk(relaxed = true)

    @Before
    fun setup() {
        every { cameraWidgetDao.getAllFlow() } returns flowOf(emptyList())
        every { staticWidgetDao.getAllFlow() } returns flowOf(emptyList())
        every { todoWidgetDao.getAllFlow() } returns flowOf(emptyList())
        every { mediaPlayerControlsWidgetDao.getAllFlow() } returns flowOf(emptyList())
        every { templateWidgetDao.getAllFlow() } returns flowOf(emptyList())
    }

    private fun setContent(buttonWidgets: List<ButtonWidgetEntity> = emptyList()) {
        every { buttonWidgetDao.getAllFlow() } returns flowOf(buttonWidgets)

        val viewModel = ManageWidgetsViewModel(
            buttonWidgetDao = buttonWidgetDao,
            cameraWidgetDao = cameraWidgetDao,
            staticWidgetDao = staticWidgetDao,
            todoWidgetDao = todoWidgetDao,
            mediaPlayerControlsWidgetDao = mediaPlayerControlsWidgetDao,
            templateWidgetDao = templateWidgetDao,
            application = ApplicationProvider.getApplicationContext<Application>(),
        )

        composeTestRule.setContent {
            HAThemeForPreview {
                ManageWidgetsView(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given no configured widgets when composed then the empty state is shown`() {
        setContent()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.no_widgets))
            .assertExists()
    }

    @Test
    fun `Given a configured button widget when composed then its row and section header are shown`() {
        val widget = createButtonWidget(id = FAKE_WIDGET_ID, label = "Kitchen Light")
        setContent(buttonWidgets = listOf(widget))

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.button_widgets))
            .assertExists()
        composeTestRule.onNodeWithText("Kitchen Light")
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.no_widgets))
            .assertDoesNotExist()
    }

    @Test
    fun `Given a configured button widget when its row is clicked then the button configure activity is launched with its widget id`() {
        val widget = createButtonWidget(id = FAKE_WIDGET_ID, label = "Kitchen Light")
        setContent(buttonWidgets = listOf(widget))

        composeTestRule.onNodeWithText("Kitchen Light")
            .performScrollTo()
            .performClick()

        val startedIntent = shadowOf(composeTestRule.activity).nextStartedActivity
        assertEquals(ButtonWidgetConfigureActivity::class.java.name, startedIntent.component?.className)
        assertEquals(FAKE_WIDGET_ID, startedIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }

    private fun createButtonWidget(id: Int, label: String) = ButtonWidgetEntity(
        id = id,
        serverId = 0,
        iconName = "",
        domain = "light",
        service = "turn_on",
        serviceData = "",
        label = label,
        requireAuthentication = false,
    )
}

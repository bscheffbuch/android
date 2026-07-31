package io.homeassistant.companion.android.settings.developer.location.views

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.database.location.LocationHistoryItem
import io.homeassistant.companion.android.database.location.LocationHistoryItemResult
import io.homeassistant.companion.android.database.location.LocationHistoryItemTrigger
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val FAKE_HISTORY_ITEM = LocationHistoryItem(
    id = 1,
    created = 0L,
    trigger = LocationHistoryItemTrigger.SINGLE_ACCURATE_LOCATION,
    result = LocationHistoryItemResult.SENT,
    latitude = 1.23,
    longitude = 4.56,
    locationName = null,
    accuracy = 10,
    data = null,
    serverId = null,
)

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class LocationTrackingViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun `Given history disabled when composed then the history off empty state is shown`() {
        composeTestRule.setContent {
            HAThemeForPreview {
                LocationTrackingView(
                    useHistory = false,
                    onSetHistory = {},
                    history = flowOf(PagingData.empty()),
                    serversList = emptyList(),
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.location_history_off_title))
            .assertExists()
    }

    @Test
    fun `Given the history toggle when clicked then onSetHistory is invoked with the toggled value`() {
        var toggledTo: Boolean? = null
        composeTestRule.setContent {
            HAThemeForPreview {
                LocationTrackingView(
                    useHistory = false,
                    onSetHistory = { toggledTo = it },
                    history = flowOf(PagingData.empty()),
                    serversList = emptyList(),
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.location_history_use))
            .performClick()

        assertEquals(true, toggledTo)
    }

    @Test
    fun `Given a history item when clicked then the location and accuracy details are revealed`() {
        composeTestRule.setContent {
            HAThemeForPreview {
                LocationTrackingHistoryRow(item = FAKE_HISTORY_ITEM, servers = emptyList())
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.accuracy)).assertDoesNotExist()

        val rowSummary = "${composeTestRule.stringResource(commonR.string.basic_sensor_name_location_accurate)} • " +
            composeTestRule.stringResource(commonR.string.location_history_sent)
        composeTestRule.onNodeWithText(rowSummary).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.accuracy)).assertExists()
    }
}

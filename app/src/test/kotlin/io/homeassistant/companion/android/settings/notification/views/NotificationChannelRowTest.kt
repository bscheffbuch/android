package io.homeassistant.companion.android.settings.notification.views

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class NotificationChannelRowTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun `Given deletable channel then delete icon is displayed`() {
        composeTestRule.setContent {
            HAThemeForPreview {
                NotificationChannelRow(
                    name = "Server-created channel",
                    canDelete = true,
                    onEditClicked = {},
                    onDeleteClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.delete_channel)).assertIsDisplayed()
    }

    @Test
    fun `Given non deletable channel then delete icon is not displayed`() {
        composeTestRule.setContent {
            HAThemeForPreview {
                NotificationChannelRow(
                    name = "App-created channel",
                    canDelete = false,
                    onEditClicked = {},
                    onDeleteClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.delete_channel)).assertDoesNotExist()
    }

    @Test
    fun `Given row when edit icon clicked then onEditClicked is called`() {
        var editClicked = false

        composeTestRule.setContent {
            HAThemeForPreview {
                NotificationChannelRow(
                    name = "Server-created channel",
                    canDelete = true,
                    onEditClicked = { editClicked = true },
                    onDeleteClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.edit_channel)).performClick()

        assertTrue(editClicked)
    }

    @Test
    fun `Given deletable row when delete icon clicked then onDeleteClicked is called`() {
        var deleteClicked = false

        composeTestRule.setContent {
            HAThemeForPreview {
                NotificationChannelRow(
                    name = "Server-created channel",
                    canDelete = true,
                    onEditClicked = {},
                    onDeleteClicked = { deleteClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.delete_channel)).performClick()

        assertTrue(deleteClicked)
    }
}

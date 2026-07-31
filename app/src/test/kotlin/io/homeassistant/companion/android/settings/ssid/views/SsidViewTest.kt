package io.homeassistant.companion.android.settings.ssid.views

import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.homeassistant.companion.android.HiltComponentActivity
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.testing.unit.ConsoleLogRule
import io.homeassistant.companion.android.testing.unit.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
class SsidViewTest {

    @get:Rule(order = 0)
    var consoleLog = ConsoleLogRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    private var addedSsid: String? = null
    private var removedSsid: String? = null
    private var permissionRequested = false
    private var ethernetResult: Boolean? = null
    private var vpnResult: Boolean? = null
    private var prioritizeResult: Boolean? = null

    private fun setContent(
        wifiSsids: List<String> = emptyList(),
        canReadWifi: Boolean = true,
        ethernet: Boolean? = false,
        vpn: Boolean? = false,
        prioritizeInternal: Boolean = false,
        usingWifi: Boolean = false,
        activeSsid: String? = null,
        activeBssid: String? = null,
        onAddWifiSsid: (String) -> Boolean = {
            addedSsid = it
            true
        },
    ) {
        composeTestRule.setContent {
            HAThemeForPreview {
                SsidView(
                    wifiSsids = wifiSsids,
                    canReadWifi = canReadWifi,
                    ethernet = ethernet,
                    vpn = vpn,
                    prioritizeInternal = prioritizeInternal,
                    usingWifi = usingWifi,
                    activeSsid = activeSsid,
                    activeBssid = activeBssid,
                    onAddWifiSsid = onAddWifiSsid,
                    onRemoveWifiSsid = { removedSsid = it },
                    onRequestPermission = { permissionRequested = true },
                    onSetEthernet = { ethernetResult = it },
                    onSetVpn = { vpnResult = it },
                    onSetPrioritize = { prioritizeResult = it },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Given no location permission when composed then the permission warning is shown instead of the input`() {
        setContent(canReadWifi = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.manage_ssids_permission))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.manage_ssids_input))
            .assertDoesNotExist()
    }

    @Test
    fun `Given no location permission when the allow action is clicked then onRequestPermission is invoked`() {
        setContent(canReadWifi = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.allow))
            .performScrollTo()
            .performClick()

        assertEquals(true, permissionRequested)
    }

    @Test
    fun `Given location permission when a new SSID is typed and submitted then onAddWifiSsid is invoked`() {
        setContent(canReadWifi = true)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.manage_ssids_input))
            .performScrollTo()
            .performTextInput("my-home-network")
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.add_ssid))
            .performScrollTo()
            .performClick()

        assertEquals("my-home-network", addedSsid)
    }

    @Test
    fun `Given an active SSID not yet saved when composed then the suggestion chip is shown`() {
        setContent(activeSsid = "new-network", wifiSsids = emptyList())

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(commonR.string.add_ssid_name_suggestion, "new-network"),
        ).assertExists()
    }

    @Test
    fun `Given an active SSID already saved when composed then no suggestion chip is shown`() {
        setContent(activeSsid = "existing-network", wifiSsids = listOf("existing-network"))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(commonR.string.add_ssid_name_suggestion, "existing-network"),
        ).assertDoesNotExist()
    }

    @Test
    fun `Given a suggestion chip when clicked then onAddWifiSsid is invoked with the active SSID`() {
        setContent(activeSsid = "new-network", wifiSsids = emptyList())

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(commonR.string.add_ssid_name_suggestion, "new-network"),
        ).performScrollTo().performClick()

        assertEquals("new-network", addedSsid)
    }

    @Test
    fun `Given saved SSIDs when the remove icon is clicked then onRemoveWifiSsid is invoked`() {
        setContent(wifiSsids = listOf("home-network"))

        composeTestRule.onNodeWithContentDescription(composeTestRule.stringResource(commonR.string.remove_ssid))
            .performScrollTo()
            .performClick()

        assertEquals("home-network", removedSsid)
    }

    @Test
    fun `Given the VPN switch when toggled then onSetVpn is invoked`() {
        setContent(vpn = false, ethernet = false)

        composeTestRule.onAllNodes(isToggleable())[0]
            .performScrollTo()
            .performClick()

        assertEquals(true, vpnResult)
    }

    @Test
    fun `Given the Ethernet switch when toggled then onSetEthernet is invoked`() {
        setContent(vpn = false, ethernet = false)

        composeTestRule.onAllNodes(isToggleable())[1]
            .performScrollTo()
            .performClick()

        assertEquals(true, ethernetResult)
    }

    @Test
    fun `Given no SSIDs Ethernet or VPN configured when composed then no security warning is shown`() {
        setContent(wifiSsids = emptyList(), ethernet = false, vpn = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.manage_ssids_warning))
            .assertDoesNotExist()
    }

    @Test
    fun `Given a saved SSID when composed then the security warning is shown`() {
        setContent(wifiSsids = listOf("home-network"))

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.manage_ssids_warning))
            .assertExists()
    }

    @Test
    fun `Given the prioritize dropdown when a new option is selected then onSetPrioritize is invoked`() {
        setContent(prioritizeInternal = false)

        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.prioritize_internal_off))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(composeTestRule.stringResource(commonR.string.prioritize_internal_on_expanded))
            .performScrollTo()
            .performClick()

        assertEquals(true, prioritizeResult)
    }
}

package io.homeassistant.companion.android.settings.ssid

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.settings.ssid.views.SsidView

class SsidScreenshotTest {

    @PreviewTest
    @Preview
    @Composable
    fun `Empty state without location permission`() {
        HAThemeForPreview {
            SsidView(
                wifiSsids = emptyList(),
                canReadWifi = false,
                ethernet = null,
                vpn = null,
                prioritizeInternal = false,
                usingWifi = false,
                activeSsid = null,
                activeBssid = null,
                onAddWifiSsid = { true },
                onRemoveWifiSsid = {},
                onRequestPermission = {},
                onSetEthernet = {},
                onSetVpn = {},
                onSetPrioritize = {},
            )
        }
    }

    @PreviewTest
    @Preview
    @Composable
    fun `Saved SSIDs with an active suggestion and VPN enabled`() {
        HAThemeForPreview {
            SsidView(
                wifiSsids = listOf("home-assistant-wifi", "BSSID:1A:2B:3C:4D:5E:6F"),
                canReadWifi = true,
                ethernet = false,
                vpn = true,
                prioritizeInternal = true,
                usingWifi = true,
                activeSsid = "home-assistant-wifi",
                activeBssid = "1A:2B:3C:4D:5E:6F",
                onAddWifiSsid = { true },
                onRemoveWifiSsid = {},
                onRequestPermission = {},
                onSetEthernet = {},
                onSetVpn = {},
                onSetPrioritize = {},
            )
        }
    }
}

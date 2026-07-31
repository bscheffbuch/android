package io.homeassistant.companion.android.settings.ssid.views

import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAAccentButton
import io.homeassistant.companion.android.common.compose.composable.HADropdownItem
import io.homeassistant.companion.android.common.compose.composable.HADropdownMenu
import io.homeassistant.companion.android.common.compose.composable.HAHint
import io.homeassistant.companion.android.common.compose.composable.HAHorizontalDivider
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.composable.HATextField
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.network.WifiHelper
import io.homeassistant.companion.android.util.compose.HaAlertWarning
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@Composable
fun SsidView(
    wifiSsids: List<String>,
    canReadWifi: Boolean,
    ethernet: Boolean?,
    vpn: Boolean?,
    prioritizeInternal: Boolean,
    usingWifi: Boolean,
    activeSsid: String?,
    activeBssid: String?,
    onAddWifiSsid: (String) -> Boolean,
    onRemoveWifiSsid: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onSetEthernet: (Boolean) -> Unit,
    onSetVpn: (Boolean) -> Unit,
    onSetPrioritize: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalHAColorScheme.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = HADimens.SPACE4) + safeBottomPaddingValues(applyHorizontal = false),
    ) {
        item("intro") {
            Column {
                Text(
                    text = stringResource(commonR.string.manage_ssids_introduction),
                    style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
                    color = colorScheme.colorTextPrimary,
                    modifier = Modifier
                        .padding(horizontal = HADimens.SPACE4)
                        .padding(bottom = HADimens.SPACE4),
                )
                SsidSubheader(
                    title = stringResource(commonR.string.manage_ssids_wifi),
                    icon = Icons.Default.Wifi,
                    checked = null,
                    onClicked = null,
                )
                if (canReadWifi) {
                    SsidInput(onAddWifiSsid)
                } else {
                    Box(Modifier.padding(horizontal = HADimens.SPACE4)) {
                        HomeAssistantAppTheme {
                            HaAlertWarning(
                                message = stringResource(commonR.string.manage_ssids_permission),
                                action = stringResource(commonR.string.allow),
                                onActionClicked = onRequestPermission,
                            )
                        }
                    }
                }
            }
        }

        if (
            activeSsid?.isNotBlank() == true &&
            wifiSsids.none { it == activeSsid } &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || activeSsid !== WifiManager.UNKNOWN_SSID)
        ) {
            item("ssid.suggestion") {
                SsidSuggestionChip(
                    label = stringResource(commonR.string.add_ssid_name_suggestion, activeSsid),
                    onClick = { onAddWifiSsid(activeSsid) },
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE2),
                )
            }
        }
        itemsIndexed(
            items = wifiSsids,
            key = { index: Int, item: String ->
                if (wifiSsids.count { it == item } == 1) "ssid.item.$item" else "ssid.index.$index"
            },
        ) { _, it ->
            val connected = remember(it, activeSsid, activeBssid, usingWifi) {
                usingWifi &&
                    (
                        it == activeSsid ||
                            (
                                it.startsWith(WifiHelper.BSSID_PREFIX) &&
                                    it.removePrefix(WifiHelper.BSSID_PREFIX).equals(activeBssid, ignoreCase = true)
                                )
                        )
            }
            Row(
                modifier = Modifier
                    .heightIn(min = HADimens.SPACE12)
                    .padding(horizontal = HADimens.SPACE4)
                    .animateItem(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (connected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colorScheme.colorFillPrimaryLoudResting,
                    )
                    Spacer(Modifier.width(HADimens.SPACE4))
                }
                Text(
                    text =
                    if (it.startsWith(WifiHelper.BSSID_PREFIX)) {
                        it.removePrefix(WifiHelper.BSSID_PREFIX)
                    } else {
                        it
                    },
                    fontFamily =
                    if (it.startsWith(WifiHelper.BSSID_PREFIX)) {
                        FontFamily.Monospace
                    } else {
                        null
                    },
                    style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
                    color = colorScheme.colorTextPrimary,
                    modifier = Modifier
                        .padding(end = HADimens.SPACE4)
                        .weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(commonR.string.remove_ssid),
                    tint = colorScheme.colorOnDangerNormal,
                    modifier = Modifier
                        .clickable { onRemoveWifiSsid(it) }
                        .size(HADimens.SPACE12)
                        .padding(all = HADimens.SPACE3),
                )
            }
        }

        item("vpn") {
            SsidSubheader(
                title = stringResource(commonR.string.manage_ssids_vpn),
                icon = Icons.Default.VpnKey,
                checked = vpn,
                onClicked = { onSetVpn(it) },
            )
        }

        item("ethernet") {
            Column {
                Spacer(Modifier.height(HADimens.SPACE4))
                SsidSubheader(
                    title = stringResource(commonR.string.manage_ssids_ethernet),
                    icon = Icons.Default.SettingsEthernet,
                    checked = ethernet,
                    onClicked = { onSetEthernet(it) },
                )
            }
        }

        if (wifiSsids.isNotEmpty() || ethernet == true || vpn == true) {
            item("warn") {
                Box(
                    Modifier
                        .padding(horizontal = HADimens.SPACE4)
                        .padding(top = HADimens.SPACE8)
                        .animateItem(),
                ) {
                    HAHint(text = stringResource(commonR.string.manage_ssids_warning))
                }
            }
        }

        item("prioritize") {
            Column {
                Spacer(modifier = Modifier.height(HADimens.SPACE12))
                HAHorizontalDivider(modifier = Modifier.padding(horizontal = HADimens.SPACE4))
                Box(modifier = Modifier.padding(all = HADimens.SPACE4)) {
                    HADropdownMenu(
                        items = listOf(
                            HADropdownItem(key = false, label = stringResource(commonR.string.prioritize_internal_off)),
                            HADropdownItem(
                                key = true,
                                label = stringResource(commonR.string.prioritize_internal_on_expanded),
                            ),
                        ),
                        selectedKey = prioritizeInternal,
                        onItemSelected = onSetPrioritize,
                        label = stringResource(commonR.string.prioritize_internal_title),
                    )
                }
            }
        }
    }
}

@Composable
fun SsidSubheader(
    title: String,
    icon: ImageVector,
    checked: Boolean?,
    onClicked: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalHAColorScheme.current
    Row(
        modifier = modifier
            .heightIn(min = HADimens.SPACE14)
            .padding(horizontal = HADimens.SPACE4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.colorTextPrimary,
        )
        Text(
            text = title,
            style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
            color = colorScheme.colorTextPrimary,
            modifier = Modifier.padding(start = HADimens.SPACE4).weight(1f),
        )
        if (onClicked != null) {
            HASwitch(
                checked = checked == true,
                onCheckedChange = onClicked,
            )
        }
    }
}

@Composable
fun SsidInput(onSubmit: (String) -> Boolean, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(modifier = modifier.padding(horizontal = HADimens.SPACE4)) {
        var ssidInput by remember { mutableStateOf("") }
        var ssidError by remember { mutableStateOf(false) }

        HATextField(
            value = ssidInput,
            singleLine = true,
            onValueChange = {
                ssidInput = it
                ssidError = false
            },
            label = { Text(stringResource(commonR.string.manage_ssids_input)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    ssidError = !onSubmit(ssidInput)
                    if (!ssidError) ssidInput = ""
                },
            ),
            isError = ssidError,
            trailingIcon = if (ssidError) {
                {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(commonR.string.manage_ssids_input_exists),
                    )
                }
            } else {
                null
            },
            modifier = Modifier.weight(1f),
        )
        HAAccentButton(
            text = stringResource(commonR.string.add_ssid),
            onClick = {
                keyboardController?.hide()
                ssidError = !onSubmit(ssidInput)
                if (!ssidError) ssidInput = ""
            },
            modifier = Modifier
                .height(HADimens.SPACE14) // align with HATextField
                .padding(start = HADimens.SPACE2),
        )
    }
}

@Composable
private fun SsidSuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = LocalHAColorScheme.current
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(color = colorScheme.colorFillNeutralQuietResting, shape = RoundedCornerShape(HARadius.Pill))
            .padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = colorScheme.colorTextPrimary,
            modifier = Modifier.size(HADimens.SPACE5),
        )
        Text(
            text = label,
            style = HATextStyle.BodyMedium,
            color = colorScheme.colorTextPrimary,
            modifier = Modifier.padding(start = HADimens.SPACE2),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSsidViewEmpty() {
    HAThemeForPreview {
        SsidView(
            wifiSsids = emptyList(),
            canReadWifi = true,
            ethernet = null,
            vpn = null,
            prioritizeInternal = false,
            activeSsid = "home-assistant-wifi",
            activeBssid = "02:00:00:00:00:00",
            usingWifi = true,
            onAddWifiSsid = { true },
            onRemoveWifiSsid = {},
            onRequestPermission = {},
            onSetEthernet = {},
            onSetVpn = {},
            onSetPrioritize = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSsidViewItems() {
    HAThemeForPreview {
        SsidView(
            wifiSsids = listOf("home-assistant-wifi", "wifi-one", "BSSID:1A:2B:3C:4D:5E:6F"),
            canReadWifi = true,
            ethernet = false,
            vpn = true,
            prioritizeInternal = false,
            activeSsid = "home-assistant-wifi",
            activeBssid = "02:00:00:00:00:00",
            usingWifi = true,
            onAddWifiSsid = { true },
            onRemoveWifiSsid = {},
            onRequestPermission = {},
            onSetEthernet = {},
            onSetVpn = {},
            onSetPrioritize = {},
        )
    }
}

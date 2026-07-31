package io.homeassistant.companion.android.settings.controls.views

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.ButtonVariant
import io.homeassistant.companion.android.common.compose.composable.HAAccentButton
import io.homeassistant.companion.android.common.compose.composable.HADropdownItem
import io.homeassistant.companion.android.common.compose.composable.HADropdownMenu
import io.homeassistant.companion.android.common.compose.composable.HAFilledButton
import io.homeassistant.companion.android.common.compose.composable.HALoading
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.composable.HATextField
import io.homeassistant.companion.android.common.compose.theme.HABorderWidth
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.ControlsAuthRequiredSetting
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.friendlyName
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.util.compose.HaAlertWarning
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.compose.getEntityDomainString
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@Composable
fun ManageControlsView(
    panelEnabled: Boolean,
    authSetting: ControlsAuthRequiredSetting,
    authRequiredList: List<String>,
    entitiesLoaded: Boolean,
    entitiesList: Map<Int, List<Entity>>,
    panelSetting: Pair<String?, Int>?,
    serversList: List<Server>,
    structureEnabled: Boolean,
    defaultServer: Int,
    onSetPanelEnabled: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onSelectEntity: (String, Int) -> Unit,
    onSetPanelSetting: (String, Int) -> Unit,
    onSetStructureEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedServer by remember(defaultServer) { mutableIntStateOf(defaultServer) }
    val initialPanelEnabled by rememberSaveable { mutableStateOf(panelEnabled) }
    var panelServer by remember(panelSetting?.second) { mutableIntStateOf(panelSetting?.second ?: defaultServer) }
    var panelPath by remember(panelSetting?.first) { mutableStateOf(panelSetting?.first ?: "") }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = HADimens.SPACE4) + safeBottomPaddingValues(applyHorizontal = false),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            item {
                Text(
                    text = stringResource(commonR.string.controls_setting_panel),
                    style = HATextStyle.BodyMedium.copy(textAlign = TextAlign.Start),
                    color = LocalHAColorScheme.current.colorTextSecondary,
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = HADimens.SPACE4)
                        .padding(top = HADimens.SPACE4, bottom = HADimens.SPACE12)
                        .height(IntrinsicSize.Min),
                ) {
                    ManageControlsModeButton(
                        isPanel = false,
                        selected = !panelEnabled,
                        onClick = { onSetPanelEnabled(false) },
                        modifier = Modifier.weight(0.5f),
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(HABorderWidth.S),
                        color = LocalHAColorScheme.current.colorBorderNeutralQuiet,
                    )
                    ManageControlsModeButton(
                        isPanel = true,
                        selected = panelEnabled,
                        onClick = { onSetPanelEnabled(true) },
                        modifier = Modifier.weight(0.5f),
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || !panelEnabled) {
            if (serversList.size > 1) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = HADimens.SPACE4, bottom = HADimens.SPACE4, end = HADimens.SPACE4)
                            .fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(commonR.string.controls_structure_enabled),
                                style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
                                color = LocalHAColorScheme.current.colorTextPrimary,
                            )
                        }
                        HASwitch(
                            checked = structureEnabled,
                            onCheckedChange = onSetStructureEnabled,
                            modifier = Modifier.padding(start = HADimens.SPACE2),
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(commonR.string.controls_setting_choose_setting),
                    style = HATextStyle.BodyMedium.copy(textAlign = TextAlign.Start),
                    color = LocalHAColorScheme.current.colorTextSecondary,
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4),
                )
            }
            if (entitiesLoaded) {
                if (entitiesList.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.padding(all = HADimens.SPACE4)) {
                            HAFilledButton(
                                text = stringResource(commonR.string.controls_setting_choose_all),
                                onClick = onSelectAll,
                                variant = ButtonVariant.NEUTRAL,
                                enabled = authSetting !== ControlsAuthRequiredSetting.NONE,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(HADimens.SPACE4))
                            HAFilledButton(
                                text = stringResource(commonR.string.controls_setting_choose_none),
                                onClick = onSelectNone,
                                variant = ButtonVariant.NEUTRAL,
                                enabled = authSetting !== ControlsAuthRequiredSetting.ALL,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (serversList.size > 1) {
                        item {
                            HADropdownMenu(
                                items = serversList.map { HADropdownItem(key = it.id, label = it.friendlyName) },
                                selectedKey = selectedServer,
                                onItemSelected = { selectedServer = it },
                                label = stringResource(commonR.string.server_select),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = HADimens.SPACE4, end = HADimens.SPACE4, bottom = HADimens.SPACE4),
                            )
                        }
                    }
                    items(entitiesList[selectedServer]?.size ?: 0, key = {
                        "$selectedServer.${entitiesList[selectedServer]?.get(it)?.entityId}"
                    }) { index ->
                        val entity = entitiesList[selectedServer]?.get(index) ?: return@items
                        ManageControlsEntity(
                            entityName = entity.friendlyName,
                            entityDomain = entity.domain,
                            selected = (
                                authSetting == ControlsAuthRequiredSetting.NONE ||
                                    (
                                        authSetting == ControlsAuthRequiredSetting.SELECTION &&
                                            !authRequiredList.contains("$selectedServer.${entity.entityId}")
                                        )
                                ),
                            onClick = { onSelectEntity(entity.entityId, selectedServer) },
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(commonR.string.controls_setting_choose_empty),
                            style = HATextStyle.Body.copy(textAlign = TextAlign.Start, fontStyle = FontStyle.Italic),
                            color = LocalHAColorScheme.current.colorTextSecondary,
                            modifier = Modifier.padding(all = HADimens.SPACE4),
                        )
                    }
                }
            } else {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(HADimens.SPACE6))
                        HALoading(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        } else {
            if (!initialPanelEnabled) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = HADimens.SPACE4)
                            .padding(bottom = HADimens.SPACE4),
                    ) {
                        HomeAssistantAppTheme {
                            HaAlertWarning(
                                message = stringResource(commonR.string.controls_setting_alert),
                                action = null,
                                onActionClicked = {},
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(commonR.string.controls_setting_dashboard_setting),
                    style = HATextStyle.BodyMedium.copy(textAlign = TextAlign.Start),
                    color = LocalHAColorScheme.current.colorTextSecondary,
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4),
                )
            }
            if (serversList.size > 1) {
                item {
                    HADropdownMenu(
                        items = serversList.map { HADropdownItem(key = it.id, label = it.friendlyName) },
                        selectedKey = panelServer,
                        onItemSelected = { panelServer = it },
                        label = stringResource(commonR.string.server_select),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HADimens.SPACE4)
                            .padding(top = HADimens.SPACE4),
                    )
                }
            }
            item {
                HATextField(
                    value = panelPath,
                    onValueChange = { panelPath = it },
                    label = { Text(stringResource(id = R.string.lovelace_view_dashboard)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                    ),
                    modifier = Modifier.padding(all = HADimens.SPACE4),
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(start = HADimens.SPACE4, bottom = HADimens.SPACE4),
                ) {
                    HAAccentButton(
                        text = stringResource(commonR.string.save),
                        enabled = (
                            (
                                panelPath != panelSetting?.first &&
                                    !(panelPath == "" && panelSetting != null && panelSetting.first == null)
                                ) ||
                                panelServer != panelSetting.second
                            ),
                        onClick = { onSetPanelSetting(panelPath, panelServer) },
                    )
                }
            }
        }
    }
}

@Composable
fun ManageControlsEntity(
    entityName: String,
    entityDomain: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalHAColorScheme.current
    Row(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(all = HADimens.SPACE4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            colors = checkboxColors(),
            modifier = Modifier.padding(end = HADimens.SPACE4),
            // Handled by parent Row clickable modifier
            onCheckedChange = null,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = entityName,
                style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
                color = colorScheme.colorTextPrimary,
            )
            Text(
                text = getEntityDomainString(entityDomain),
                style = HATextStyle.BodyMedium.copy(textAlign = TextAlign.Start),
                color = colorScheme.colorTextSecondary,
            )
        }
    }
}

@Composable
fun ManageControlsModeButton(isPanel: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = LocalHAColorScheme.current
    Box(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .selectable(selected = selected, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(all = HADimens.SPACE2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                asset = if (isPanel) {
                    CommunityMaterial.Icon3.cmd_view_dashboard
                } else {
                    CommunityMaterial.Icon.cmd_dip_switch
                },
                contentDescription = null,
                modifier = Modifier.size(HADimens.SPACE9),
                colorFilter = ColorFilter.tint(colorScheme.colorTextPrimary),
            )
            Text(
                text = stringResource(
                    if (isPanel) commonR.string.lovelace else commonR.string.controls_setting_mode_builtin_title,
                ),
                style = HATextStyle.Body.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.colorTextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                // Add newline at the end for spacing
                text = "${stringResource(
                    if (isPanel) {
                        commonR.string.controls_setting_mode_panel_info
                    } else {
                        commonR.string.controls_setting_mode_builtin_info
                    },
                )}\n",
                style = HATextStyle.BodyMedium,
                color = colorScheme.colorTextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            RadioButton(
                selected = selected,
                colors = radioButtonColors(),
                // Handled by parent
                onClick = null,
            )
        }
    }
}

@Composable
private fun checkboxColors(): CheckboxColors {
    val colorScheme = LocalHAColorScheme.current
    return CheckboxColors(
        checkedCheckmarkColor = colorScheme.colorOnPrimaryLoud,
        uncheckedCheckmarkColor = Color.Transparent,
        checkedBoxColor = colorScheme.colorFillPrimaryLoudResting,
        uncheckedBoxColor = Color.Transparent,
        disabledCheckedBoxColor = colorScheme.colorFillDisabledLoudResting,
        disabledUncheckedBoxColor = Color.Transparent,
        disabledIndeterminateBoxColor = colorScheme.colorFillDisabledLoudResting,
        checkedBorderColor = colorScheme.colorFillPrimaryLoudResting,
        uncheckedBorderColor = colorScheme.colorOnNeutralNormal,
        disabledBorderColor = colorScheme.colorOnDisabledNormal,
        disabledUncheckedBorderColor = colorScheme.colorOnDisabledNormal,
        disabledIndeterminateBorderColor = colorScheme.colorOnDisabledNormal,
    )
}

@Composable
private fun radioButtonColors(): RadioButtonColors {
    val colorScheme = LocalHAColorScheme.current
    return RadioButtonColors(
        selectedColor = colorScheme.colorOnPrimaryNormal,
        unselectedColor = colorScheme.colorOnNeutralNormal,
        disabledUnselectedColor = colorScheme.colorOnDisabledNormal,
        disabledSelectedColor = colorScheme.colorOnDisabledNormal,
    )
}

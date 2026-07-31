package io.homeassistant.companion.android.settings.qs.views

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mikepenz.iconics.compose.Image
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HADropdownItem
import io.homeassistant.companion.android.common.compose.composable.HADropdownMenu
import io.homeassistant.companion.android.common.compose.composable.HAFilledButton
import io.homeassistant.companion.android.common.compose.composable.HAPlainButton
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.composable.HATextField
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.settings.qs.ManageTilesViewModel
import io.homeassistant.companion.android.util.compose.entity.EntityPicker
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import io.homeassistant.companion.android.util.safeBottomWindowInsets
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun ManageTilesView(
    viewModel: ManageTilesViewModel,
    onShowIconDialog: (tag: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colorScheme = LocalHAColorScheme.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect("snackbar") {
        viewModel.tileInfoSnackbar.onEach {
            if (it != 0) {
                snackbarHostState.showSnackbar(context.getString(it))
            }
        }.launchIn(this)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(safeBottomWindowInsets(applyHorizontal = false)),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(safeBottomPaddingValues(applyHorizontal = false))
                .padding(all = HADimens.SPACE4),
        ) {
            HADropdownMenu(
                items = viewModel.slots.map { HADropdownItem(key = it, label = it.name) },
                selectedKey = viewModel.selectedTile,
                onItemSelected = { slot -> viewModel.selectTile(viewModel.slots.indexOf(slot)) },
                label = stringResource(R.string.tile_select),
            )

            HATextField(
                value = viewModel.tileLabel,
                onValueChange = { viewModel.tileLabel = it },
                label = { Text(text = stringResource(id = R.string.tile_label)) },
                modifier = Modifier.padding(top = HADimens.SPACE4),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                HATextField(
                    value = viewModel.tileSubtitle.orEmpty(),
                    onValueChange = { viewModel.tileSubtitle = it },
                    label = { Text(text = stringResource(id = R.string.tile_subtitle)) },
                    modifier = Modifier.padding(top = HADimens.SPACE4),
                )
            }

            if (viewModel.servers.size > 1 || viewModel.servers.none { it.id == viewModel.selectedServerId }) {
                HADropdownMenu(
                    items = viewModel.servers.map { HADropdownItem(key = it.id, label = it.friendlyName) },
                    selectedKey = viewModel.selectedServerId,
                    onItemSelected = viewModel::selectServerId,
                    label = stringResource(R.string.tile_server),
                    modifier = Modifier.padding(top = HADimens.SPACE4),
                )
            }

            EntityPicker(
                entities = viewModel.sortedEntities,
                selectedEntityId = viewModel.selectedEntityId,
                onEntitySelectedId = { viewModel.selectEntityId(it) },
                onEntityCleared = { viewModel.selectEntityId("") },
                modifier = Modifier.padding(vertical = HADimens.SPACE4),
                addButtonText = stringResource(R.string.tile_entity),
                entityRegistry = viewModel.entityRegistry,
                deviceRegistry = viewModel.deviceRegistry,
                areaRegistry = viewModel.areaRegistry,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.tile_icon),
                    style = HATextStyle.BodyMedium,
                    color = colorScheme.colorTextPrimary,
                    modifier = Modifier.padding(end = HADimens.SPACE2),
                )
                IconButton(onClick = { onShowIconDialog(viewModel.selectedTile.id) }) {
                    viewModel.selectedIcon?.let { icon ->
                        Image(
                            icon,
                            contentDescription = stringResource(id = R.string.tile_icon),
                            colorFilter = ColorFilter.tint(colorScheme.colorFillPrimaryLoudResting),
                            modifier = Modifier.size(HADimens.SPACE6),
                        )
                    }
                }
                if (viewModel.selectedIconId != null && viewModel.selectedEntityId.isNotBlank()) {
                    HAPlainButton(
                        text = stringResource(R.string.tile_icon_original),
                        onClick = { viewModel.selectIcon(null) },
                        modifier = Modifier.padding(start = HADimens.SPACE1),
                    )
                }
            }

            val vibrateLabel = stringResource(R.string.tile_vibrate)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = vibrateLabel,
                    style = HATextStyle.BodyMedium,
                    color = colorScheme.colorTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                HASwitch(
                    checked = viewModel.selectedShouldVibrate,
                    onCheckedChange = { viewModel.selectedShouldVibrate = it },
                    modifier = Modifier.semantics { contentDescription = vibrateLabel },
                )
            }

            val authRequiredLabel = stringResource(R.string.tile_auth_required)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = authRequiredLabel,
                    style = HATextStyle.BodyMedium,
                    color = colorScheme.colorTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                HASwitch(
                    checked = viewModel.tileAuthRequired,
                    onCheckedChange = { viewModel.tileAuthRequired = it },
                    modifier = Modifier.semantics { contentDescription = authRequiredLabel },
                )
            }

            HAFilledButton(
                text = stringResource(viewModel.submitButtonLabel),
                onClick = { viewModel.addTile() },
                enabled = viewModel.tileLabel.isNotBlank() &&
                    viewModel.selectedServerId in viewModel.servers.map { it.id } &&
                    viewModel.selectedEntityId in viewModel.sortedEntities.map { it.entityId },
                modifier = Modifier
                    .padding(top = HADimens.SPACE6)
                    .fillMaxWidth(),
            )
        }
    }
}

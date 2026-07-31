package io.homeassistant.companion.android.automations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.homeassistant.companion.android.automations.AutomationsUiState
import io.homeassistant.companion.android.automations.AutomationsViewModel
import io.homeassistant.companion.android.automations.SaveSceneDialogUiState
import io.homeassistant.companion.android.automations.SceneEntityCandidate
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.isActive
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun AutomationsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AutomationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveSceneDialogState by viewModel.saveSceneDialogState.collectAsStateWithLifecycle()
    AutomationsScreen(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onTriggerNow = viewModel::triggerNow,
        onActivateScene = viewModel::activateScene,
        errorEvents = viewModel.errorEvents,
        onNavigateBack = onNavigateBack,
        saveSceneDialogState = saveSceneDialogState,
        onCreateSceneClicked = viewModel::onCreateSceneClicked,
        onDismissCreateScene = viewModel::onDismissCreateScene,
        onSaveScene = viewModel::saveScene,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutomationsScreen(
    uiState: AutomationsUiState,
    onToggle: (String) -> Unit,
    onTriggerNow: (String) -> Unit,
    onActivateScene: (String) -> Unit,
    errorEvents: Flow<String>,
    onNavigateBack: (() -> Unit)? = null,
    saveSceneDialogState: SaveSceneDialogUiState = SaveSceneDialogUiState.Hidden,
    onCreateSceneClicked: () -> Unit = {},
    onDismissCreateScene: () -> Unit = {},
    onSaveScene: (String, Set<String>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = LocalHAColorScheme.current
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorEvents) {
        errorEvents.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is AutomationsUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = onCreateSceneClicked,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(commonR.string.overview_save_scene_dialog_title)) },
                    containerColor = colors.colorFillPrimaryLoudResting,
                    contentColor = colors.colorOnPrimaryLoud,
                )
            }
        },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(commonR.string.automations_scenes_title)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = stringResource(commonR.string.back),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.colorSurfaceDefault,
                    scrolledContainerColor = colors.colorSurfaceLow,
                    titleContentColor = colors.colorTextPrimary,
                    actionIconContentColor = colors.colorTextSecondary,
                ),
            )
        },
        containerColor = colors.colorSurfaceDefault,
    ) { innerPadding ->
        when (uiState) {
            is AutomationsUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            is AutomationsUiState.Error -> ErrorContent(uiState.message, Modifier.padding(innerPadding))
            is AutomationsUiState.Success -> {
                if (uiState.automations.isEmpty() && uiState.scenes.isEmpty()) {
                    EmptyContent(Modifier.padding(innerPadding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(HADimens.SPACE4),
                        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
                    ) {
                        if (uiState.automations.isNotEmpty()) {
                            item(key = "automations_header") {
                                SectionHeader(stringResource(commonR.string.automations_title))
                            }
                            items(uiState.automations, key = { it.entityId }) { entity ->
                                AutomationRow(
                                    entity = entity,
                                    onToggle = { onToggle(entity.entityId) },
                                    onTriggerNow = { onTriggerNow(entity.entityId) },
                                )
                            }
                        }
                        if (uiState.scenes.isNotEmpty()) {
                            item(key = "scenes_header") {
                                SectionHeader(stringResource(commonR.string.scenes))
                            }
                            items(uiState.scenes, key = { it.entityId }) { entity ->
                                SceneRow(
                                    entity = entity,
                                    onActivate = { onActivateScene(entity.entityId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val dialog = saveSceneDialogState) {
        SaveSceneDialogUiState.Hidden -> Unit
        SaveSceneDialogUiState.Loading -> SaveSceneLoadingDialog(onDismiss = onDismissCreateScene)
        is SaveSceneDialogUiState.Ready -> SaveSceneDialog(
            state = dialog,
            onDismiss = onDismissCreateScene,
            onSave = onSaveScene,
        )
    }
}

@Composable
private fun AutomationRow(entity: Entity, onToggle: () -> Unit, onTriggerNow: () -> Unit) {
    val colors = LocalHAColorScheme.current
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val isActive = entity.isActive()

    HASettingsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
        ) {
            IconButton(onClick = onTriggerNow) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = stringResource(commonR.string.overview_automation_run_now),
                    tint = colors.colorTextSecondary,
                )
            }
            Text(
                text = friendlyName,
                style = HATextStyle.Body,
                color = colors.colorTextPrimary,
                modifier = Modifier.weight(1f),
            )
            HASwitch(
                checked = isActive,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = LocalHAColorScheme.current
    Text(
        text = title,
        style = HATextStyle.BodyMedium.copy(fontWeight = FontWeight.W600, textAlign = TextAlign.Start),
        color = colors.colorTextSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = HADimens.SPACE2, bottom = HADimens.SPACE1),
    )
}

@Composable
private fun SceneRow(entity: Entity, onActivate: () -> Unit) {
    val colors = LocalHAColorScheme.current
    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId

    HASettingsCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onActivate),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE3),
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(commonR.string.automations_scene_activate),
                tint = colors.colorTextSecondary,
            )
            Text(
                text = friendlyName,
                style = HATextStyle.Body,
                color = colors.colorTextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SaveSceneDialog(
    state: SaveSceneDialogUiState.Ready,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>) -> Unit,
) {
    val colors = LocalHAColorScheme.current
    var name by remember { mutableStateOf("") }
    var selectedIds by remember(state.candidates) {
        mutableStateOf(state.candidates.map { it.entityId }.toSet())
    }
    val canSave = name.isNotBlank() && selectedIds.isNotEmpty() && !state.isSaving

    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text(stringResource(commonR.string.overview_save_scene_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE3)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(commonR.string.overview_save_scene_name_label)) },
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.candidates.isEmpty()) {
                    Text(
                        text = stringResource(commonR.string.overview_save_scene_empty),
                        color = colors.colorTextSecondary,
                    )
                } else {
                    Text(
                        text = stringResource(commonR.string.overview_save_scene_entities_header),
                        color = colors.colorTextPrimary,
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
                        modifier = Modifier.heightIn(max = 320.dp),
                    ) {
                        items(state.candidates, key = { it.entityId }) { candidate ->
                            val selected = candidate.entityId in selectedIds
                            SceneEntityPickerRow(
                                candidate = candidate,
                                selected = selected,
                                enabled = !state.isSaving,
                                onToggle = {
                                    selectedIds = if (selected) {
                                        selectedIds - candidate.entityId
                                    } else {
                                        selectedIds + candidate.entityId
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onSave(name, selectedIds) }) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.colorFillPrimaryLoudResting,
                    )
                } else {
                    Text(stringResource(commonR.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(stringResource(commonR.string.cancel))
            }
        },
        containerColor = colors.colorSurfaceDefault,
        titleContentColor = colors.colorTextPrimary,
        textContentColor = colors.colorTextPrimary,
    )
}

@Composable
private fun SceneEntityPickerRow(
    candidate: SceneEntityCandidate,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalHAColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = colors.colorFillPrimaryLoudResting),
        )
        Text(
            text = candidate.friendlyName,
            style = HATextStyle.Body,
            color = colors.colorTextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SaveSceneLoadingDialog(onDismiss: () -> Unit) {
    val colors = LocalHAColorScheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(commonR.string.overview_save_scene_dialog_title)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HADimens.SPACE4),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.colorFillPrimaryLoudResting)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(commonR.string.cancel))
            }
        },
        containerColor = colors.colorSurfaceDefault,
        titleContentColor = colors.colorTextPrimary,
        textContentColor = colors.colorTextPrimary,
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val colors = LocalHAColorScheme.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = colors.colorFillPrimaryLoudResting)
    }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier = Modifier) {
    val colors = LocalHAColorScheme.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = colors.colorOnDangerNormal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val colors = LocalHAColorScheme.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(commonR.string.automations_empty),
            color = colors.colorTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun AutomationsScreenPreview() {
    HAThemeForPreview {
        AutomationsScreen(
            uiState = AutomationsUiState.Success(
                automations = listOf(
                    Entity(
                        entityId = "automation.morning_routine",
                        state = "on",
                        attributes = mapOf("friendly_name" to "Morning routine"),
                        lastChanged = LocalDateTime.now(),
                        lastUpdated = LocalDateTime.now(),
                    ),
                    Entity(
                        entityId = "automation.night_mode",
                        state = "off",
                        attributes = mapOf("friendly_name" to "Night mode"),
                        lastChanged = LocalDateTime.now(),
                        lastUpdated = LocalDateTime.now(),
                    ),
                ),
                scenes = listOf(
                    Entity(
                        entityId = "scene.movie_night",
                        state = "2026-07-05T00:00:00Z",
                        attributes = mapOf("friendly_name" to "Movie night"),
                        lastChanged = LocalDateTime.now(),
                        lastUpdated = LocalDateTime.now(),
                    ),
                ),
            ),
            onToggle = {},
            onTriggerNow = {},
            onActivateScene = {},
            errorEvents = emptyFlow(),
            onNavigateBack = {},
        )
    }
}

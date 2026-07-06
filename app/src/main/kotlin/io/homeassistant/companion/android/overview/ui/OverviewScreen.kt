package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.overview.OverviewLightGroup
import io.homeassistant.companion.android.overview.OverviewUiState
import io.homeassistant.companion.android.overview.OverviewViewModel
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.VisibleForTesting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    onRefresh: () -> Unit,
    onToggleEntity: (String) -> Unit,
    onToggleLightGroup: (String) -> Unit,
    onBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onGroupBrightnessChange: (groupId: String, brightness: Float, immediate: Boolean) -> Unit,
    onColorChange: (entityId: String, rgbColor: List<Int>) -> Unit,
    onGroupColorChange: (groupId: String, rgbColor: List<Int>) -> Unit,
    onColorTemperatureChange: (entityId: String, kelvin: Int, immediate: Boolean) -> Unit,
    onGroupColorTemperatureChange: (groupId: String, kelvin: Int, immediate: Boolean) -> Unit,
    onEditModeChange: (Boolean) -> Unit,
    onSaveLightGroup: (groupId: String?, name: String, entityIds: List<String>, colorArgb: Long?) -> Unit,
    onDeleteLightGroup: (String) -> Unit,
    onMoveItem: (fromKey: String, toKey: String) -> Unit,
    onItemDrop: (sourceKey: String, targetKey: String) -> Unit,
    onRemoveEntityFromGroup: (groupId: String, entityId: String) -> Unit,
    onGroupExpandedChange: (groupId: String, expanded: Boolean) -> Unit,
    onSetDisplayedAsLight: (entityId: String, asLight: Boolean) -> Unit,
    onTriggerAutomation: (String) -> Unit,
    onSetFanSpeed: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onSetCoverPosition: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onStopCover: (String) -> Unit,
    onCycleClimateHvacMode: (String) -> Unit,
    onSetClimateTemperature: (entityId: String, temperature: Float, immediate: Boolean) -> Unit,
    onTogglePlayback: (String) -> Unit,
    onSetMediaVolume: (entityId: String, volume: Float, immediate: Boolean) -> Unit,
    onSkipToPreviousTrack: (String) -> Unit,
    onSkipToNextTrack: (String) -> Unit,
    onSetHumidifierHumidity: (entityId: String, humidity: Float, immediate: Boolean) -> Unit,
    onCycleHumidifierMode: (entityId: String) -> Unit,
    onClose: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenAutomations: (() -> Unit)? = null,
    errorEvents: Flow<String>? = null,
) {
    val colors = LocalHAColorScheme.current
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    var detailEntityId by remember { mutableStateOf<String?>(null) }
    var detailGroupId by remember { mutableStateOf<String?>(null) }
    var groupEditor by remember { mutableStateOf<OverviewLightGroup?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    val success = uiState as? OverviewUiState.Success
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorEvents) {
        errorEvents?.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(commonR.string.overview_title)) },
                navigationIcon = {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(commonR.string.close),
                            )
                        }
                    }
                },
                actions = {
                    if (onOpenAutomations != null) {
                        IconButton(onClick = onOpenAutomations) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = stringResource(commonR.string.automations_title),
                            )
                        }
                    }
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(commonR.string.settings),
                            )
                        }
                    }
                    if (success?.isEditMode == true) {
                        IconButton(onClick = { showNewGroupDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(commonR.string.overview_add_group),
                            )
                        }
                    }
                    IconButton(onClick = { onEditModeChange(success?.isEditMode != true) }) {
                        Icon(
                            imageVector = if (success?.isEditMode == true) Icons.Rounded.Done else Icons.Rounded.Edit,
                            contentDescription = stringResource(
                                if (success?.isEditMode == true) commonR.string.done else commonR.string.edit,
                            ),
                        )
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
            is OverviewUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            is OverviewUiState.Error -> ErrorContent(uiState.message, Modifier.padding(innerPadding))
            is OverviewUiState.Success -> {
                val lightEntities = uiState.entities.filter { it.domain == "light" }
                val displayItems = remember(
                    uiState.entities,
                    uiState.lightGroups,
                    uiState.itemOrder,
                    uiState.expandedLightGroupIds,
                ) {
                    uiState.toDisplayItems()
                }
                OverviewGrid(
                    displayItems = displayItems,
                    isEditMode = uiState.isEditMode,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    onMoveItem = onMoveItem,
                    onToggleEntity = onToggleEntity,
                    onToggleLightGroup = onToggleLightGroup,
                    onBrightnessChange = onBrightnessChange,
                    onGroupBrightnessChange = onGroupBrightnessChange,
                    onOpenEntityDetail = { detailEntityId = it.entityId },
                    onOpenGroupDetail = { detailGroupId = it.id },
                    onEditGroup = { groupEditor = it },
                    onDeleteLightGroup = onDeleteLightGroup,
                    onItemDrop = onItemDrop,
                    onRemoveEntityFromGroup = onRemoveEntityFromGroup,
                    onGroupExpandedChange = onGroupExpandedChange,
                    displayedAsLightEntityIds = uiState.displayedAsLightEntityIds,
                    onSetDisplayedAsLight = onSetDisplayedAsLight,
                    onTriggerAutomation = onTriggerAutomation,
                    onSetFanSpeed = onSetFanSpeed,
                    onSetCoverPosition = onSetCoverPosition,
                    onStopCover = onStopCover,
                    onCycleClimateHvacMode = onCycleClimateHvacMode,
                    onSetClimateTemperature = onSetClimateTemperature,
                    onTogglePlayback = onTogglePlayback,
                    onSetMediaVolume = onSetMediaVolume,
                    onSkipToPreviousTrack = onSkipToPreviousTrack,
                    onSkipToNextTrack = onSkipToNextTrack,
                    onSetHumidifierHumidity = onSetHumidifierHumidity,
                    onCycleHumidifierMode = onCycleHumidifierMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )

                if (showNewGroupDialog) {
                    LightGroupEditorDialog(
                        group = null,
                        lightEntities = lightEntities,
                        onDismiss = { showNewGroupDialog = false },
                        onSave = { groupId, name, entityIds, colorArgb ->
                            onSaveLightGroup(groupId, name, entityIds, colorArgb)
                            showNewGroupDialog = false
                        },
                    )
                }
                groupEditor?.let { group ->
                    LightGroupEditorDialog(
                        group = group,
                        lightEntities = lightEntities,
                        onDismiss = { groupEditor = null },
                        onSave = { groupId, name, entityIds, colorArgb ->
                            onSaveLightGroup(groupId, name, entityIds, colorArgb)
                            groupEditor = null
                        },
                    )
                }
            }
        }
    }

    val currentSuccess = uiState as? OverviewUiState.Success
    detailEntityId?.let { entityId ->
        val entity = currentSuccess?.entities?.firstOrNull { it.entityId == entityId } ?: return@let
        EntityDetailBottomSheet(
            entity = entity,
            onDismiss = { detailEntityId = null },
            onToggle = { onToggleEntity(entity.entityId) },
            onBrightnessChange = { brightness, immediate ->
                onBrightnessChange(entity.entityId, brightness, immediate)
            },
            onColorChange = { rgbColor -> onColorChange(entity.entityId, rgbColor) },
            onColorTemperatureChange = { kelvin, immediate ->
                onColorTemperatureChange(entity.entityId, kelvin, immediate)
            },
            displayedAsLight = entity.entityId in currentSuccess.displayedAsLightEntityIds,
            onDisplayedAsLightChange = { onSetDisplayedAsLight(entity.entityId, it) },
        )
    }

    detailGroupId?.let { groupId ->
        val group = currentSuccess?.lightGroups?.firstOrNull { it.id == groupId } ?: return@let
        val members = currentSuccess.entities.filter { it.entityId in group.entityIds }
        LightGroupDetailBottomSheet(
            group = group,
            entities = members,
            onDismiss = { detailGroupId = null },
            onBrightnessChange = { brightness, immediate ->
                onGroupBrightnessChange(group.id, brightness, immediate)
            },
            onColorChange = { rgbColor -> onGroupColorChange(group.id, rgbColor) },
            onColorTemperatureChange = { kelvin, immediate ->
                onGroupColorTemperatureChange(group.id, kelvin, immediate)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewGrid(
    displayItems: List<OverviewDisplayItem>,
    isEditMode: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMoveItem: (fromKey: String, toKey: String) -> Unit,
    onToggleEntity: (String) -> Unit,
    onToggleLightGroup: (String) -> Unit,
    onBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onGroupBrightnessChange: (groupId: String, brightness: Float, immediate: Boolean) -> Unit,
    onOpenEntityDetail: (Entity) -> Unit,
    onOpenGroupDetail: (OverviewLightGroup) -> Unit,
    onEditGroup: (OverviewLightGroup) -> Unit,
    onDeleteLightGroup: (String) -> Unit,
    onItemDrop: (sourceKey: String, targetKey: String) -> Unit,
    onRemoveEntityFromGroup: (groupId: String, entityId: String) -> Unit,
    onGroupExpandedChange: (groupId: String, expanded: Boolean) -> Unit,
    displayedAsLightEntityIds: Set<String>,
    onSetDisplayedAsLight: (entityId: String, asLight: Boolean) -> Unit,
    onTriggerAutomation: (String) -> Unit,
    onSetFanSpeed: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onSetCoverPosition: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onStopCover: (String) -> Unit,
    onCycleClimateHvacMode: (String) -> Unit,
    onSetClimateTemperature: (entityId: String, temperature: Float, immediate: Boolean) -> Unit,
    onTogglePlayback: (String) -> Unit,
    onSetMediaVolume: (entityId: String, volume: Float, immediate: Boolean) -> Unit,
    onSkipToPreviousTrack: (String) -> Unit,
    onSkipToNextTrack: (String) -> Unit,
    onSetHumidifierHumidity: (entityId: String, humidity: Float, immediate: Boolean) -> Unit,
    onCycleHumidifierMode: (entityId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyGridState = rememberLazyGridState()
    var draggingSourceKey by remember { mutableStateOf<String?>(null) }
    var groupDropTargetKey by remember { mutableStateOf<String?>(null) }
    var swapDropTargetKey by remember { mutableStateOf<String?>(null) }
    val borrowedNeighbors = remember(displayItems) { displayItems.withBorrowedNeighbors() }

    androidx.compose.runtime.LaunchedEffect(displayItems, isEditMode) {
        if (!isEditMode) {
            draggingSourceKey = null
            groupDropTargetKey = null
            swapDropTargetKey = null
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(OverviewCardGap),
            horizontalArrangement = Arrangement.spacedBy(OverviewCardGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = borrowedNeighbors.items,
                key = { _, item -> item.key },
                span = { _, item ->
                    GridItemSpan(if (item is OverviewDisplayItem.Group && item.isExpanded) maxLineSpan else 1)
                },
            ) { index, item ->
                OverviewGridItem(
                    item = item,
                    index = index,
                    displayItems = borrowedNeighbors.items,
                    borrowedNeighbor = (item as? OverviewDisplayItem.Group)?.let {
                        borrowedNeighbors.byGroupKey[it.key]
                    },
                    visibleItemsInfoProvider = { lazyGridState.layoutInfo.visibleItemsInfo },
                    isEditMode = isEditMode,
                    isGroupDropTarget = item.key == groupDropTargetKey,
                    isSwapDropTarget = item.key == swapDropTargetKey,
                    modifier = Modifier.animateItem(),
                    onMoveItem = onMoveItem,
                    onToggleEntity = onToggleEntity,
                    onToggleLightGroup = onToggleLightGroup,
                    onBrightnessChange = onBrightnessChange,
                    onGroupBrightnessChange = onGroupBrightnessChange,
                    onOpenEntityDetail = onOpenEntityDetail,
                    onOpenGroupDetail = onOpenGroupDetail,
                    onEditGroup = onEditGroup,
                    onDeleteLightGroup = onDeleteLightGroup,
                    onItemDrop = onItemDrop,
                    onRemoveEntityFromGroup = onRemoveEntityFromGroup,
                    onGroupExpandedChange = onGroupExpandedChange,
                    displayedAsLightEntityIds = displayedAsLightEntityIds,
                    onSetDisplayedAsLight = onSetDisplayedAsLight,
                    onTriggerAutomation = onTriggerAutomation,
                    onSetFanSpeed = onSetFanSpeed,
                    onSetCoverPosition = onSetCoverPosition,
                    onStopCover = onStopCover,
                    onCycleClimateHvacMode = onCycleClimateHvacMode,
                    onSetClimateTemperature = onSetClimateTemperature,
                    onTogglePlayback = onTogglePlayback,
                    onSetMediaVolume = onSetMediaVolume,
                    onSkipToPreviousTrack = onSkipToPreviousTrack,
                    onSkipToNextTrack = onSkipToNextTrack,
                    onSetHumidifierHumidity = onSetHumidifierHumidity,
                    onCycleHumidifierMode = onCycleHumidifierMode,
                    onDragStart = { sourceKey ->
                        draggingSourceKey = sourceKey
                    },
                    onDragPreview = { _, targetKey, isGrouping ->
                        groupDropTargetKey = if (isGrouping) targetKey else null
                        swapDropTargetKey = if (!isGrouping) targetKey else null
                    },
                    onDragFinished = {
                        draggingSourceKey = null
                        groupDropTargetKey = null
                        swapDropTargetKey = null
                    },
                )
            }
        }
    }
}

@Composable
private fun OverviewGridItem(
    item: OverviewDisplayItem,
    index: Int,
    displayItems: List<OverviewDisplayItem>,
    borrowedNeighbor: Entity?,
    visibleItemsInfoProvider: () -> List<LazyGridItemInfo>,
    isEditMode: Boolean,
    isGroupDropTarget: Boolean,
    isSwapDropTarget: Boolean,
    modifier: Modifier = Modifier,
    onMoveItem: (fromKey: String, toKey: String) -> Unit,
    onToggleEntity: (String) -> Unit,
    onToggleLightGroup: (String) -> Unit,
    onBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onGroupBrightnessChange: (groupId: String, brightness: Float, immediate: Boolean) -> Unit,
    onOpenEntityDetail: (Entity) -> Unit,
    onOpenGroupDetail: (OverviewLightGroup) -> Unit,
    onEditGroup: (OverviewLightGroup) -> Unit,
    onDeleteLightGroup: (String) -> Unit,
    onItemDrop: (sourceKey: String, targetKey: String) -> Unit,
    onRemoveEntityFromGroup: (groupId: String, entityId: String) -> Unit,
    onGroupExpandedChange: (groupId: String, expanded: Boolean) -> Unit,
    displayedAsLightEntityIds: Set<String>,
    onSetDisplayedAsLight: (entityId: String, asLight: Boolean) -> Unit,
    onTriggerAutomation: (String) -> Unit,
    onSetFanSpeed: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onSetCoverPosition: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onStopCover: (String) -> Unit,
    onCycleClimateHvacMode: (String) -> Unit,
    onSetClimateTemperature: (entityId: String, temperature: Float, immediate: Boolean) -> Unit,
    onTogglePlayback: (String) -> Unit,
    onSetMediaVolume: (entityId: String, volume: Float, immediate: Boolean) -> Unit,
    onSkipToPreviousTrack: (String) -> Unit,
    onSkipToNextTrack: (String) -> Unit,
    onSetHumidifierHumidity: (entityId: String, humidity: Float, immediate: Boolean) -> Unit,
    onCycleHumidifierMode: (entityId: String) -> Unit,
    onDragStart: (sourceKey: String) -> Unit,
    onDragPreview: (sourceKey: String, targetKey: String?, isGrouping: Boolean) -> Unit,
    onDragFinished: (committed: Boolean) -> Unit,
) {
    var isDragging by remember(item.key) { mutableStateOf(false) }
    var dragTranslation by remember(item.key) { mutableStateOf(Offset.Zero) }
    val editDragModifier = if (isEditMode) {
        Modifier.editDragHandle(
            sourceKey = item.key,
            displayItems = displayItems,
            visibleItemsInfoProvider = visibleItemsInfoProvider,
            onDragStateChange = { isDragging = it },
            onDragTranslationChange = { dragTranslation = it },
            onDragStart = onDragStart,
            onDragPreview = onDragPreview,
            onDragCommit = { sourceKey, targetKey, isGrouping ->
                if (isGrouping) {
                    onItemDrop(sourceKey, targetKey)
                } else {
                    onMoveItem(sourceKey, targetKey)
                }
                onDragFinished(true)
            },
            onDragCancel = {
                onDragFinished(false)
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(editDragModifier)
            .zIndex(
                when {
                    isDragging -> 2f
                    isGroupDropTarget -> 1f
                    else -> 0f
                },
            )
            .graphicsLayer {
                translationX = dragTranslation.x
                translationY = dragTranslation.y
                scaleX = if (isDragging || isGroupDropTarget || isSwapDropTarget) 1.04f else 1f
                scaleY = if (isDragging || isGroupDropTarget || isSwapDropTarget) 1.04f else 1f
                shadowElevation = if (isDragging) 16f else 0f
            },
    ) {
        when (item) {
            is OverviewDisplayItem.Group -> {
                val isExpanded = item.isExpanded
                val accentColor = item.group.accentColor()
                if (isExpanded) {
                    ExpandedLightGroupCard(
                        group = item.group,
                        entities = item.entities,
                        accentColor = accentColor,
                        isAnchorRightColumn = displayItems.collapsedColumnOf(index) == 1,
                        isEditMode = isEditMode,
                        onExpandedChange = { expanded -> onGroupExpandedChange(item.group.id, expanded) },
                        onToggleGroup = { onToggleLightGroup(item.group.id) },
                        onGroupBrightnessChange = { brightness, immediate ->
                            onGroupBrightnessChange(item.group.id, brightness, immediate)
                        },
                        onOpenGroupDetail = { onOpenGroupDetail(item.group) },
                        onEditGroup = { onEditGroup(item.group) },
                        onDeleteGroup = { onDeleteLightGroup(item.group.id) },
                        onToggleEntity = onToggleEntity,
                        onEntityBrightnessChange = onBrightnessChange,
                        onOpenEntityDetail = onOpenEntityDetail,
                        onRemoveEntityFromGroup = { entityId -> onRemoveEntityFromGroup(item.group.id, entityId) },
                        borrowedNeighbor = borrowedNeighbor,
                        displayedAsLightEntityIds = displayedAsLightEntityIds,
                        onTriggerAutomation = onTriggerAutomation,
                        onSetFanSpeed = onSetFanSpeed,
                        onSetCoverPosition = onSetCoverPosition,
                        onStopCover = onStopCover,
                        onCycleClimateHvacMode = onCycleClimateHvacMode,
                        onSetClimateTemperature = onSetClimateTemperature,
                        onTogglePlayback = onTogglePlayback,
                        onSetMediaVolume = onSetMediaVolume,
                        onSkipToPreviousTrack = onSkipToPreviousTrack,
                        onSkipToNextTrack = onSkipToNextTrack,
                        onSetHumidifierHumidity = onSetHumidifierHumidity,
                        onCycleHumidifierMode = onCycleHumidifierMode,
                    )
                } else {
                    LightGroupCard(
                        group = item.group,
                        entities = item.entities,
                        isExpanded = isExpanded,
                        accentColor = accentColor,
                        onExpandedChange = { onGroupExpandedChange(item.group.id, it) },
                        onToggle = { onToggleLightGroup(item.group.id) },
                        onBrightnessChange = { brightness, immediate ->
                            onGroupBrightnessChange(item.group.id, brightness, immediate)
                        },
                        onOpenDetail = { onOpenGroupDetail(item.group) },
                        enabled = !isEditMode,
                    )
                    if (isEditMode) {
                        GroupEditOverlay(
                            onEdit = { onEditGroup(item.group) },
                            onDelete = { onDeleteLightGroup(item.group.id) },
                        )
                    }
                }
            }

            is OverviewDisplayItem.EntityItem -> OverviewEntityItemContent(
                entity = item.entity,
                isEditMode = isEditMode,
                displayedAsLightEntityIds = displayedAsLightEntityIds,
                onToggleEntity = onToggleEntity,
                onBrightnessChange = onBrightnessChange,
                onOpenEntityDetail = onOpenEntityDetail,
                onTriggerAutomation = onTriggerAutomation,
                onSetFanSpeed = onSetFanSpeed,
                onSetCoverPosition = onSetCoverPosition,
                onStopCover = onStopCover,
                onCycleClimateHvacMode = onCycleClimateHvacMode,
                onSetClimateTemperature = onSetClimateTemperature,
                onTogglePlayback = onTogglePlayback,
                onSetMediaVolume = onSetMediaVolume,
                onSkipToPreviousTrack = onSkipToPreviousTrack,
                onSkipToNextTrack = onSkipToNextTrack,
                onSetHumidifierHumidity = onSetHumidifierHumidity,
                onCycleHumidifierMode = onCycleHumidifierMode,
            )
        }
        if (isGroupDropTarget) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        2.dp,
                        LocalHAColorScheme.current.colorFillLightLoudResting.copy(
                            alpha = LIGHT_PICKER_SELECTED_CONTAINER_ALPHA,
                        ),
                        RoundedCornerShape(18.dp),
                    ),
            )
        } else if (isSwapDropTarget) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, LocalHAColorScheme.current.colorFillPrimaryLoudResting, RoundedCornerShape(18.dp)),
            )
        }
    }
}

/**
 * Renders a single non-group [entity] as the domain-appropriate card, dispatching on
 * [Entity.domain] (and, for switches, `device_class`). Shared between the top-level overview grid
 * and [ExpandedLightGroupCard]'s borrowed-neighbor trailing slot, so a card looks and behaves
 * identically whether it occupies its own grid cell or a group's trailing slot.
 */
@Composable
private fun OverviewEntityItemContent(
    entity: Entity,
    isEditMode: Boolean,
    displayedAsLightEntityIds: Set<String>,
    onToggleEntity: (String) -> Unit,
    onBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onOpenEntityDetail: (Entity) -> Unit,
    onTriggerAutomation: (String) -> Unit,
    onSetFanSpeed: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onSetCoverPosition: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onStopCover: (String) -> Unit,
    onCycleClimateHvacMode: (String) -> Unit,
    onSetClimateTemperature: (entityId: String, temperature: Float, immediate: Boolean) -> Unit,
    onTogglePlayback: (String) -> Unit,
    onSetMediaVolume: (entityId: String, volume: Float, immediate: Boolean) -> Unit,
    onSkipToPreviousTrack: (String) -> Unit,
    onSkipToNextTrack: (String) -> Unit,
    onSetHumidifierHumidity: (entityId: String, humidity: Float, immediate: Boolean) -> Unit,
    onCycleHumidifierMode: (entityId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceClass = entity.attributes["device_class"] as? String
    when {
        entity.domain == "light" -> LightEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onBrightnessChange = { brightness, immediate ->
                onBrightnessChange(entity.entityId, brightness, immediate)
            },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "switch" && deviceClass == "outlet" -> OutletEntityCard(
            entity = entity,
            displayedAsLight = entity.entityId in displayedAsLightEntityIds,
            onToggle = { onToggleEntity(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "automation" -> AutomationEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            onTriggerNow = { onTriggerAutomation(entity.entityId) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "lock" -> LockEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "fan" -> FanEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onSpeedChange = { percentage, immediate ->
                onSetFanSpeed(entity.entityId, percentage, immediate)
            },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "cover" -> CoverEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onPositionChange = { percentage, immediate ->
                onSetCoverPosition(entity.entityId, percentage, immediate)
            },
            onStop = { onStopCover(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "climate" -> ClimateEntityCard(
            entity = entity,
            onCycleHvacMode = { onCycleClimateHvacMode(entity.entityId) },
            onSetTemperature = { temperature, immediate ->
                onSetClimateTemperature(entity.entityId, temperature, immediate)
            },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "media_player" -> MediaPlayerEntityCard(
            entity = entity,
            onTogglePlayback = { onTogglePlayback(entity.entityId) },
            onSetVolume = { volume, immediate ->
                onSetMediaVolume(entity.entityId, volume, immediate)
            },
            onSkipToPreviousTrack = { onSkipToPreviousTrack(entity.entityId) },
            onSkipToNextTrack = { onSkipToNextTrack(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.domain == "humidifier" -> HumidifierEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onSetHumidity = { humidity, immediate ->
                onSetHumidifierHumidity(entity.entityId, humidity, immediate)
            },
            onCycleMode = { onCycleHumidifierMode(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        else -> GenericEntityCard(
            entity = entity,
            onToggle = { onToggleEntity(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )
    }
}

/**
 * An L-shaped outline for [ExpandedLightGroupCard]'s tinted highlight background when the group
 * has a trailing partial row: full width down to [stepYPx] (the top of that partial row), then only
 * the left half (the real member's cell) down to the bottom, leaving the right half of the partial
 * row uncovered for a borrowed neighbor (or empty space) to read as clearly outside the group.
 *
 * The step is a smooth concave curve rather than a sharp corner: convex corners place the arc's
 * center inset toward the interior, as usual, while the one reflex corner — where the right edge
 * of the partial row meets its left-half boundary — places the arc's center in the mirror
 * position, inset into the *excluded* quadrant instead of the interior, which "scoops" the curve
 * concave instead of bulging it outward.
 *
 * The shape is always drawn one [GroupHighlightBleed] larger than the member cells it wraps (see
 * the draw site in [ExpandedLightGroupCard]), so a single [cornerRadiusPx] of cell radius + bleed
 * makes every corner — convex and concave alike — concentric with the card corner it wraps: each
 * arc's center coincides with the wrapped card corner's own center, keeping the tint band the same
 * width through the corners as along the straight edges instead of pinching or gapping there.
 */
private class GroupHighlightShape(private val stepYPx: Float, private val cornerRadiusPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val halfW = w / 2f
        val r = cornerRadiusPx.coerceIn(0f, minOf(halfW / 2f, stepYPx, h - stepYPx))
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), startAngleDegrees = 270f, sweepAngleDegrees = 90f, forceMoveTo = false)
            lineTo(w, stepYPx - r)
            arcTo(
                Rect(w - 2 * r, stepYPx - 2 * r, w, stepYPx),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(halfW + r, stepYPx)
            arcTo(
                Rect(halfW, stepYPx, halfW + 2 * r, stepYPx + 2 * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false,
            )
            lineTo(halfW, h - r)
            arcTo(
                Rect(halfW - 2 * r, h - 2 * r, halfW, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false)
            lineTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * The expanded state of a [LightGroupCard]: the group controller stays anchored in the grid's
 * first cell (matching its collapsed single-cell footprint and bearing the collapse chevron),
 * sharing its row with the first member entity, followed by the remaining group members in a
 * 2-column grid of [LightEntityCard]s. A shared rounded surface behind the whole stack visually
 * unifies the group, standing in for the Figma "group highlight surface".
 *
 * [isAnchorRightColumn] mirrors the Figma reference's two documented expansion directions, both
 * anchored on the controller's original collapsed column: when the controller was in the grid's
 * left column, it stays on the left and the layout grows right-then-down (the "Reading Lamp"
 * example); when it was in the right column, it stays on the right of the first row instead (the
 * "Office Lamps" example) so the controller never visually jumps to a different side than where
 * it was collapsed. The remaining members still fill left-then-right beneath either way.
 *
 * When [entities] has an even count, the last row has only one real member: the trailing cell is
 * filled by [borrowedNeighbor] — the next real, non-group item from the overview grid, rendered as
 * a fully interactive card via [OverviewEntityItemContent] — instead of stretching the lone member
 * or leaving a gap. Because that cell isn't part of the group, the tinted highlight background is
 * clipped to an L-shape that hugs only the real member rows via [GroupHighlightShape], with a
 * smooth concave corner at the inward step, so the borrowed card reads as clearly separate. If no
 * neighbor is available to borrow (e.g. the group is last in the whole list), the trailing cell is
 * simply left empty but the background still steps around it the same way.
 *
 * A per-group "vertical only" mode — where an unrelated neighboring card keeps occupying the space
 * beside the *whole* growing column via true masonry packing, instead of only the trailing partial
 * row — is deferred; this grid item always claims the full row width (see
 * `GridItemSpan(maxLineSpan)` at the call site), and that mode would need a staggered grid rather
 * than [LazyVerticalGrid]'s fixed-row model — out of scope for this slice.
 */
@Composable
@VisibleForTesting
internal fun ExpandedLightGroupCard(
    group: OverviewLightGroup,
    entities: List<Entity>,
    accentColor: Color,
    isAnchorRightColumn: Boolean,
    isEditMode: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleGroup: () -> Unit,
    onGroupBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onOpenGroupDetail: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onToggleEntity: (String) -> Unit,
    onEntityBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onOpenEntityDetail: (Entity) -> Unit,
    onRemoveEntityFromGroup: (entityId: String) -> Unit,
    borrowedNeighbor: Entity?,
    displayedAsLightEntityIds: Set<String>,
    onTriggerAutomation: (String) -> Unit,
    onSetFanSpeed: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onSetCoverPosition: (entityId: String, percentage: Float, immediate: Boolean) -> Unit,
    onStopCover: (String) -> Unit,
    onCycleClimateHvacMode: (String) -> Unit,
    onSetClimateTemperature: (entityId: String, temperature: Float, immediate: Boolean) -> Unit,
    onTogglePlayback: (String) -> Unit,
    onSetMediaVolume: (entityId: String, volume: Float, immediate: Boolean) -> Unit,
    onSkipToPreviousTrack: (String) -> Unit,
    onSkipToNextTrack: (String) -> Unit,
    onSetHumidifierHumidity: (entityId: String, humidity: Float, immediate: Boolean) -> Unit,
    onCycleHumidifierMode: (entityId: String) -> Unit,
) {
    val density = LocalDensity.current
    val hasPartialTrailingRow = entities.isNotEmpty() && entities.size % 2 == 0
    // The shared highlight frame is a soft, low-opacity wash of the group's accent so it reads as a
    // subtle backing zone, while the controller and member cards carry the accent at full strength
    // (per the Figma reference: light tinted frame, fully accented cards). The borrowed neighbor
    // keeps its own domain color and sits outside the frame, so it stands out without extra styling.
    val tint = accentColor.copy(alpha = GROUP_HIGHLIGHT_TINT_ALPHA)
    val memberAccentColor = accentColor
    // Outline drawn around the group's controller (main) card while expanded so it's obvious which
    // cell drives the whole group — the controller and its member lights otherwise share the same
    // accent fill. Uses the primary text color so the ring stays legible over both the accent-filled
    // (on) and dark (off) controller states, in light and dark themes alike.
    val controllerOutlineColor = LocalHAColorScheme.current.colorTextPrimary
    var completeRowsHeightPx by remember { mutableIntStateOf(0) }

    // The member cells sit exactly where ordinary grid cells would (no extra padding around them),
    // and the tint is drawn as pure overdraw bleeding [GroupHighlightBleed] past the item bounds on
    // every side — halfway into the surrounding 8dp grid gaps. Card-to-card distance therefore stays
    // the grid's uniform 8dp everywhere: between a member card and any outside card it splits as 4dp
    // tint + 4dp background, instead of the highlight adding its own margin on top of the grid gap
    // and breaking the grid's rhythm (and misaligning member cards against the outside columns).
    val highlightShape: Shape = if (hasPartialTrailingRow && completeRowsHeightPx > 0) {
        GroupHighlightShape(
            stepYPx = completeRowsHeightPx + with(density) { OverviewCardGap.toPx() },
            cornerRadiusPx = with(density) { (OverviewCardCornerRadius + GroupHighlightBleed).toPx() },
        )
    } else {
        RoundedCornerShape(OverviewCardCornerRadius + GroupHighlightBleed)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val bleedPx = GroupHighlightBleed.toPx()
                val outline = highlightShape.createOutline(
                    size = Size(size.width + 2 * bleedPx, size.height + 2 * bleedPx),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                translate(left = -bleedPx, top = -bleedPx) {
                    drawOutline(outline = outline, color = tint)
                }
            },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OverviewCardGap),
        ) {
            val completeRows = entities.drop(1).chunked(2).let { if (hasPartialTrailingRow) it.dropLast(1) else it }
            val firstRowMember = entities.firstOrNull()
            val controller: @Composable RowScope.() -> Unit = {
                Box(modifier = Modifier.weight(1f)) {
                    LightGroupCard(
                        group = group,
                        entities = entities,
                        isExpanded = true,
                        accentColor = memberAccentColor,
                        onExpandedChange = onExpandedChange,
                        onToggle = onToggleGroup,
                        onBrightnessChange = onGroupBrightnessChange,
                        onOpenDetail = onOpenGroupDetail,
                        enabled = !isEditMode,
                    )
                    if (isEditMode) {
                        GroupEditOverlay(onEdit = onEditGroup, onDelete = onDeleteGroup)
                    }
                    // Drawn last (on top of the card) so the ring stays visible over the accent
                    // brightness fill; a plain border-only Box does not intercept taps, so the
                    // controller card underneath stays fully interactive.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(width = 2.dp, color = controllerOutlineColor, shape = OverviewCardShape),
                    )
                }
            }
            val firstMember: @Composable RowScope.() -> Unit = {
                if (firstRowMember != null) {
                    LightEntityCard(
                        entity = firstRowMember,
                        onToggle = { onToggleEntity(firstRowMember.entityId) },
                        onBrightnessChange = { brightness, immediate ->
                            onEntityBrightnessChange(firstRowMember.entityId, brightness, immediate)
                        },
                        onOpenDetail = { onOpenEntityDetail(firstRowMember) },
                        enabled = !isEditMode,
                        accentColor = memberAccentColor,
                        modifier = Modifier
                            .weight(1f)
                            .dragOutToRemoveFromGroup(
                                entityId = firstRowMember.entityId,
                                enabled = isEditMode,
                                onRemove = { onRemoveEntityFromGroup(firstRowMember.entityId) },
                            ),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { completeRowsHeightPx = it.height },
                verticalArrangement = Arrangement.spacedBy(OverviewCardGap),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OverviewCardGap),
                ) {
                    if (isAnchorRightColumn) {
                        firstMember()
                        controller()
                    } else {
                        controller()
                        firstMember()
                    }
                }
                completeRows.forEach { rowEntities ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OverviewCardGap),
                    ) {
                        rowEntities.forEach { entity ->
                            LightEntityCard(
                                entity = entity,
                                onToggle = { onToggleEntity(entity.entityId) },
                                onBrightnessChange = { brightness, immediate ->
                                    onEntityBrightnessChange(entity.entityId, brightness, immediate)
                                },
                                onOpenDetail = { onOpenEntityDetail(entity) },
                                enabled = !isEditMode,
                                accentColor = memberAccentColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .dragOutToRemoveFromGroup(
                                        entityId = entity.entityId,
                                        enabled = isEditMode,
                                        onRemove = { onRemoveEntityFromGroup(entity.entityId) },
                                    ),
                            )
                        }
                    }
                }
            }
            if (hasPartialTrailingRow) {
                val lastMember = entities.last()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OverviewCardGap),
                ) {
                    LightEntityCard(
                        entity = lastMember,
                        onToggle = { onToggleEntity(lastMember.entityId) },
                        onBrightnessChange = { brightness, immediate ->
                            onEntityBrightnessChange(lastMember.entityId, brightness, immediate)
                        },
                        onOpenDetail = { onOpenEntityDetail(lastMember) },
                        enabled = !isEditMode,
                        accentColor = memberAccentColor,
                        modifier = Modifier
                            .weight(1f)
                            .dragOutToRemoveFromGroup(
                                entityId = lastMember.entityId,
                                enabled = isEditMode,
                                onRemove = { onRemoveEntityFromGroup(lastMember.entityId) },
                            ),
                    )
                    if (borrowedNeighbor != null) {
                        OverviewEntityItemContent(
                            entity = borrowedNeighbor,
                            isEditMode = isEditMode,
                            displayedAsLightEntityIds = displayedAsLightEntityIds,
                            onToggleEntity = onToggleEntity,
                            onBrightnessChange = onEntityBrightnessChange,
                            onOpenEntityDetail = onOpenEntityDetail,
                            onTriggerAutomation = onTriggerAutomation,
                            onSetFanSpeed = onSetFanSpeed,
                            onSetCoverPosition = onSetCoverPosition,
                            onStopCover = onStopCover,
                            onCycleClimateHvacMode = onCycleClimateHvacMode,
                            onSetClimateTemperature = onSetClimateTemperature,
                            onTogglePlayback = onTogglePlayback,
                            onSetMediaVolume = onSetMediaVolume,
                            onSkipToPreviousTrack = onSkipToPreviousTrack,
                            onSkipToNextTrack = onSkipToNextTrack,
                            onSetHumidifierHumidity = onSetHumidifierHumidity,
                            onCycleHumidifierMode = onCycleHumidifierMode,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupEditOverlay(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .padding(4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        OverviewEditIconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(commonR.string.edit))
        }
        OverviewEditIconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(commonR.string.delete))
        }
    }
}

@Composable
private fun OverviewEditIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalHAColorScheme.current
    FilledTonalIconButton(
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = colors.colorSurfaceDefault.copy(alpha = 0.92f),
            contentColor = colors.colorTextPrimary,
        ),
        onClick = onClick,
        content = content,
    )
}

/**
 * Wires up the long-press-drag gesture used to reorder or group-merge grid items in edit mode.
 *
 * [pointerInput] only restarts its gesture-detection coroutine when [sourceKey] changes, which for
 * most items is rare (an item's key persists across recompositions unless it changes group
 * membership). Everything else this function receives — [displayItems] most importantly — is
 * captured by the coroutine's closure at whatever point [sourceKey] last changed, so without
 * [rememberUpdatedState] a long-lived item's gesture would keep evaluating merge/reorder decisions
 * against a stale snapshot of the grid (e.g. missing a group created after that point). Wrapping
 * every captured value in [rememberUpdatedState] keeps the coroutine itself alive (no gesture
 * restart mid-drag) while always reading the latest value.
 */
@Composable
private fun Modifier.editDragHandle(
    sourceKey: String,
    displayItems: List<OverviewDisplayItem>,
    visibleItemsInfoProvider: () -> List<LazyGridItemInfo>,
    onDragStateChange: (Boolean) -> Unit,
    onDragTranslationChange: (Offset) -> Unit,
    onDragStart: (sourceKey: String) -> Unit,
    onDragPreview: (sourceKey: String, targetKey: String?, isGrouping: Boolean) -> Unit,
    onDragCommit: (sourceKey: String, targetKey: String, isGrouping: Boolean) -> Unit,
    onDragCancel: () -> Unit,
): Modifier {
    val currentDisplayItems by rememberUpdatedState(displayItems)
    val currentVisibleItemsInfoProvider by rememberUpdatedState(visibleItemsInfoProvider)
    val currentOnDragStateChange by rememberUpdatedState(onDragStateChange)
    val currentOnDragTranslationChange by rememberUpdatedState(onDragTranslationChange)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragPreview by rememberUpdatedState(onDragPreview)
    val currentOnDragCommit by rememberUpdatedState(onDragCommit)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    return pointerInput(sourceKey) {
        var dragOffset = Offset.Zero
        var dragStartCenter = Offset.Zero
        var latestDrop: GridDropTarget? = null
        detectDragGesturesAfterLongPress(
            onDragStart = {
                dragOffset = Offset.Zero
                dragStartCenter = currentVisibleItemsInfoProvider()
                    .firstOrNull { it.key == sourceKey }
                    ?.centerOffset()
                    ?: Offset.Zero
                latestDrop = null
                currentOnDragStateChange(true)
                currentOnDragTranslationChange(Offset.Zero)
                currentOnDragStart(sourceKey)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                dragOffset += dragAmount
                val visibleItems = currentVisibleItemsInfoProvider()
                val dropPoint = dragStartCenter + dragOffset
                latestDrop = visibleItems.targetDropFor(sourceKey, dropPoint)
                val targetKey = latestDrop?.key
                val isGrouping = targetKey != null &&
                    currentDisplayItems.isGroupDrop(sourceKey, targetKey, latestDrop?.isCenterDrop == true)
                currentOnDragPreview(sourceKey, targetKey, isGrouping)
                val currentCenter = visibleItems.firstOrNull { it.key == sourceKey }?.centerOffset()
                currentOnDragTranslationChange(
                    if (currentCenter != null) {
                        dropPoint - currentCenter
                    } else {
                        dragOffset
                    },
                )
            },
            onDragEnd = {
                currentOnDragStateChange(false)
                currentOnDragTranslationChange(Offset.Zero)
                val targetKey = latestDrop?.key
                if (targetKey != null && targetKey != sourceKey) {
                    val isGrouping =
                        currentDisplayItems.isGroupDrop(sourceKey, targetKey, latestDrop?.isCenterDrop == true)
                    currentOnDragPreview(sourceKey, null, false)
                    currentOnDragCommit(sourceKey, targetKey, isGrouping)
                } else {
                    currentOnDragPreview(sourceKey, null, false)
                    currentOnDragCancel()
                }
            },
            onDragCancel = {
                dragOffset = Offset.Zero
                currentOnDragStateChange(false)
                currentOnDragTranslationChange(Offset.Zero)
                currentOnDragPreview(sourceKey, null, false)
                currentOnDragCancel()
            },
        )
    }
}

private data class GridDropTarget(val key: String, val isCenterDrop: Boolean)

/**
 * A local, self-contained long-press-drag gesture for a single group member card that removes it
 * from the group when dragged past [GROUP_MEMBER_REMOVE_DRAG_THRESHOLD] on either axis. Unlike
 * [editDragHandle], this is deliberately NOT wired into the grid-level [LazyGridItemInfo] system:
 * member cards rendered inside an expanded group have no corresponding grid item, so there is
 * nothing for a grid-level drag to reference. [pointerInput] is keyed on [entityId] (matching the
 * idiom already used by [LightEntityCard]'s own internal gesture), not the enclosing group, so the
 * gesture is not restarted by unrelated group changes.
 */
@Composable
private fun Modifier.dragOutToRemoveFromGroup(entityId: String, enabled: Boolean, onRemove: () -> Unit): Modifier {
    if (!enabled) return this
    var dragOffset by remember(entityId) { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val currentOnRemove by rememberUpdatedState(onRemove)
    val thresholdPx = with(LocalDensity.current) { GROUP_MEMBER_REMOVE_DRAG_THRESHOLD.toPx() }
    return this
        .graphicsLayer {
            val dragFraction = (maxOf(abs(dragOffset.x), abs(dragOffset.y)) / thresholdPx).coerceIn(0f, 1f)
            translationX = dragOffset.x
            translationY = dragOffset.y
            alpha = 1f - dragFraction * 0.5f
            scaleX = 1f - dragFraction * 0.1f
            scaleY = 1f - dragFraction * 0.1f
        }
        .pointerInput(entityId) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragOffset = Offset.Zero
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                },
                onDragEnd = {
                    val shouldRemove = abs(dragOffset.x) > thresholdPx || abs(dragOffset.y) > thresholdPx
                    dragOffset = Offset.Zero
                    if (shouldRemove) currentOnRemove()
                },
                onDragCancel = {
                    dragOffset = Offset.Zero
                },
            )
        }
}

private val GROUP_MEMBER_REMOVE_DRAG_THRESHOLD = 72.dp

private fun List<LazyGridItemInfo>.targetDropFor(sourceKey: String, dropPoint: Offset): GridDropTarget? {
    val target = firstOrNull { item ->
        item.key != sourceKey &&
            dropPoint.x >= item.offset.x &&
            dropPoint.x <= item.offset.x + item.size.width &&
            dropPoint.y >= item.offset.y &&
            dropPoint.y <= item.offset.y + item.size.height
    } ?: return null
    val relativeX = (dropPoint.x - target.offset.x) / target.size.width.toFloat()
    val relativeY = (dropPoint.y - target.offset.y) / target.size.height.toFloat()
    val targetKey = target.key as? String ?: return null
    return GridDropTarget(
        key = targetKey,
        isCenterDrop = relativeX in 0.25f..0.75f && relativeY in 0.25f..0.75f,
    )
}

private fun LazyGridItemInfo.centerOffset(): Offset = Offset(
    x = offset.x + size.width / 2f,
    y = offset.y + size.height / 2f,
)

private fun List<OverviewDisplayItem>.shouldGroupDrop(sourceKey: String, targetKey: String): Boolean {
    val source = firstOrNull { it.key == sourceKey }
    val target = firstOrNull { it.key == targetKey }
    return when {
        source is OverviewDisplayItem.EntityItem && target is OverviewDisplayItem.EntityItem ->
            source.entity.domain == "light" && target.entity.domain == "light"
        source is OverviewDisplayItem.EntityItem && target is OverviewDisplayItem.Group ->
            source.entity.domain == "light"
        source is OverviewDisplayItem.Group && target is OverviewDisplayItem.EntityItem ->
            target.entity.domain == "light"
        else -> false
    }
}

/**
 * Whether a drop onto [targetKey] should merge [sourceKey] into a light group instead of
 * reordering it into that position. A center drop merges (matching the "drop onto" affordance
 * used across the grid); a drop nearer the target's edge reorders instead, exactly like the
 * light-on-light case. This applies uniformly regardless of whether a [OverviewDisplayItem.Group]
 * is the source or the target, so an existing group can still be repositioned by dragging it to
 * the edge of another cell instead of always merging that cell's light into the group.
 */
private fun List<OverviewDisplayItem>.isGroupDrop(
    sourceKey: String,
    targetKey: String,
    isCenterDrop: Boolean,
): Boolean = isCenterDrop && shouldGroupDrop(sourceKey, targetKey)

private const val LIGHT_PICKER_SELECTED_CONTAINER_ALPHA = 0.22f

// Opacity of the expanded group's shared highlight frame. Kept low so the frame reads as a soft
// backing wash behind the fully-accented controller/member cards, matching the Figma reference's
// light tinted frame rather than competing with the cards' full-strength accent color.
private const val GROUP_HIGHLIGHT_TINT_ALPHA = 0.3f

// How far the expanded group's highlight bleeds past the group's item bounds into the surrounding
// grid gaps — exactly half of [OverviewCardGap]. The member cells sit at ordinary grid positions, so
// this bleed keeps the card-to-card rhythm uniform across the highlight's edge: half-gap of tint plus
// half-gap of plain background add up to the same gap that separates any two ordinary cards (per the
// Figma reference, whose highlight margin is likewise half its grid gap).
private val GroupHighlightBleed = OverviewCardGap / 2f

// Kept as a plain ARGB literal (rather than an HAColorScheme token) since it's persisted as the
// group's stored color and read outside of composition. Deliberately distinct from
// HAColorScheme's light/loud-resting accent used by standalone light cards, per the Figma
// reference: an uncustomized group still needs its own default tint so it visually reads as a
// group control rather than an indistinguishable single light.
private const val DEFAULT_GROUP_ACCENT_ARGB = 0xFFFFA000L
private val GroupColorOptions = listOf(
    DEFAULT_GROUP_ACCENT_ARGB,
    0xFF42A5F5L,
    0xFF66BB6AL,
    0xFFAB47BCL,
    0xFFFF7043L,
    0xFF78909CL,
)

private fun OverviewLightGroup.accentColor(): Color = Color(colorArgb ?: DEFAULT_GROUP_ACCENT_ARGB)

private const val GRID_COLUMN_COUNT = 2

/**
 * The column (0-based) [targetIndex] would occupy in the 2-column [GridCells.Fixed] grid if it
 * rendered as a normal single-cell item, mirroring [LazyVerticalGrid]'s own span-packing: a
 * full-width ([GRID_COLUMN_COUNT]-span) item always starts and ends its own row, resetting the
 * column cursor to 0 for whatever follows it.
 *
 * Used to recover the "home" column a [OverviewDisplayItem.Group] was anchored to before it
 * expanded to a full-width span, so [ExpandedLightGroupCard] can keep its controller on the same
 * side, per the Figma reference's "controller stays anchored in its original grid cell" behavior.
 */
private fun List<OverviewDisplayItem>.collapsedColumnOf(targetIndex: Int): Int {
    var column = 0
    for (i in 0 until targetIndex) {
        val isFullWidth = (this[i] as? OverviewDisplayItem.Group)?.isExpanded == true
        column = if (isFullWidth) 0 else (column + 1) % GRID_COLUMN_COUNT
    }
    return column
}

/** The result of [withBorrowedNeighbors]: the top-level items to render as grid cells, plus which
 * entity (if any) each expanded group borrowed into its trailing slot. */
private data class BorrowedNeighbors(val items: List<OverviewDisplayItem>, val byGroupKey: Map<String, Entity>)

/**
 * An expanded, default-style group whose member count is even always leaves exactly one lone
 * member in its trailing row (the controller's row already consumes one member, so an even total
 * leaves an odd, non-doubling remainder). Rather than stretch that lone member to fill the row or
 * leave the slot empty, this borrows the very next [OverviewDisplayItem.EntityItem] in list order
 * into that slot: it's removed from the top-level list returned here (so [LazyVerticalGrid] no
 * longer renders it as its own cell) and recorded in [BorrowedNeighbors.byGroupKey] so
 * [ExpandedLightGroupCard] can render it as a real, fully interactive card, clearly outside the
 * group's own tinted background.
 *
 * A neighboring [OverviewDisplayItem.Group] is deliberately never borrowed — a collapsed or
 * expanded group needs its own full cell/row, not a half-width trailing slot. If no eligible
 * neighbor exists (the group is the last item, or the next item is a group), the slot is simply
 * left empty; this is recomputed from scratch on every recomposition, so it always reflects the
 * current [displayItems] order.
 *
 * A borrowed item has no top-level grid cell for as long as it's borrowed, so it's temporarily not
 * reachable through [editDragHandle]'s [LazyGridItemInfo]-based drag system — an accepted
 * limitation, since the grid reflows and it becomes draggable again as soon as it's no longer the
 * borrowed item for that row.
 */
private fun List<OverviewDisplayItem>.withBorrowedNeighbors(): BorrowedNeighbors {
    val borrowedKeys = mutableSetOf<String>()
    val byGroupKey = mutableMapOf<String, Entity>()
    forEachIndexed { index, item ->
        val hasTrailingSlot = item is OverviewDisplayItem.Group && item.isExpanded && item.entities.size % 2 == 0
        if (!hasTrailingSlot) return@forEachIndexed
        val neighbor = getOrNull(index + 1)
        if (neighbor is OverviewDisplayItem.EntityItem && neighbor.key !in borrowedKeys) {
            byGroupKey[item.key] = neighbor.entity
            borrowedKeys += neighbor.key
        }
    }
    return BorrowedNeighbors(items = filterNot { it.key in borrowedKeys }, byGroupKey = byGroupKey)
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
private fun LightGroupEditorDialog(
    group: OverviewLightGroup?,
    lightEntities: List<Entity>,
    onDismiss: () -> Unit,
    onSave: (groupId: String?, name: String, entityIds: List<String>, colorArgb: Long?) -> Unit,
) {
    var name by remember(group?.id) { mutableStateOf(group?.name.orEmpty()) }
    var selectedEntityIds by remember(group?.id) { mutableStateOf(group?.entityIds?.toSet().orEmpty()) }
    var selectedColorArgb by remember(group?.id) { mutableStateOf(group?.colorArgb ?: DEFAULT_GROUP_ACCENT_ARGB) }
    val canSave = name.isNotBlank() && selectedEntityIds.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (group ==
                        null
                    ) {
                        commonR.string.overview_create_group
                    } else {
                        commonR.string.overview_edit_group
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = if (it.isBlank()) "" else it },
                    label = { Text(stringResource(commonR.string.overview_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(commonR.string.overview_group_color),
                        color = LocalHAColorScheme.current.colorTextPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GroupColorOptions.forEach { colorArgb ->
                            val selected = selectedColorArgb == colorArgb
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(colorArgb), RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) {
                                            LocalHAColorScheme.current.colorTextPrimary
                                        } else {
                                            LocalHAColorScheme.current.colorTextDisabled.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable { selectedColorArgb = colorArgb },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(commonR.string.overview_group_members),
                        color = LocalHAColorScheme.current.colorTextPrimary,
                    )
                    Text(
                        text = stringResource(commonR.string.overview_selected_lights, selectedEntityIds.size),
                        color = LocalHAColorScheme.current.colorTextSecondary,
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    items(lightEntities, key = { it.entityId }) { entity ->
                        val selected = entity.entityId in selectedEntityIds
                        LightGroupPickerEntityCard(
                            entity = entity,
                            selected = selected,
                            onClick = {
                                selectedEntityIds = if (selected) {
                                    selectedEntityIds - entity.entityId
                                } else {
                                    selectedEntityIds + entity.entityId
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(group?.id, name, selectedEntityIds.toList(), selectedColorArgb) },
            ) {
                Text(stringResource(commonR.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(commonR.string.cancel))
            }
        },
        containerColor = LocalHAColorScheme.current.colorSurfaceDefault,
        titleContentColor = LocalHAColorScheme.current.colorTextPrimary,
        textContentColor = LocalHAColorScheme.current.colorTextPrimary,
    )
}

@Composable
private fun LightGroupPickerEntityCard(
    entity: Entity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = LocalHAColorScheme.current.colorFillLightLoudResting
    Box(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
    ) {
        LightEntityCard(
            entity = entity,
            onToggle = {},
            onBrightnessChange = { _, _ -> },
            onOpenDetail = {},
            modifier = Modifier
                .matchParentSize(),
            enabled = false,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        accentColor.copy(alpha = LIGHT_PICKER_SELECTED_CONTAINER_ALPHA),
                        RoundedCornerShape(18.dp),
                    )
                    .border(2.dp, accentColor, RoundedCornerShape(18.dp)),
            )
        }
    }
}

private sealed interface OverviewDisplayItem {
    val key: String

    data class Group(val group: OverviewLightGroup, val entities: List<Entity>, val isExpanded: Boolean) :
        OverviewDisplayItem {
        override val key = "${OverviewViewModel.GROUP_ITEM_PREFIX}${group.id}"
    }

    data class EntityItem(val entity: Entity) : OverviewDisplayItem {
        override val key = "${OverviewViewModel.ENTITY_ITEM_PREFIX}${entity.entityId}"
    }
}

private val OverviewDisplayItem.canBeGrouped: Boolean
    get() = when (this) {
        is OverviewDisplayItem.Group -> true
        is OverviewDisplayItem.EntityItem -> entity.domain == "light"
    }

private val OverviewDisplayItem.canAcceptGroupDrop: Boolean
    get() = when (this) {
        is OverviewDisplayItem.Group -> true
        is OverviewDisplayItem.EntityItem -> entity.domain == "light"
    }

private fun OverviewUiState.Success.toDisplayItems(): List<OverviewDisplayItem> {
    val entityById = entities.associateBy { it.entityId }
    val groupById = lightGroups.associateBy { it.id }
    val groupedEntityIds = lightGroups.flatMap { it.entityIds }.toSet()
    val emittedEntityIds = mutableSetOf<String>()
    return buildList {
        itemOrder.forEach { key ->
            when {
                key.startsWith(OverviewViewModel.GROUP_ITEM_PREFIX) -> {
                    val groupId = key.removePrefix(OverviewViewModel.GROUP_ITEM_PREFIX)
                    val group = groupById[groupId] ?: return@forEach
                    val members = group.entityIds.mapNotNull { entityById[it] }
                    if (members.isEmpty()) return@forEach
                    val isExpanded = groupId in expandedLightGroupIds
                    add(OverviewDisplayItem.Group(group, members, isExpanded))
                    emittedEntityIds += group.entityIds
                }

                key.startsWith(OverviewViewModel.ENTITY_ITEM_PREFIX) -> {
                    val entityId = key.removePrefix(OverviewViewModel.ENTITY_ITEM_PREFIX)
                    if (entityId !in groupedEntityIds && entityId !in emittedEntityIds) {
                        entityById[entityId]?.let { add(OverviewDisplayItem.EntityItem(it)) }
                    }
                }

                else -> Unit
            }
        }
    }
}

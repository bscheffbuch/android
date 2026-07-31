package io.homeassistant.companion.android.overview.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.overview.OverviewLightGroup
import io.homeassistant.companion.android.overview.OverviewUiState
import io.homeassistant.companion.android.overview.OverviewViewModel
import io.homeassistant.companion.android.overview.isOutletSwitch
import io.homeassistant.companion.android.overview.supportsDisplayAsLight
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
    onMoveGroupMember: (groupId: String, fromEntityId: String, toEntityId: String) -> Unit,
    onMoveEntityIntoGroup: (entityId: String, groupId: String, targetEntityId: String) -> Unit,
    onMoveEntityOutOfGroup: (groupId: String, entityId: String, targetKey: String) -> Unit,
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
        // The app bar's nested-scroll connection is applied INSIDE OverviewGrid (directly on the
        // grid, inner to the pull-to-refresh), not here on the Scaffold. Otherwise pull-to-refresh
        // sits inner to the app bar and eats the pull-down-at-top gesture that the collapsed
        // LargeTopAppBar needs to re-expand, leaving the header stuck collapsed ("can't scroll up").
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
                    scrollBehavior = scrollBehavior,
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
                    onMoveGroupMember = onMoveGroupMember,
                    onMoveEntityIntoGroup = onMoveEntityIntoGroup,
                    onMoveEntityOutOfGroup = onMoveEntityOutOfGroup,
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
    scrollBehavior: TopAppBarScrollBehavior,
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
    onMoveGroupMember: (groupId: String, fromEntityId: String, toEntityId: String) -> Unit,
    onMoveEntityIntoGroup: (entityId: String, groupId: String, targetEntityId: String) -> Unit,
    onMoveEntityOutOfGroup: (groupId: String, entityId: String, targetKey: String) -> Unit,
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
    // One drag session for the whole grid. Owned here (the stable container) so a card can be carried
    // across the top-level/group boundary without its per-card gesture being torn down mid-drag.
    val dragState = remember { OverviewDragState() }
    val borrowedNeighbors = remember(displayItems) { displayItems.withBorrowedNeighbors() }
    val entityById = remember(borrowedNeighbors) { borrowedNeighbors.items.draggableEntityLookup() }
    val visibleItemsInfoProvider = { lazyGridState.layoutInfo.visibleItemsInfo }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) dragState.reset()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Root-space origin of this box: lets the drag machinery convert the grid's
                // viewport-relative cell offsets and the finger position into one shared space.
                .onGloballyPositioned { dragState.containerOrigin = it.positionInRoot() }
                .overviewDragDetector(
                    enabled = isEditMode,
                    dragState = dragState,
                    displayItems = borrowedNeighbors.items,
                    entityById = entityById,
                    visibleItemsInfoProvider = visibleItemsInfoProvider,
                    onMoveItem = onMoveItem,
                    onMoveGroupMember = onMoveGroupMember,
                    onMoveEntityIntoGroup = onMoveEntityIntoGroup,
                    onMoveEntityOutOfGroup = onMoveEntityOutOfGroup,
                    onMergeCommit = onItemDrop,
                    onRemoveEntityFromGroup = onRemoveEntityFromGroup,
                ),
        ) {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(OverviewCardGap),
                horizontalArrangement = Arrangement.spacedBy(OverviewCardGap),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        dragState = dragState,
                        isEditMode = isEditMode,
                        isMergeTarget = item.key == dragState.mergeTargetKey,
                        // The dragged item is drawn by the floating overlay, so its own cell renders as
                        // an invisible placeholder that just holds the grid gap open. Every OTHER card
                        // keeps animateItem() so it slides as the live shuffle reorders the grid.
                        modifier = if (item.key == dragState.draggedKey) Modifier else Modifier.animateItem(),
                        onToggleEntity = onToggleEntity,
                        onToggleLightGroup = onToggleLightGroup,
                        onBrightnessChange = onBrightnessChange,
                        onGroupBrightnessChange = onGroupBrightnessChange,
                        onOpenEntityDetail = onOpenEntityDetail,
                        onOpenGroupDetail = onOpenGroupDetail,
                        onEditGroup = onEditGroup,
                        onDeleteLightGroup = onDeleteLightGroup,
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
                    )
                }
            }
            DraggedCardOverlay(
                dragState = dragState,
                displayItems = borrowedNeighbors.items,
                entityById = entityById,
                displayedAsLightEntityIds = displayedAsLightEntityIds,
            )
        }
    }
}

@Composable
private fun OverviewGridItem(
    item: OverviewDisplayItem,
    index: Int,
    displayItems: List<OverviewDisplayItem>,
    borrowedNeighbor: Entity?,
    dragState: OverviewDragState,
    isEditMode: Boolean,
    isMergeTarget: Boolean,
    modifier: Modifier = Modifier,
    onToggleEntity: (String) -> Unit,
    onToggleLightGroup: (String) -> Unit,
    onBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onGroupBrightnessChange: (groupId: String, brightness: Float, immediate: Boolean) -> Unit,
    onOpenEntityDetail: (Entity) -> Unit,
    onOpenGroupDetail: (OverviewLightGroup) -> Unit,
    onEditGroup: (OverviewLightGroup) -> Unit,
    onDeleteLightGroup: (String) -> Unit,
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
) {
    // The dragged card is drawn by the floating overlay ([DraggedCardOverlay]), so its home cell
    // renders invisibly (alpha 0) and just holds the grid gap open — the live shuffle moves that gap
    // around. A card the drag is hovering as a merge target swells slightly; springing it reads as
    // "dynamic".
    val isDragged = item.key == dragState.draggedKey
    val mergeSwell by animateFloatAsState(
        targetValue = if (isMergeTarget) DROP_TARGET_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "merge_target_swell",
    )

    Box(
        modifier = modifier
            .zIndex(if (isMergeTarget) 1f else 0f)
            .graphicsLayer {
                scaleX = mergeSwell
                scaleY = mergeSwell
                alpha = if (isDragged) 0f else 1f
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
                        dragState = dragState,
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
        if (isMergeTarget) {
            MergeTargetIndicator()
        }
    }
}

// Fill opacity of the drop-target preview overlay — enough to clearly mark the landing cell while
// still letting the card underneath read through.
private const val DROP_TARGET_FILL_ALPHA = 0.22f

/**
 * The "merge here" preview drawn over the light (or group) cell a drag is hovering dead-centre: a
 * filled, outlined wash of the group accent plus a centred plus badge, so the user sees both *where*
 * the two lights will combine and *that* dropping groups them. Reordering has no indicator of its
 * own — the live shuffle physically opening the landing slot is the feedback.
 */
@Composable
private fun BoxScope.MergeTargetIndicator() {
    val colors = LocalHAColorScheme.current
    val accent = colors.colorFillLightLoudResting
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(accent.copy(alpha = DROP_TARGET_FILL_ALPHA), RoundedCornerShape(18.dp))
            .border(2.dp, accent, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.colorSurfaceDefault, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
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

        // Any on/off entity the user chose to show as a lamp (switch, outlet, or input_boolean)
        // renders through the outlet card in its light treatment: warm fill and a bulb icon.
        entity.supportsDisplayAsLight() && entity.entityId in displayedAsLightEntityIds -> OutletEntityCard(
            entity = entity,
            displayedAsLight = true,
            onToggle = { onToggleEntity(entity.entityId) },
            onOpenDetail = { onOpenEntityDetail(entity) },
            enabled = !isEditMode,
            modifier = modifier,
        )

        entity.isOutletSwitch() -> OutletEntityCard(
            entity = entity,
            displayedAsLight = false,
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
    dragState: OverviewDragState = remember { OverviewDragState() },
    onExpandedChange: (Boolean) -> Unit,
    onToggleGroup: () -> Unit,
    onGroupBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onOpenGroupDetail: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onToggleEntity: (String) -> Unit,
    onEntityBrightnessChange: (entityId: String, brightness: Float, immediate: Boolean) -> Unit,
    onOpenEntityDetail: (Entity) -> Unit,
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

    // Members register with the shared, screen-level [dragState] and are rendered through their own
    // movable content so their placement animation follows them as a live reorder relocates them
    // between rows — a plain positional card would lose that state and teleport instead of sliding.
    // The group's highlight bounds are unregistered when it collapses so a stale rectangle can't keep
    // reading as a live "inside this group" drop zone. Callbacks are read through rememberUpdatedState
    // refs so this map, created once per member *set*, always calls the latest lambdas without being
    // recreated on every recomposition (which would defeat the identity preservation).
    DisposableEffect(group.id) {
        onDispose { dragState.groupContentBounds.remove(group.id) }
    }
    val latestOnToggleEntity = rememberUpdatedState(onToggleEntity)
    val latestOnEntityBrightnessChange = rememberUpdatedState(onEntityBrightnessChange)
    val latestOnOpenEntityDetail = rememberUpdatedState(onOpenEntityDetail)
    val latestAccentColor = rememberUpdatedState(memberAccentColor)
    val latestIsEditMode = rememberUpdatedState(isEditMode)
    val memberCards = remember(entities.map { it.entityId }.toSet(), dragState) {
        entities.associate { member ->
            member.entityId to movableContentOf<Entity, Modifier> { entity, cardModifier ->
                GroupMemberCard(
                    entity = entity,
                    groupId = group.id,
                    accentColor = latestAccentColor.value,
                    isEditMode = latestIsEditMode.value,
                    dragState = dragState,
                    onToggle = { latestOnToggleEntity.value(entity.entityId) },
                    onBrightnessChange = { brightness, immediate ->
                        latestOnEntityBrightnessChange.value(entity.entityId, brightness, immediate)
                    },
                    onOpenDetail = { latestOnOpenEntityDetail.value(entity) },
                    modifier = cardModifier,
                )
            }
        }
    }

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
            // Root-space bounds of the whole group: the "inside = reorder among members / insert here,
            // outside = remove" zone the shared drag machinery hit-tests against.
            .onGloballyPositioned { dragState.groupContentBounds[group.id] = it.boundsInRoot() }
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
                    memberCards[firstRowMember.entityId]?.invoke(firstRowMember, Modifier.weight(1f))
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
                            memberCards[entity.entityId]?.invoke(entity, Modifier.weight(1f))
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
                    memberCards[lastMember.entityId]?.invoke(lastMember, Modifier.weight(1f))
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
 * Shared, grid-level state for the whole overview's drag-and-drop, replacing the old per-card handles.
 * Held by [OverviewGrid] (a stable container that outlives any individual card) so a single drag can
 * carry a card across the top-level/group boundary — reordering it in the grid, pulling a member out
 * of a group, or dropping a light into one — without its gesture being torn down when its membership
 * (and therefore its host composable) changes mid-drag.
 *
 * All rectangles and points are in **root space**. [containerOrigin] is the drag container's root
 * top-left, used to translate the lazy grid's viewport-relative cell offsets into root space and to
 * place the floating overlay back into container space. [groupContentBounds] and [memberBounds] are
 * the settled rectangles the finger is hit-tested against; [draggedKey]/[fingerRoot]/[grabOffset]/
 * [draggedSize] describe the active drag so the overlay can follow the finger; [mergeTargetKey] marks
 * a cell a centre-hover would merge into on release; [inRemoveZone] marks that releasing now would
 * pull the dragged member out of its group.
 */
internal class OverviewDragState {
    var draggedKey by mutableStateOf<String?>(null)
    var fingerRoot by mutableStateOf(Offset.Zero)
    var grabOffset by mutableStateOf(Offset.Zero)
    var draggedSize by mutableStateOf(Size.Zero)
    var containerOrigin by mutableStateOf(Offset.Zero)
    var mergeTargetKey by mutableStateOf<String?>(null)
    var inRemoveZone by mutableStateOf(false)
    val groupContentBounds = mutableStateMapOf<String, Rect>()
    val memberBounds = mutableStateMapOf<String, MemberSlot>()

    fun reset() {
        draggedKey = null
        mergeTargetKey = null
        inRemoveZone = false
    }
}

/** A settled light-group member cell: its owning [groupId], its [entityId], and its root-space [rect]. */
internal data class MemberSlot(val groupId: String, val entityId: String, val rect: Rect)

/** A card the drag just picked up: its display [key] and its root-space [rect]. */
private data class GrabbedCard(val key: String, val rect: Rect)

/**
 * Where the finger currently sits during a drag, resolved live so cross-boundary moves can be applied
 * as it moves. [id] is a stable identity so a stationary hover fires a move only once, not every frame.
 */
private sealed interface DropZone {
    val id: String

    data object None : DropZone {
        override val id = "none"
    }

    /** Directly over member [entityId] of expanded group [groupId] — insert or reorder there. */
    data class Member(val groupId: String, val entityId: String) : DropZone {
        override val id = "member:$groupId:$entityId"
    }

    /**
     * Inside expanded group [groupId]'s frame but not over any specific member — the inter-member
     * gaps, the controller, and the trailing slot. A stable hold zone: the order is left untouched
     * while the finger hovers this dead space, so a drag can't thrash the arrangement.
     */
    data class GroupInterior(val groupId: String) : DropZone {
        override val id = "groupinterior:$groupId"
    }

    /** Over top-level cell [key]; [isCenter] is a centre-hover (merge affordance) vs an edge (reorder). */
    data class TopLevel(val key: String, val isCenter: Boolean) : DropZone {
        override val id = "top:$key:${if (isCenter) "center" else "edge"}"
    }
}

/** The top-level display key ("entity:<id>") for an entity, matching [OverviewDisplayItem.EntityItem]. */
private fun entityItemKey(entityId: String): String = "${OverviewViewModel.ENTITY_ITEM_PREFIX}$entityId"

/** The entity id encoded in this display key, or null when it is a group (or otherwise non-entity) key. */
private fun String.entityIdOrNull(): String? =
    if (startsWith(OverviewViewModel.ENTITY_ITEM_PREFIX)) removePrefix(OverviewViewModel.ENTITY_ITEM_PREFIX) else null

/** The id of the expanded group currently listing [entityId] as a member, or null if it is top-level. */
private fun List<OverviewDisplayItem>.groupIdContaining(entityId: String): String? =
    filterIsInstance<OverviewDisplayItem.Group>()
        .firstOrNull { group -> group.entities.any { it.entityId == entityId } }
        ?.group
        ?.id

/**
 * Every draggable entity (top-level items and expanded-group members) keyed by its display key, so the
 * floating overlay can render a faithful copy of whichever card is being dragged.
 */
private fun List<OverviewDisplayItem>.draggableEntityLookup(): Map<String, Entity> = buildMap {
    this@draggableEntityLookup.forEach { item ->
        when (item) {
            is OverviewDisplayItem.EntityItem -> put(item.key, item.entity)
            is OverviewDisplayItem.Group -> item.entities.forEach { put(entityItemKey(it.entityId), it) }
        }
    }
}

/** This lazy grid cell's rectangle in root space (its viewport-relative offset shifted by [origin]). */
private fun LazyGridItemInfo.rootRect(origin: Offset): Rect {
    val topLeft = origin + Offset(offset.x.toFloat(), offset.y.toFloat())
    return Rect(topLeft, Size(size.width.toFloat(), size.height.toFloat()))
}

/**
 * Finds which draggable card sits under [fingerRoot] at pick-up. Expanded-group members are checked
 * before top-level cells, so grabbing a member picks up that member while grabbing the controller (or
 * any non-member part of the group cell) picks up the whole group.
 */
private fun OverviewDragState.grabbedCardAt(fingerRoot: Offset, visibleItems: List<LazyGridItemInfo>): GrabbedCard? {
    memberBounds.values.firstOrNull { it.rect.contains(fingerRoot) }?.let {
        return GrabbedCard(key = entityItemKey(it.entityId), rect = it.rect)
    }
    visibleItems.firstOrNull { it.rootRect(containerOrigin).contains(fingerRoot) }?.let { info ->
        val key = info.key as? String ?: return null
        return GrabbedCard(key = key, rect = info.rootRect(containerOrigin))
    }
    return null
}

/**
 * Resolves [fingerRoot] to a [DropZone]. When the dragged entity can join a light group
 * ([draggedIsLight]) and the finger is inside an expanded group's frame, it resolves to the member
 * whose cell actually **contains** the finger (or [DropZone.GroupInterior] over the dead space between
 * them). Using contains rather than nearest-member is deliberate: it leaves the inter-member gaps,
 * controller, and trailing slot as a dead zone so the order doesn't flip back and forth as the cards
 * reflow under a hovering finger. Otherwise the group is treated as an ordinary top-level cell so a
 * group (or a non-light) can still be reordered past it. The dragged card's own slot is skipped.
 */
private fun OverviewDragState.dropZoneFor(
    fingerRoot: Offset,
    visibleItems: List<LazyGridItemInfo>,
    draggedKey: String,
    draggedIsLight: Boolean,
): DropZone {
    if (draggedIsLight) {
        val group = groupContentBounds.entries.firstOrNull { it.value.contains(fingerRoot) }
        if (group != null) {
            val over = memberBounds.values.firstOrNull {
                it.groupId == group.key &&
                    entityItemKey(it.entityId) != draggedKey &&
                    it.rect.contains(fingerRoot)
            }
            return if (over != null) DropZone.Member(group.key, over.entityId) else DropZone.GroupInterior(group.key)
        }
    }
    val cell = visibleItems.firstOrNull {
        it.key != draggedKey && it.rootRect(containerOrigin).contains(fingerRoot)
    } ?: return DropZone.None
    val key = cell.key as? String ?: return DropZone.None
    val rect = cell.rootRect(containerOrigin)
    val relX = (fingerRoot.x - rect.left) / rect.width
    val relY = (fingerRoot.y - rect.top) / rect.height
    return DropZone.TopLevel(key = key, isCenter = relX in 0.25f..0.75f && relY in 0.25f..0.75f)
}

/**
 * A single light-group member cell. Outside edit mode it is an ordinary interactive [LightEntityCard];
 * in edit mode it registers with the shared [dragState] (so the screen-level gesture can pick it up
 * and hit-test drops against it), slides between rows as a live reorder relocates it, and renders
 * invisibly while it is the card being dragged (the floating overlay draws the lifted copy instead).
 */
@Composable
private fun GroupMemberCard(
    entity: Entity,
    groupId: String,
    accentColor: Color,
    isEditMode: Boolean,
    dragState: OverviewDragState,
    onToggle: () -> Unit,
    onBrightnessChange: (brightness: Float, immediate: Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightEntityCard(
        entity = entity,
        onToggle = onToggle,
        onBrightnessChange = onBrightnessChange,
        onOpenDetail = onOpenDetail,
        enabled = !isEditMode,
        accentColor = accentColor,
        modifier = modifier.groupMemberSlot(
            entityId = entity.entityId,
            groupId = groupId,
            enabled = isEditMode,
            dragState = dragState,
        ),
    )
}

/**
 * Registers a light-group member with the shared [dragState] and animates it between its settled
 * positions as a live reorder moves it (standing in for [LazyVerticalGrid]'s `animateItem()` inside
 * [ExpandedLightGroupCard]'s hand-built, non-lazy layout). While this member is the one being dragged
 * it renders invisibly, since [DraggedCardOverlay] draws the lifted card instead. Its root-space
 * bounds are unregistered when it leaves so a stale rectangle can't keep reading as a live drop
 * target. Outside edit mode ([enabled] = false) it adds nothing.
 */
@Composable
private fun Modifier.groupMemberSlot(
    entityId: String,
    groupId: String,
    enabled: Boolean,
    dragState: OverviewDragState,
): Modifier {
    if (!enabled) return this
    val isDragged = dragState.draggedKey == entityItemKey(entityId)
    DisposableEffect(entityId) {
        onDispose { dragState.memberBounds.remove(entityId) }
    }
    return this
        // Records the settled home cell in root space (before animateMemberPlacement's own offset, so
        // it reports the placed slot, not the mid-slide position). Used to pick the member up and to
        // hit-test drops onto it.
        .onGloballyPositioned {
            dragState.memberBounds[entityId] =
                MemberSlot(groupId = groupId, entityId = entityId, rect = it.boundsInRoot())
        }
        .animateMemberPlacement(
            enabled = !isDragged,
            groupOrigin = { dragState.groupContentBounds[groupId]?.topLeft },
        )
        .graphicsLayer { alpha = if (isDragged) 0f else 1f }
}

/**
 * The single, screen-level long-press-drag gesture for the whole overview grid. Because this node
 * lives on the stable grid container it survives every membership change a drag triggers, which is
 * what lets one continuous gesture carry a card across the top-level/group boundary.
 *
 * On long-press it hit-tests [OverviewDragState.grabbedCardAt] to pick up whichever card is under the
 * finger. As the finger moves it resolves a [DropZone] and applies the matching move **live** — a
 * top-level reorder ([onMoveItem]), a within-group reorder ([onMoveGroupMember]), pulling a member out
 * to the grid ([onMoveEntityOutOfGroup]), or dropping a light into a group ([onMoveEntityIntoGroup]) —
 * so the grid and groups shuffle under the drag. A move fires only when the resolved zone's identity
 * changes, so a stationary hover doesn't thrash. Merging is destructive, so a centre-hover over a
 * light (or a collapsed group) only previews the "＋" affordance and commits via [onMergeCommit] on
 * release; dragging a member out into empty space arms removal ([onRemoveEntityFromGroup]).
 *
 * Everything captured is read through [rememberUpdatedState] so the gesture coroutine — kept alive for
 * the whole session by `pointerInput(Unit)` — always sees the latest grid snapshot without restarting.
 */
@Composable
private fun Modifier.overviewDragDetector(
    enabled: Boolean,
    dragState: OverviewDragState,
    displayItems: List<OverviewDisplayItem>,
    entityById: Map<String, Entity>,
    visibleItemsInfoProvider: () -> List<LazyGridItemInfo>,
    onMoveItem: (fromKey: String, toKey: String) -> Unit,
    onMoveGroupMember: (groupId: String, fromEntityId: String, toEntityId: String) -> Unit,
    onMoveEntityIntoGroup: (entityId: String, groupId: String, targetEntityId: String) -> Unit,
    onMoveEntityOutOfGroup: (groupId: String, entityId: String, targetKey: String) -> Unit,
    onMergeCommit: (sourceKey: String, targetKey: String) -> Unit,
    onRemoveEntityFromGroup: (groupId: String, entityId: String) -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val currentDisplayItems by rememberUpdatedState(displayItems)
    val currentEntityById by rememberUpdatedState(entityById)
    val currentVisibleItems by rememberUpdatedState(visibleItemsInfoProvider)
    val currentOnMoveItem by rememberUpdatedState(onMoveItem)
    val currentOnMoveGroupMember by rememberUpdatedState(onMoveGroupMember)
    val currentOnMoveEntityIntoGroup by rememberUpdatedState(onMoveEntityIntoGroup)
    val currentOnMoveEntityOutOfGroup by rememberUpdatedState(onMoveEntityOutOfGroup)
    val currentOnMergeCommit by rememberUpdatedState(onMergeCommit)
    val currentOnRemoveEntityFromGroup by rememberUpdatedState(onRemoveEntityFromGroup)
    if (!enabled) return this
    return this.pointerInput(Unit) {
        // The zone the dragged card was last moved onto; a move fires only when this changes, so a
        // stationary hover over one cell doesn't re-shuffle every frame.
        var lastZoneId: String? = null
        detectDragGesturesAfterLongPress(
            onDragStart = { localPos ->
                val fingerRoot = dragState.containerOrigin + localPos
                val grabbed = dragState.grabbedCardAt(fingerRoot, currentVisibleItems())
                if (grabbed != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragState.draggedKey = grabbed.key
                    dragState.draggedSize = grabbed.rect.size
                    dragState.grabOffset = fingerRoot - grabbed.rect.topLeft
                    dragState.fingerRoot = fingerRoot
                    dragState.mergeTargetKey = null
                    dragState.inRemoveZone = false
                    lastZoneId = null
                } else {
                    dragState.reset()
                }
            },
            onDrag = { change, dragAmount ->
                val draggedKey = dragState.draggedKey
                if (draggedKey != null) {
                    change.consume()
                    dragState.fingerRoot += dragAmount
                    val entityId = draggedKey.entityIdOrNull()
                    val zone = dragState.dropZoneFor(
                        fingerRoot = dragState.fingerRoot,
                        visibleItems = currentVisibleItems(),
                        draggedKey = draggedKey,
                        draggedIsLight = currentEntityById[draggedKey]?.domain == "light",
                    )
                    if (zone.id != lastZoneId) {
                        lastZoneId = zone.id
                        val sourceGroupId = entityId?.let { currentDisplayItems.groupIdContaining(it) }
                        when (zone) {
                            is DropZone.Member -> {
                                dragState.mergeTargetKey = null
                                dragState.inRemoveZone = false
                                if (entityId != null && zone.entityId != entityId) {
                                    if (sourceGroupId == zone.groupId) {
                                        currentOnMoveGroupMember(zone.groupId, entityId, zone.entityId)
                                    } else {
                                        currentOnMoveEntityIntoGroup(entityId, zone.groupId, zone.entityId)
                                    }
                                }
                            }

                            is DropZone.GroupInterior -> {
                                // Hovering a group's dead space (gaps / controller / trailing): hold the
                                // current order and don't arm removal — the finger is still inside a group.
                                dragState.mergeTargetKey = null
                                dragState.inRemoveZone = false
                            }

                            is DropZone.TopLevel -> {
                                dragState.inRemoveZone = false
                                val mergeable = sourceGroupId == null &&
                                    zone.isCenter &&
                                    currentDisplayItems.isGroupDrop(draggedKey, zone.key, isCenterDrop = true)
                                if (mergeable) {
                                    dragState.mergeTargetKey = zone.key
                                } else {
                                    dragState.mergeTargetKey = null
                                    if (zone.key != draggedKey) {
                                        if (entityId != null && sourceGroupId != null) {
                                            currentOnMoveEntityOutOfGroup(sourceGroupId, entityId, zone.key)
                                        } else {
                                            currentOnMoveItem(draggedKey, zone.key)
                                        }
                                    }
                                }
                            }

                            is DropZone.None -> {
                                dragState.mergeTargetKey = null
                                dragState.inRemoveZone = entityId != null && sourceGroupId != null
                            }
                        }
                    }
                }
            },
            onDragEnd = {
                val draggedKey = dragState.draggedKey
                val entityId = draggedKey?.entityIdOrNull()
                val mergeTarget = dragState.mergeTargetKey
                val removeGroupId = if (dragState.inRemoveZone && entityId != null) {
                    currentDisplayItems.groupIdContaining(entityId)
                } else {
                    null
                }
                if (draggedKey != null && mergeTarget != null) {
                    currentOnMergeCommit(draggedKey, mergeTarget)
                } else if (entityId != null && removeGroupId != null) {
                    currentOnRemoveEntityFromGroup(removeGroupId, entityId)
                }
                dragState.reset()
            },
            onDragCancel = { dragState.reset() },
        )
    }
}

/**
 * The floating card that follows the finger during a drag, drawn on top of the whole grid (a sibling
 * after the [LazyVerticalGrid]) so it reads as lifted above every cell no matter where the dragged
 * item currently lives — its source cell renders invisibly meanwhile. It is a faithful copy of the
 * dragged card (same size and domain rendering) with the usual lift treatment: scale, shadow and a
 * slight tilt, plus a translucency that lets a merge target's "＋" read through it and fades further
 * when releasing would pull a member out of its group.
 */
@Composable
private fun BoxScope.DraggedCardOverlay(
    dragState: OverviewDragState,
    displayItems: List<OverviewDisplayItem>,
    entityById: Map<String, Entity>,
    displayedAsLightEntityIds: Set<String>,
) {
    val draggedKey = dragState.draggedKey ?: return
    val size = dragState.draggedSize
    if (size == Size.Zero) return
    val density = LocalDensity.current
    val defaultLightAccent = LocalHAColorScheme.current.colorFillLightLoudResting

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            dragState.inRemoveZone -> GROUP_MEMBER_REMOVE_ALPHA
            dragState.mergeTargetKey != null -> DRAG_LIFT_ALPHA
            else -> DRAG_OVERLAY_RESTING_ALPHA
        },
        label = "drag_overlay_alpha",
    )

    val draggedGroup = displayItems.filterIsInstance<OverviewDisplayItem.Group>().firstOrNull { it.key == draggedKey }
    val draggedEntity = entityById[draggedKey]
    val memberAccent = draggedKey.entityIdOrNull()?.let { id ->
        displayItems.filterIsInstance<OverviewDisplayItem.Group>()
            .firstOrNull { group -> group.entities.any { it.entityId == id } }
            ?.group
            ?.accentColor()
    }

    Box(
        modifier = Modifier
            .size(
                width = with(density) { size.width.toDp() },
                height = with(density) { size.height.toDp() },
            )
            .graphicsLayer {
                val topLeft = dragState.fingerRoot - dragState.grabOffset - dragState.containerOrigin
                translationX = topLeft.x
                translationY = topLeft.y
                scaleX = DRAG_LIFT_SCALE
                scaleY = DRAG_LIFT_SCALE
                rotationZ = DRAG_LIFT_ROTATION_DEGREES
                shadowElevation = DRAG_LIFT_ELEVATION
                alpha = overlayAlpha
            },
    ) {
        when {
            draggedGroup != null -> LightGroupCard(
                group = draggedGroup.group,
                entities = draggedGroup.entities,
                isExpanded = false,
                accentColor = draggedGroup.group.accentColor(),
                onExpandedChange = {},
                onToggle = {},
                onBrightnessChange = { _, _ -> },
                onOpenDetail = {},
                enabled = false,
            )

            draggedEntity != null && draggedEntity.domain == "light" -> LightEntityCard(
                entity = draggedEntity,
                onToggle = {},
                onBrightnessChange = { _, _ -> },
                onOpenDetail = {},
                enabled = false,
                accentColor = memberAccent ?: defaultLightAccent,
            )

            draggedEntity != null -> OverviewEntityItemContent(
                entity = draggedEntity,
                isEditMode = true,
                displayedAsLightEntityIds = displayedAsLightEntityIds,
                onToggleEntity = {},
                onBrightnessChange = { _, _, _ -> },
                onOpenEntityDetail = {},
                onTriggerAutomation = {},
                onSetFanSpeed = { _, _, _ -> },
                onSetCoverPosition = { _, _, _ -> },
                onStopCover = {},
                onCycleClimateHvacMode = {},
                onSetClimateTemperature = { _, _, _ -> },
                onTogglePlayback = {},
                onSetMediaVolume = { _, _, _ -> },
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onSetHumidifierHumidity = { _, _, _ -> },
                onCycleHumidifierMode = {},
            )
        }
    }
}

/**
 * Animates this card between its settled positions so it slides when a live reorder moves it,
 * standing in for [LazyVerticalGrid]'s `animateItem()` inside [ExpandedLightGroupCard]'s hand-built
 * (non-lazy) layout. [onGloballyPositioned] — placed before the animating [offset], so it reports the
 * settled position rather than the mid-animation one — records the target, and the offset draws the
 * card stepping toward it. The first placement snaps (no intro slide); [enabled] is false for the
 * card currently being dragged, which is glued to the finger instead.
 *
 * Placement is measured **relative to the group's own origin** ([groupOrigin] = the expanded card's
 * root-space top-left) rather than in absolute root space. Scrolling the overview moves the whole
 * group — and every member's root position with it — by the same amount, so a group-relative
 * position is invariant under scroll and the placement spring stays idle. Measuring in absolute root
 * space instead (the previous behavior) made every member chase its own scrolling root position, so
 * the members visibly lagged behind the controller and frame while the list scrolled. Only a real
 * reorder moves a member *relative to* the group, and only that now retriggers the slide.
 *
 * [groupOrigin] returns null until the group has been positioned; while it (or this card's own home)
 * is unknown the card simply sits at its laid-out spot with no offset.
 */
@Composable
private fun Modifier.animateMemberPlacement(enabled: Boolean, groupOrigin: () -> Offset?): Modifier {
    var homeRoot by remember { mutableStateOf<Offset?>(null) }
    var initialized by remember { mutableStateOf(false) }
    val animated = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    LaunchedEffect(Unit) {
        snapshotFlow {
            val home = homeRoot ?: return@snapshotFlow null
            val origin = groupOrigin() ?: return@snapshotFlow null
            home - origin
        }.collect { relative ->
            if (relative == null) return@collect
            if (!initialized) {
                animated.snapTo(relative)
                initialized = true
            } else if (animated.targetValue != relative) {
                animated.animateTo(
                    targetValue = relative,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                )
            }
        }
    }
    return this
        .onGloballyPositioned { homeRoot = it.positionInRoot() }
        .offset {
            val home = homeRoot
            val origin = groupOrigin()
            if (!enabled || !initialized || home == null || origin == null) {
                IntOffset.Zero
            } else {
                (animated.value - (home - origin)).round()
            }
        }
}

// How faded the lifted member card becomes once it has been dragged out past the group's bounds, so
// the user can see that releasing now will remove it from the group rather than just reorder it.
private const val GROUP_MEMBER_REMOVE_ALPHA = 0.3f

// Visual feedback applied to a card while it is being dragged in edit mode, and to a card the drag
// is hovering over as a drop target.
private const val DRAG_LIFT_SCALE = 1.08f
private const val DROP_TARGET_SCALE = 1.04f
private const val DRAG_LIFT_ELEVATION = 16f
private const val DRAG_LIFT_ROTATION_DEGREES = 2f
private const val DRAG_LIFT_ALPHA = 0.55f

// Opacity of the floating drag overlay at rest. Kept nearly opaque so the lifted card reads as solid,
// dropping to [DRAG_LIFT_ALPHA] over a merge target (so its "＋" reads through) and to
// [GROUP_MEMBER_REMOVE_ALPHA] once dragged out far enough that releasing would remove the member.
private const val DRAG_OVERLAY_RESTING_ALPHA = 0.94f

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
 * reachable through the [LazyGridItemInfo]-based hit-testing the drag detector uses — an accepted
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
            // Everything lives in one LazyColumn so the full-colour wheel, presets and the member
            // list scroll together as a single surface (a plain Column can't host the nested member
            // list, and the wheel makes the content taller than the dialog on shorter screens).
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 520.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = if (it.isBlank()) "" else it },
                        label = { Text(stringResource(commonR.string.overview_group_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(commonR.string.overview_group_color),
                                color = LocalHAColorScheme.current.colorTextPrimary,
                            )
                            // Live preview of the picked colour.
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(selectedColorArgb), CircleShape)
                                    .border(
                                        1.dp,
                                        LocalHAColorScheme.current.colorTextDisabled.copy(alpha = 0.4f),
                                        CircleShape,
                                    ),
                            )
                        }
                        ColorWheel(
                            selectedColor = Color(selectedColorArgb),
                            onColorSelected = { selectedColorArgb = it.toArgb().toLong() and 0xFFFFFFFFL },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        // Preset quick-picks for the common accents, kept alongside the wheel.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            GroupColorOptions.forEach { colorArgb ->
                                val selected = selectedColorArgb == colorArgb
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(colorArgb), CircleShape)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) {
                                                LocalHAColorScheme.current.colorTextPrimary
                                            } else {
                                                LocalHAColorScheme.current.colorTextDisabled.copy(alpha = 0.4f)
                                            },
                                            shape = CircleShape,
                                        )
                                        .clickable { selectedColorArgb = colorArgb },
                                )
                            }
                        }
                    }
                }

                item {
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
                }

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

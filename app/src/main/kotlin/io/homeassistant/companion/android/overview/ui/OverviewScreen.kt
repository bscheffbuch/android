package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.overview.OverviewUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    onRefresh: () -> Unit,
    onToggleEntity: (String) -> Unit,
    onBrightnessChange: (entityId: String, brightness: Float) -> Unit,
) {
    val colors = LocalHAColorScheme.current
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    var detailEntity by remember { mutableStateOf<Entity?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(commonR.string.overview_title)) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(commonR.string.refresh),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
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
            is OverviewUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.colorFillPrimaryLoudResting)
                }
            }

            is OverviewUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.message,
                        color = colors.colorOnDangerNormal,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is OverviewUiState.Success -> {
                val isRefreshing = false
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = uiState.entities,
                            key = { it.entityId },
                        ) { entity ->
                            if (entity.domain == "light") {
                                LightEntityCard(
                                    entity = entity,
                                    onToggle = { onToggleEntity(entity.entityId) },
                                    onBrightnessChange = { onBrightnessChange(entity.entityId, it) },
                                    onOpenDetail = { detailEntity = entity },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                GenericEntityCard(
                                    entity = entity,
                                    onToggle = { onToggleEntity(entity.entityId) },
                                    onOpenDetail = { detailEntity = entity },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailEntity?.let { entity ->
        EntityDetailBottomSheet(
            entity = entity,
            onDismiss = { detailEntity = null },
            onBrightnessChange = { brightness ->
                onBrightnessChange(entity.entityId, brightness)
            },
        )
    }
}

package io.homeassistant.companion.android.settings.developer.location.views

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.database.location.LocationHistoryItem
import io.homeassistant.companion.android.database.location.LocationHistoryItemResult
import io.homeassistant.companion.android.database.location.LocationHistoryItemTrigger
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.settings.views.EmptyState
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import java.text.DateFormat
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

@Composable
fun LocationTrackingView(
    useHistory: Boolean,
    onSetHistory: (Boolean) -> Unit,
    history: Flow<PagingData<LocationHistoryItem>>,
    serversList: List<Server>,
    modifier: Modifier = Modifier,
) {
    val historyState = history.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier,
        contentPadding = safeBottomPaddingValues(applyHorizontal = false),
    ) {
        item("history.use") {
            HASettingsCard(
                modifier = Modifier
                    .padding(all = HADimens.SPACE4)
                    .clickable { onSetHistory(!useHistory) },
            ) {
                val historyUseLabel = stringResource(commonR.string.location_history_use)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = historyUseLabel,
                        style = HATextStyle.Body,
                        color = LocalHAColorScheme.current.colorTextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    HASwitch(
                        checked = useHistory,
                        // Handled by row
                        onCheckedChange = {},
                        modifier = Modifier
                            .padding(start = HADimens.SPACE4)
                            .semantics { contentDescription = historyUseLabel },
                    )
                }
            }
        }
        if (!useHistory || (historyState.loadState.refresh !is LoadState.Loading && historyState.itemCount == 0)) {
            item("history.empty") {
                HomeAssistantAppTheme {
                    EmptyState(
                        icon = CommunityMaterial.Icon3.cmd_map_marker_path,
                        title = stringResource(
                            if (useHistory) {
                                commonR.string.location_history_empty_title
                            } else {
                                commonR.string.location_history_off_title
                            },
                        ),
                        subtitle = stringResource(
                            if (useHistory) {
                                commonR.string.location_history_empty_summary
                            } else {
                                commonR.string.location_history_off_summary
                            },
                        ),
                    )
                }
            }
        } else {
            items(
                count = historyState.itemCount,
                key = historyState.itemKey { "history.${it.id}" },
            ) { index ->
                LocationTrackingHistoryRow(item = historyState[index], servers = serversList)
            }
        }
    }
}

@Composable
fun LocationTrackingHistoryRow(item: LocationHistoryItem?, servers: List<Server>, modifier: Modifier = Modifier) {
    var opened by rememberSaveable { mutableStateOf(false) }
    val colorScheme = LocalHAColorScheme.current
    val cornerRadius by animateDpAsState(
        if (opened) HARadius.XL else HARadius.Square,
        label = "HistoryRow corner radius",
    )
    val date by remember(item?.id) {
        mutableStateOf(
            item?.created?.let {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.DEFAULT).apply {
                    timeZone = TimeZone.getDefault()
                }.format(it)
            },
        )
    }

    Column(
        modifier
            .zIndex(if (opened) 1f else 0f)
            .fillMaxWidth()
            .background(
                if (opened) colorScheme.colorSurfaceLow else colorScheme.colorSurfaceDefault,
                RoundedCornerShape(cornerRadius),
            )
            .clickable { opened = !opened }
            .animateContentSize(),
    ) {
        ReadOnlyRow(
            primarySlot = {
                Text(text = date ?: "", style = HATextStyle.Body, color = colorScheme.colorTextPrimary)
            },
            secondarySlot = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item?.let {
                        val sent = it.result == LocationHistoryItemResult.SENT
                        val failed = it.result == LocationHistoryItemResult.FAILED_SEND
                        Text(
                            text = "${stringResource(
                                item.trigger.toStringResource(),
                            )} • ${stringResource(it.result.toStringResource())}",
                            style = HATextStyle.BodyMedium,
                            color = colorScheme.colorTextSecondary,
                            modifier = Modifier.padding(end = HADimens.SPACE1),
                        )
                        Image(
                            asset = when {
                                sent -> CommunityMaterial.Icon.cmd_check
                                failed -> CommunityMaterial.Icon.cmd_alert_outline
                                else -> CommunityMaterial.Icon.cmd_debug_step_over
                            },
                            contentDescription = if (sent ||
                                failed
                            ) {
                                null
                            } else {
                                stringResource(commonR.string.location_history_skipped)
                            },
                            colorFilter = ColorFilter.tint(
                                when {
                                    sent -> colorScheme.colorOnSuccessNormal
                                    failed -> colorScheme.colorOnWarningNormal
                                    else -> colorScheme.colorTextSecondary
                                },
                            ),
                            modifier = Modifier.size(with(LocalDensity.current) { 16.sp.toDp() }),
                        )
                    }
                }
            },
        )
        AnimatedVisibility(visible = opened) {
            val context = LocalContext.current
            val serverName by remember {
                mutableStateOf(
                    if (item?.serverId != null) {
                        servers.firstOrNull { it.id == item.serverId }?.friendlyName
                    } else {
                        "-"
                    },
                )
            }
            Column {
                ReadOnlyRow(
                    primaryText = stringResource(commonR.string.location),
                    secondaryText = (item?.locationName ?: "${item?.latitude}, ${item?.longitude}"),
                )
                ReadOnlyRow(
                    primaryText = stringResource(commonR.string.accuracy),
                    secondaryText = item?.accuracy.toString(),
                )
                if (servers.size > 1 || serverName == null) { // null serverName suggests deleted server
                    ReadOnlyRow(
                        primaryText = stringResource(commonR.string.server),
                        secondaryText = serverName ?: stringResource(commonR.string.state_unknown),
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = HADimens.SPACE4)
                        .padding(bottom = HADimens.SPACE2),
                ) {
                    if (item?.latitude != null && item.longitude != null) {
                        IconButton(
                            onClick = {
                                val latlng = "${item.latitude},${item.longitude}"
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "geo:$latlng?q=$latlng(Home+Assistant)".toUri(),
                                    ),
                                )
                            },
                        ) {
                            Image(
                                asset = CommunityMaterial.Icon3.cmd_map,
                                contentDescription = stringResource(commonR.string.show_on_map),
                                modifier = Modifier.size(HADimens.SPACE6),
                                colorFilter = ColorFilter.tint(colorScheme.colorFillPrimaryLoudResting),
                            )
                        }
                        Spacer(Modifier.width(HADimens.SPACE4))
                    }
                    IconButton(
                        onClick = {
                            ShareCompat.IntentBuilder(context)
                                .setText(item?.toSharingString(serverName) ?: "")
                                .setType("text/plain")
                                .startChooser()
                        },
                    ) {
                        Image(
                            asset = CommunityMaterial.Icon3.cmd_share_variant,
                            contentDescription = stringResource(commonR.string.share_logs),
                            modifier = Modifier.size(HADimens.SPACE6),
                            colorFilter = ColorFilter.tint(colorScheme.colorFillPrimaryLoudResting),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyRow(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    selectingEnabled: Boolean = true,
) = ReadOnlyRow(
    primarySlot = {
        Text(text = primaryText, style = HATextStyle.Body, color = LocalHAColorScheme.current.colorTextPrimary)
    },
    secondarySlot = {
        val secondaryContent: @Composable () -> Unit = {
            Text(
                text = secondaryText,
                style = HATextStyle.BodyMedium,
                color = LocalHAColorScheme.current.colorTextSecondary,
            )
        }
        if (selectingEnabled) {
            SelectionContainer { secondaryContent() }
        } else {
            secondaryContent()
        }
    },
    modifier = modifier,
)

@Composable
fun ReadOnlyRow(
    primarySlot: @Composable () -> Unit,
    secondarySlot: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .heightIn(min = HADimens.SPACE14)
                .padding(all = HADimens.SPACE4),
            verticalArrangement = Arrangement.Center,
        ) {
            primarySlot()
            secondarySlot()
        }
    }
}

private fun LocationHistoryItemResult.toStringResource() = when (this) {
    LocationHistoryItemResult.SKIPPED_ACCURACY -> commonR.string.location_history_skipped_accuracy
    LocationHistoryItemResult.SKIPPED_FUTURE -> commonR.string.location_history_skipped_future
    LocationHistoryItemResult.SKIPPED_NOT_LATEST -> commonR.string.location_history_skipped_not_latest
    LocationHistoryItemResult.SKIPPED_DUPLICATE -> commonR.string.location_history_skipped_duplicate
    LocationHistoryItemResult.SKIPPED_DEBOUNCE -> commonR.string.location_history_skipped_debounce
    LocationHistoryItemResult.SKIPPED_OLD -> commonR.string.location_history_skipped_old
    LocationHistoryItemResult.FAILED_SEND -> commonR.string.location_history_failed_send
    LocationHistoryItemResult.SENT -> commonR.string.location_history_sent
}

private fun LocationHistoryItemTrigger.toStringResource() = when (this) {
    LocationHistoryItemTrigger.FLP_BACKGROUND -> commonR.string.basic_sensor_name_location_background
    LocationHistoryItemTrigger.FLP_FOREGROUND -> commonR.string.basic_sensor_name_high_accuracy_mode
    LocationHistoryItemTrigger.GEOFENCE_ENTER -> commonR.string.location_history_geofence_enter
    LocationHistoryItemTrigger.GEOFENCE_EXIT -> commonR.string.location_history_geofence_exit
    LocationHistoryItemTrigger.GEOFENCE_DWELL -> commonR.string.location_history_geofence_dwell
    LocationHistoryItemTrigger.SINGLE_ACCURATE_LOCATION -> commonR.string.basic_sensor_name_location_accurate
    LocationHistoryItemTrigger.UNKNOWN -> commonR.string.state_unknown
}

package io.homeassistant.companion.android.settings.notification.views

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.util.appCreatedChannels
import io.homeassistant.companion.android.settings.notification.NotificationViewModel
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import io.homeassistant.companion.android.util.safeBottomWindowInsets
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationChannelView(notificationViewModel: NotificationViewModel, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(safeBottomWindowInsets(applyHorizontal = false)),
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            contentPadding = PaddingValues(all = HADimens.SPACE4) + safeBottomPaddingValues(applyHorizontal = false),
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.notification_channels_description),
                    style = HATextStyle.Body,
                    color = LocalHAColorScheme.current.colorTextSecondary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = HADimens.SPACE5),
                )
            }

            items(notificationViewModel.channelList, key = { it.id }) { channel ->
                NotificationChannelRow(
                    name = channel.name.toString().take(30),
                    canDelete = channel.id !in appCreatedChannels,
                    onEditClicked = { notificationViewModel.editChannelDetails(channel.id) },
                    onDeleteClicked = {
                        notificationViewModel.deleteChannel(channel.id)
                        notificationViewModel.updateChannelList()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.notification_channel_deleted, channel.name),
                                actionLabel = context.getString(R.string.undo),
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                notificationViewModel.createChannel(channel)
                                notificationViewModel.updateChannelList()
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = HADimens.SPACE1),
                )
            }
        }
    }
}

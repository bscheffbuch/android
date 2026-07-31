package io.homeassistant.companion.android.settings.notification.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme

/**
 * A single notification channel row, showing the channel [name] plus two independent trailing
 * actions: edit (always shown) and delete (only shown when [canDelete] is true, i.e. the channel
 * was not created by the app itself).
 *
 * Unlike [io.homeassistant.companion.android.common.compose.composable.HASettingsRow], this row
 * has no single click target; each action is its own tap target.
 *
 * @param name The channel's display name.
 * @param canDelete Whether the delete action should be shown for this channel.
 * @param onEditClicked Called when the edit icon is clicked.
 * @param onDeleteClicked Called when the delete icon is clicked. Only reachable when [canDelete] is true.
 */
@Composable
fun NotificationChannelRow(
    name: String,
    canDelete: Boolean,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalHAColorScheme.current
    HASettingsCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HADimens.SPACE10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE4),
        ) {
            Text(
                text = name,
                style = HATextStyle.Body,
                color = colorScheme.colorTextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEditClicked) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit_channel),
                    tint = colorScheme.colorTextPrimary,
                )
            }
            if (canDelete) {
                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete_channel),
                        tint = colorScheme.colorTextPrimary,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NotificationChannelRowPreview() {
    HAThemeForPreview {
        NotificationChannelRow(
            name = "Server-created channel",
            canDelete = true,
            onEditClicked = {},
            onDeleteClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun NotificationChannelRowAppCreatedPreview() {
    HAThemeForPreview {
        NotificationChannelRow(
            name = "App-created channel",
            canDelete = false,
            onEditClicked = {},
            onDeleteClicked = {},
        )
    }
}

package io.homeassistant.companion.android.settings.server

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAHorizontalDivider
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.compose.ModalBottomSheet

@Composable
fun ServerChooserView(servers: List<Server>, onServerSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    // ModalBottomSheet is a legacy Material 2 composable shared with several other consumers
    // (EntityPicker, AssistSheetView, ImprovSheetView, ...), so it is nested-wrapped rather than
    // migrated here.
    HomeAssistantAppTheme {
        ModalBottomSheet(
            modifier = modifier,
            title = stringResource(commonR.string.server_select),
        ) {
            servers.forEach {
                ServerChooserRow(server = it, onServerSelected = onServerSelected)
            }
            Spacer(modifier = Modifier.height(HADimens.SPACE4))
        }
    }
}

@Composable
fun ServerChooserRow(server: Server, onServerSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = LocalHAColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HADimens.SPACE14)
            .clickable { onServerSelected(server.id) }
            .padding(horizontal = HADimens.SPACE4),
    ) {
        Text(
            text = server.friendlyName,
            style = HATextStyle.Body,
            color = colorScheme.colorTextPrimary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
            contentDescription = null,
            tint = colorScheme.colorTextPrimary,
            modifier = Modifier
                .size(HADimens.SPACE6)
                .padding(HADimens.SPACE1),
        )
    }
    HAHorizontalDivider(modifier = Modifier.padding(horizontal = HADimens.SPACE4))
}

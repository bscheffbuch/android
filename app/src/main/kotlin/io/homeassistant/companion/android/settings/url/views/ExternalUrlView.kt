package io.homeassistant.companion.android.settings.url.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@Composable
fun ExternalUrlView(
    canUseCloud: Boolean,
    useCloud: Boolean,
    externalUrl: String?,
    onUseCloudToggle: (Boolean) -> Unit,
    onExternalUrlSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .padding(safeBottomPaddingValues(applyHorizontal = false))
            .padding(vertical = HADimens.SPACE4),
    ) {
        if (canUseCloud) {
            ExternalUrlCloudView(
                useCloud = useCloud,
                onUseCloudToggle = onUseCloudToggle,
                modifier = Modifier.padding(horizontal = HADimens.SPACE4),
            )
            Spacer(modifier = Modifier.height(HADimens.SPACE6))
        }

        if (!canUseCloud || !useCloud) {
            ExternalUrlInputView(
                url = externalUrl,
                focusRequester = focusRequester,
                onSaveUrl = onExternalUrlSaved,
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!canUseCloud) focusRequester.requestFocus()
    }
}

@Composable
fun ExternalUrlCloudView(useCloud: Boolean, onUseCloudToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = LocalHAColorScheme.current

    HASettingsCard(
        modifier = modifier
            .clip(RoundedCornerShape(HARadius.XL))
            .clickable(role = Role.Switch) { onUseCloudToggle(!useCloud) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(commonR.string.input_cloud),
                style = HATextStyle.Body,
                color = colorScheme.colorTextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            HASwitch(
                checked = useCloud,
                onCheckedChange = onUseCloudToggle,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewExternalUrlViewCloudOn() {
    HAThemeForPreview {
        ExternalUrlView(
            canUseCloud = true,
            useCloud = true,
            externalUrl = "https://home.example.com:8123/",
            onUseCloudToggle = {},
            onExternalUrlSaved = {},
        )
    }
}

@Preview
@Composable
private fun PreviewExternalUrlViewCloudOff() {
    HAThemeForPreview {
        ExternalUrlView(
            canUseCloud = true,
            useCloud = false,
            externalUrl = "https://home.example.com:8123/",
            onUseCloudToggle = {},
            onExternalUrlSaved = {},
        )
    }
}

@Preview
@Composable
private fun PreviewExternalUrlViewCloudNone() {
    HAThemeForPreview {
        ExternalUrlView(
            canUseCloud = false,
            useCloud = false,
            externalUrl = "https://home.example.com:8123/",
            onUseCloudToggle = {},
            onExternalUrlSaved = {},
        )
    }
}

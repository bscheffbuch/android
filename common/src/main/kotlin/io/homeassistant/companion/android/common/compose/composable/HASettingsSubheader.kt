package io.homeassistant.companion.android.common.compose.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme

/**
 * A Home Assistant themed settings list subheader. This is the Material 3 replacement for the
 * legacy `SettingsSubheader` composable.
 *
 * Unlike the legacy composable, this does not offer icon-aligned padding: settings rows are now
 * displayed as standalone [HASettingsCard]s rather than a continuous grid-aligned list, so a
 * subheader no longer needs to indent its text to match a row's icon column.
 *
 * @param text The subheader text.
 * @param modifier The [Modifier] to be applied to the subheader.
 */
@Composable
fun HASettingsSubheader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HATextStyle.BodyMedium,
        color = LocalHAColorScheme.current.colorTextSecondary,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HADimens.SPACE10)
            .wrapContentSize(align = Alignment.CenterStart),
    )
}

@PreviewLightDark
@Composable
private fun HASettingsSubheaderPreview() {
    HAThemeForPreview {
        HASettingsSubheader(text = "Attributes")
    }
}

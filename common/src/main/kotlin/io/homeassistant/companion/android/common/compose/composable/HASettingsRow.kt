package io.homeassistant.companion.android.common.compose.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme

/**
 * A Home Assistant themed clickable settings list row displaying a primary and secondary text,
 * with an optional leading icon slot. This is the Material 3 replacement for the legacy
 * `SettingsRow` composable, styled as a standalone [HASettingsCard].
 *
 * The [icon] slot is intentionally generic rather than tied to a specific icon library so this
 * composable can live in the shared `:common` module. Callers that need to render an MDI icon
 * (via Iconics) build the composable themselves and pass it into this slot.
 *
 * @param primaryText The main text of the row.
 * @param secondaryText The secondary, supporting text displayed below [primaryText].
 * @param onClicked Callback invoked when the row is clicked.
 * @param modifier The [Modifier] to be applied to the row.
 * @param enabled Whether the row can be interacted with and is displayed in its enabled colors.
 * @param icon An optional composable slot displayed at the start of the row, before the text.
 */
@Composable
fun HASettingsRow(
    primaryText: String,
    secondaryText: String,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val colorScheme = LocalHAColorScheme.current
    HASettingsCard(
        modifier = modifier
            .clip(RoundedCornerShape(HARadius.XL))
            .clickable(enabled = enabled, onClick = onClicked),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HADimens.SPACE10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE4),
        ) {
            icon?.invoke()
            Column(verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1)) {
                Text(
                    text = primaryText,
                    style = HATextStyle.Body,
                    color = if (enabled) colorScheme.colorTextPrimary else colorScheme.colorTextDisabled,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = secondaryText,
                    style = HATextStyle.BodyMedium,
                    color = if (enabled) colorScheme.colorTextSecondary else colorScheme.colorTextDisabled,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HASettingsRowPreview() {
    HAThemeForPreview {
        HASettingsRow(
            primaryText = "Swipe up",
            secondaryText = "Open quick bar",
            onClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HASettingsRowDisabledPreview() {
    HAThemeForPreview {
        HASettingsRow(
            primaryText = "Swipe up",
            secondaryText = "Open quick bar",
            onClicked = {},
            enabled = false,
        )
    }
}

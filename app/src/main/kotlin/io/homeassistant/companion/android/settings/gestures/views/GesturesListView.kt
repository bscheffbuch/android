package io.homeassistant.companion.android.settings.gestures.views

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HASettingsRow
import io.homeassistant.companion.android.common.compose.composable.HASettingsSubheader
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.util.GestureAction
import io.homeassistant.companion.android.common.util.HAGesture
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues

/**
 * Shows all gestures with the configured action for it. Clicking on a
 * gesture calls [onGestureClicked] with the gesture.
 *
 * @param gestureActions User settings (gestures and the current action)
 * @param onGestureClicked Called when a gesture is selected
 */
@Composable
fun GesturesListView(
    gestureActions: Map<HAGesture, GestureAction>,
    onGestureClicked: (HAGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp) + safeBottomPaddingValues(applyHorizontal = false),
        modifier = modifier,
    ) {
        item {
            Text(
                text = stringResource(R.string.gestures_description),
                style = HATextStyle.Body,
                color = LocalHAColorScheme.current.colorTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            )
        }

        val gesturesGrouped = HAGesture.entries.groupBy { it.direction }
        gesturesGrouped.forEach { (direction, gestures) ->
            item {
                HASettingsSubheader(
                    text = stringResource(direction.description),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(gestures) { gesture ->
                val action = gestureActions[gesture]
                HASettingsRow(
                    primaryText = stringResource(gesture.pointers.description),
                    secondaryText = action?.let { stringResource(it.description) } ?: "",
                    onClicked = { onGestureClicked(gesture) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewGesturesListView() {
    HAThemeForPreview {
        GesturesListView(
            gestureActions = HAGesture.entries.associateWith { GestureAction.NONE },
            onGestureClicked = { _ -> },
        )
    }
}

package io.homeassistant.companion.android.settings.gestures.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.homeassistant.companion.android.common.compose.composable.HARadioGroup
import io.homeassistant.companion.android.common.compose.composable.HASettingsSubheader
import io.homeassistant.companion.android.common.compose.composable.RadioOption
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.util.GestureAction
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues

/**
 * View showing all actions for a gesture, grouped by category, and the currently
 * configured action. Clicking on an action calls [onActionClicked] with the action.
 *
 * @param selectedAction The current action
 * @param onActionClicked Called when an action is selected
 */
@Composable
fun GestureActionsView(
    selectedAction: GestureAction,
    onActionClicked: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionsGrouped = GestureAction.entries.minus(GestureAction.NONE).groupBy { it.category }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE4) +
            safeBottomPaddingValues(applyHorizontal = false),
        verticalArrangement = Arrangement.spacedBy(HADimens.SPACE2),
        modifier = modifier,
    ) {
        item {
            // GestureAction.NONE is here so it's always first in the list with no header
            HARadioGroup(
                options = listOf(
                    RadioOption(
                        selectionKey = GestureAction.NONE,
                        headline = stringResource(GestureAction.NONE.description),
                    ),
                ),
                onSelect = { onActionClicked(it.selectionKey) },
                selectionKey = selectedAction,
            )
        }
        actionsGrouped.forEach { (category, actions) ->
            item {
                HASettingsSubheader(stringResource(category.description))
            }
            item {
                HARadioGroup(
                    options = actions.map { action ->
                        RadioOption(selectionKey = action, headline = stringResource(action.description))
                    },
                    onSelect = { onActionClicked(it.selectionKey) },
                    selectionKey = selectedAction,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewGestureActionsView() {
    HAThemeForPreview {
        GestureActionsView(
            selectedAction = GestureAction.QUICKBAR_DEFAULT,
            onActionClicked = {},
        )
    }
}

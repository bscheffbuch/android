package io.homeassistant.companion.android.settings.widgets.views

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme as M2MaterialTheme
import androidx.compose.material.Text as M2Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.composable.HASettingsSubheader
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.database.widget.WidgetEntity
import io.homeassistant.companion.android.settings.views.EmptyState
import io.homeassistant.companion.android.settings.widgets.ManageWidgetsViewModel
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.compose.MdcAlertDialog
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import io.homeassistant.companion.android.widgets.button.ButtonWidgetConfigureActivity
import io.homeassistant.companion.android.widgets.camera.CameraWidgetConfigureActivity
import io.homeassistant.companion.android.widgets.entity.EntityWidgetConfigureActivity
import io.homeassistant.companion.android.widgets.mediaplayer.MediaPlayerControlsWidgetConfigureActivity
import io.homeassistant.companion.android.widgets.template.TemplateWidgetConfigureActivity
import io.homeassistant.companion.android.widgets.todo.TodoWidgetConfigureActivity

enum class WidgetType(val widgetIcon: IIcon) {
    BUTTON(CommunityMaterial.Icon2.cmd_gesture_tap),
    CAMERA(CommunityMaterial.Icon.cmd_camera_image),
    STATE(CommunityMaterial.Icon3.cmd_shape),
    MEDIA(CommunityMaterial.Icon3.cmd_play_box_multiple),
    TEMPLATE(CommunityMaterial.Icon.cmd_code_braces),
    TODO(CommunityMaterial.Icon.cmd_clipboard_list),
    ;

    fun configureActivity() = when (this) {
        BUTTON -> ButtonWidgetConfigureActivity::class.java
        CAMERA -> CameraWidgetConfigureActivity::class.java
        MEDIA -> MediaPlayerControlsWidgetConfigureActivity::class.java
        STATE -> EntityWidgetConfigureActivity::class.java
        TEMPLATE -> TemplateWidgetConfigureActivity::class.java
        TODO -> TodoWidgetConfigureActivity::class.java
    }
}

@Composable
fun ManageWidgetsView(viewModel: ManageWidgetsViewModel, modifier: Modifier = Modifier) {
    var expandedAddWidget by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (viewModel.supportsAddingWidgets) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(safeBottomPaddingValues(applyHorizontal = false)),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_widget)) },
                    onClick = { expandedAddWidget = true },
                )
            }
        },
    ) { contentPadding ->
        if (expandedAddWidget) {
            val availableWidgets = listOf(
                stringResource(R.string.widget_button_image_description) to WidgetType.BUTTON,
                stringResource(R.string.widget_camera_description) to WidgetType.CAMERA,
                stringResource(R.string.widget_static_image_description) to WidgetType.STATE,
                stringResource(R.string.widget_media_player_description) to WidgetType.MEDIA,
                stringResource(R.string.template_widget) to WidgetType.TEMPLATE,
                stringResource(R.string.todo_widget) to WidgetType.TODO,
            ).sortedBy { it.first }

            HomeAssistantAppTheme {
                MdcAlertDialog(
                    onDismissRequest = { expandedAddWidget = false },
                    title = { Text(stringResource(R.string.add_widget)) },
                    content = {
                        LazyColumn {
                            items(availableWidgets, key = { (key) -> key }) { (key, widgetType) ->
                                PopupWidgetRow(widgetLabel = key, widgetType = widgetType) {
                                    expandedAddWidget = false
                                }
                            }
                        }
                    },
                    onCancel = { expandedAddWidget = false },
                    contentPadding = PaddingValues(all = 0.dp),
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(all = 16.dp) + safeBottomPaddingValues(applyHorizontal = false),
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth(),
        ) {
            if (viewModel.buttonWidgetList.value.isEmpty() &&
                viewModel.staticWidgetList.value.isEmpty() &&
                viewModel.mediaWidgetList.value.isEmpty() &&
                viewModel.templateWidgetList.value.isEmpty() &&
                viewModel.cameraWidgetList.value.isEmpty() &&
                viewModel.todoWidgetList.value.isEmpty()
            ) {
                item {
                    HomeAssistantAppTheme {
                        EmptyState(
                            icon = CommunityMaterial.Icon3.cmd_widgets,
                            title = stringResource(R.string.no_widgets),
                            subtitle = stringResource(R.string.no_widgets_summary),
                        )
                    }
                }
            }
            widgetItems(
                viewModel.buttonWidgetList.value,
                widgetType = WidgetType.BUTTON,
                title = R.string.button_widgets,
                widgetLabel = { item ->
                    val label = item.label
                    if (!label.isNullOrEmpty()) label else "${item.domain}.${item.service}"
                },
            )
            widgetItems(
                viewModel.cameraWidgetList.value,
                widgetType = WidgetType.CAMERA,
                title = R.string.camera_widgets,
                widgetLabel = { item -> item.entityId },
            )
            widgetItems(
                viewModel.staticWidgetList.value,
                widgetType = WidgetType.STATE,
                title = R.string.entity_state_widgets,
                widgetLabel = { item ->
                    val label = item.label
                    if (!label.isNullOrEmpty()) {
                        label
                    } else {
                        "${item.entityId} ${item.stateSeparator} ${item.attributeIds.orEmpty()}"
                    }
                },
            )
            widgetItems(
                viewModel.mediaWidgetList.value,
                widgetType = WidgetType.MEDIA,
                title = R.string.media_player_widgets,
                widgetLabel = { item ->
                    val label = item.label
                    if (!label.isNullOrEmpty()) label else item.entityId
                },
            )
            widgetItems(
                viewModel.templateWidgetList.value,
                widgetType = WidgetType.TEMPLATE,
                title = R.string.template_widgets,
                widgetLabel = { item -> item.template },
            )
            widgetItems(
                viewModel.todoWidgetList.value,
                widgetType = WidgetType.TODO,
                title = R.string.todo_widgets,
                widgetLabel = { item -> item.entityId },
            )
        }
    }
}

private fun <T : WidgetEntity<T>> LazyListScope.widgetItems(
    widgetList: List<T>,
    @StringRes title: Int,
    widgetLabel: @Composable (T) -> String,
    widgetType: WidgetType,
) {
    if (widgetList.isNotEmpty()) {
        item {
            HASettingsSubheader(text = stringResource(id = title))
        }
        items(widgetList, key = { "$widgetType-${it.id}" }) { item ->
            WidgetRow(
                widgetLabel = widgetLabel(item),
                widgetId = item.id,
                widgetType = widgetType,
                modifier = Modifier.padding(vertical = HADimens.SPACE1),
            )
        }
    }
}

@Composable
private fun PopupWidgetRow(
    widgetLabel: String,
    widgetType: WidgetType,
    modifier: Modifier = Modifier,
    onClickCallback: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, widgetType.configureActivity()).apply {
                    putExtra(ManageWidgetsViewModel.CONFIGURE_REQUEST_LAUNCHER, true)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
                onClickCallback()
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                asset = widgetType.widgetIcon,
                colorFilter = ColorFilter.tint(M2MaterialTheme.colors.onSurface),
                contentDescription = widgetLabel,
            )
            M2Text(text = widgetLabel, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

/**
 * A single configured widget, showing its type icon and current label. Tapping opens the
 * widget's own configuration screen so the user can adjust it.
 */
@Composable
private fun WidgetRow(widgetLabel: String, widgetId: Int, widgetType: WidgetType, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colorScheme = LocalHAColorScheme.current
    HASettingsCard(
        modifier = modifier
            .clip(RoundedCornerShape(HARadius.XL))
            .clickable {
                val intent = Intent(context, widgetType.configureActivity()).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                context.startActivity(intent)
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HADimens.SPACE10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HADimens.SPACE4),
        ) {
            Image(
                asset = widgetType.widgetIcon,
                colorFilter = ColorFilter.tint(colorScheme.colorTextPrimary),
                contentDescription = null,
            )
            Text(
                text = widgetLabel,
                style = HATextStyle.Body,
                color = colorScheme.colorTextPrimary,
                textAlign = TextAlign.Start,
            )
        }
    }
}

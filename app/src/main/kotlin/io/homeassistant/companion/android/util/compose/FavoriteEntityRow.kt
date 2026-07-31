package io.homeassistant.companion.android.util.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun ReorderableCollectionItemScope.FavoriteEntityRow(
    entityName: String,
    entityId: String,
    onClick: () -> Unit,
    checked: Boolean,
    modifier: Modifier = Modifier,
    draggable: Boolean = false,
    isDragging: Boolean = false,
) {
    val colorScheme = LocalHAColorScheme.current
    val surfaceElevation = animateDpAsState(targetValue = if (isDragging) HADimens.SPACE2 else HADimens.SPACE0)
    var rowModifier = Modifier.fillMaxWidth().heightIn(min = HADimens.SPACE18)
    if (draggable) {
        rowModifier = rowModifier.longPressDraggableHandle()
    }
    Surface(
        color = colorScheme.colorSurfaceLow,
        shadowElevation = surfaceElevation.value,
        shape = RoundedCornerShape(HARadius.XL),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(start = HADimens.SPACE4),
            ) {
                Text(
                    text = entityName,
                    style = HATextStyle.Body.copy(textAlign = TextAlign.Start),
                    color = colorScheme.colorTextPrimary,
                )
                Text(
                    text = entityId,
                    style = HATextStyle.BodyMedium.copy(textAlign = TextAlign.Start),
                    color = colorScheme.colorTextSecondary,
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (checked) Icons.Default.Clear else Icons.Default.Add,
                    contentDescription = stringResource(if (checked) R.string.delete else R.string.add_favorite),
                    tint = colorScheme.colorTextPrimary,
                )
            }
            if (draggable) {
                Image(
                    asset = CommunityMaterial.Icon.cmd_drag_horizontal_variant,
                    contentDescription = stringResource(R.string.hold_to_reorder),
                    colorFilter = ColorFilter.tint(colorScheme.colorTextSecondary),
                    modifier = Modifier
                        .size(width = HADimens.SPACE10, height = HADimens.SPACE6)
                        .padding(end = HADimens.SPACE4),
                )
            }
        }
    }
}

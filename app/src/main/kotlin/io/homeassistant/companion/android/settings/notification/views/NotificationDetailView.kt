package io.homeassistant.companion.android.settings.notification.views

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HASettingsSubheader
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.database.notification.NotificationItem
import io.homeassistant.companion.android.util.notificationItem
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import java.util.Calendar
import java.util.GregorianCalendar
import kotlinx.serialization.json.Json

@Composable
fun LoadNotification(notification: NotificationItem, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val valueModifier = Modifier.padding(start = HADimens.SPACE6)
    val colorScheme = LocalHAColorScheme.current

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(safeBottomPaddingValues(applyHorizontal = false)),
    ) {
        HASettingsSubheader(
            text = stringResource(commonR.string.notification_received_at),
            modifier = Modifier.padding(start = HADimens.SPACE4, top = HADimens.SPACE8),
        )
        val cal: Calendar = GregorianCalendar()
        cal.timeInMillis = notification.received
        Text(
            text = cal.time.toString(),
            style = HATextStyle.Body,
            color = colorScheme.colorTextPrimary,
            textAlign = TextAlign.Start,
            modifier = valueModifier,
        )

        HASettingsSubheader(
            text = stringResource(commonR.string.notification_source),
            modifier = Modifier.padding(start = HADimens.SPACE4, top = HADimens.SPACE8),
        )
        Text(
            text = notification.source,
            style = HATextStyle.Body,
            color = colorScheme.colorTextPrimary,
            textAlign = TextAlign.Start,
            modifier = valueModifier,
        )

        HASettingsSubheader(
            text = stringResource(commonR.string.notification_message),
            modifier = Modifier.padding(start = HADimens.SPACE4, top = HADimens.SPACE8),
        )
        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    text = HtmlCompat.fromHtml(notification.message, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    textSize = 16f
                }
            },
            modifier = valueModifier,
        )

        HASettingsSubheader(
            text = stringResource(commonR.string.notification_data),
            modifier = Modifier.padding(start = HADimens.SPACE4, top = HADimens.SPACE8),
        )
        val notifData =
            // Try to pretty print the JSON
            try {
                val mapper = Json {
                    isLenient = true // allow unquoted field names
                    prettyPrint = true
                }
                val jsonElement = mapper.parseToJsonElement(notification.data)
                mapper.encodeToString(jsonElement)
            } catch (e: Exception) {
                notification.data
            }
        Text(
            text = notifData,
            style = HATextStyle.Body,
            color = colorScheme.colorTextPrimary,
            textAlign = TextAlign.Start,
            modifier = valueModifier.then(Modifier.padding(bottom = HADimens.SPACE4)),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewNotificationDetails() {
    HAThemeForPreview {
        LoadNotification(notification = notificationItem)
    }
}

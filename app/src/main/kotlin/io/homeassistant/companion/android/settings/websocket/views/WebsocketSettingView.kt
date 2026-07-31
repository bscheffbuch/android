package io.homeassistant.companion.android.settings.websocket.views

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.getSystemService
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.compose.composable.HARadioGroup
import io.homeassistant.companion.android.common.compose.composable.RadioOption
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.util.CHANNEL_WEBSOCKET
import io.homeassistant.companion.android.database.settings.WebsocketSetting
import io.homeassistant.companion.android.util.compose.HaAlertWarning
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.compose.InfoNotification
import io.homeassistant.companion.android.util.safeBottomPaddingValues

@Composable
fun WebsocketSettingView(
    websocketSetting: WebsocketSetting,
    unrestrictedBackgroundAccess: Boolean,
    hasWifi: Boolean,
    onSettingChanged: (WebsocketSetting) -> Unit,
    onBackgroundAccessTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Box(modifier = modifier.verticalScroll(scrollState)) {
        Column(
            modifier = Modifier
                .padding(safeBottomPaddingValues(applyHorizontal = false))
                .padding(all = HADimens.SPACE4),
        ) {
            Text(
                text = stringResource(R.string.websocket_setting_description),
                style = HATextStyle.Body,
                color = LocalHAColorScheme.current.colorTextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = HADimens.SPACE4),
            )
            if (!unrestrictedBackgroundAccess && websocketSetting != WebsocketSetting.NEVER) {
                HomeAssistantAppTheme {
                    HaAlertWarning(
                        message = stringResource(R.string.websocket_notification_backgroundaccess),
                        action = stringResource(R.string.allow),
                        onActionClicked = onBackgroundAccessTapped,
                    )
                }
                Spacer(modifier = Modifier.height(HADimens.SPACE4))
            }
            HARadioGroup(
                options = buildList {
                    add(
                        RadioOption(
                            selectionKey = WebsocketSetting.NEVER,
                            headline = stringResource(
                                if (BuildConfig.FLAVOR == "full") {
                                    R.string.websocket_setting_never
                                } else {
                                    R.string.websocket_setting_never_minimal
                                },
                            ),
                        ),
                    )
                    if (hasWifi) {
                        add(
                            RadioOption(
                                selectionKey = WebsocketSetting.HOME_WIFI,
                                headline = stringResource(
                                    if (BuildConfig.FLAVOR == "full") {
                                        R.string.websocket_setting_home_wifi
                                    } else {
                                        R.string.websocket_setting_home_wifi_minimal
                                    },
                                ),
                            ),
                        )
                    }
                    add(
                        RadioOption(
                            selectionKey = WebsocketSetting.SCREEN_ON,
                            headline = stringResource(
                                if (BuildConfig.FLAVOR == "full") {
                                    R.string.websocket_setting_while_screen_on
                                } else {
                                    R.string.websocket_setting_while_screen_on_minimal
                                },
                            ),
                        ),
                    )
                    add(
                        RadioOption(
                            selectionKey = WebsocketSetting.ALWAYS,
                            headline = stringResource(
                                if (BuildConfig.FLAVOR == "full") {
                                    R.string.websocket_setting_always
                                } else {
                                    R.string.websocket_setting_always_minimal
                                },
                            ),
                        ),
                    )
                },
                selectionKey = websocketSetting,
                onSelect = { onSettingChanged(it.selectionKey) },
            )
            val uiManager = context.getSystemService<UiModeManager>()
            if (websocketSetting != WebsocketSetting.NEVER &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                uiManager?.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION
            ) {
                HomeAssistantAppTheme {
                    InfoNotification(
                        infoString = R.string.websocket_persistent_notification,
                        channelId = CHANNEL_WEBSOCKET,
                        buttonString = R.string.websocket_notification_channel,
                    )
                }
            }
        }
    }
}

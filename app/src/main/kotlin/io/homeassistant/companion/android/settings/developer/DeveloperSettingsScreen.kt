package io.homeassistant.companion.android.settings.developer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAPlainButton
import io.homeassistant.companion.android.common.compose.composable.HASettingsCard
import io.homeassistant.companion.android.common.compose.composable.HASettingsRow
import io.homeassistant.companion.android.common.compose.composable.HASwitch
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HARadius
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.util.plus
import io.homeassistant.companion.android.util.safeBottomPaddingValues
import org.jetbrains.annotations.VisibleForTesting

/**
 * Developer settings ("Troubleshooting") screen: share logs, view location tracking history,
 * toggle WebView remote debugging, sync Thread credentials, and clear the frontend cache.
 *
 * @param onRequestServerSelection Shows a server picker when a Thread sync needs to know which of
 * several registered servers to target; the host must forward the user's choice to the given
 * callback.
 */
@Composable
fun DeveloperSettingsScreen(
    viewModel: DeveloperSettingsViewModel,
    onShowLogsClicked: () -> Unit,
    onLocationTrackingClicked: () -> Unit,
    onRequestServerSelection: (onServerSelected: (Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val threadPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onThreadPermissionResult(result)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DeveloperSettingsEvent.RequestServerSelectionForThreadDebug ->
                    onRequestServerSelection(viewModel::onServerSelectedForThreadDebug)

                is DeveloperSettingsEvent.RequestThreadPermission ->
                    threadPermissionLauncher.launch(IntentSenderRequest.Builder(event.intentSender).build())
            }
        }
    }

    LaunchedEffect(uiState.webViewClearCacheResult) {
        val success = uiState.webViewClearCacheResult ?: return@LaunchedEffect
        Toast.makeText(
            context,
            if (success) commonR.string.clear_webview_cache_success else commonR.string.clear_webview_cache_failed,
            Toast.LENGTH_SHORT,
        ).show()
        viewModel.onWebViewClearCacheResultAcknowledged()
    }

    DeveloperSettingsContent(
        uiState = uiState,
        onShowLogsClicked = onShowLogsClicked,
        onLocationTrackingClicked = onLocationTrackingClicked,
        onRemoteDebuggingToggled = viewModel::onRemoteDebuggingToggled,
        onThreadDebugClicked = viewModel::onThreadDebugClicked,
        onClearWebViewCacheClicked = viewModel::onClearWebViewCacheClicked,
        onDismissThreadDebugResult = viewModel::onThreadDebugResultDismissed,
        modifier = modifier,
    )
}

@VisibleForTesting
internal const val DEVELOPER_SETTINGS_LIST_TEST_TAG = "developer_settings_list"

@Composable
@VisibleForTesting
internal fun DeveloperSettingsContent(
    uiState: DeveloperSettingsUiState,
    onShowLogsClicked: () -> Unit,
    onLocationTrackingClicked: () -> Unit,
    onRemoteDebuggingToggled: (Boolean) -> Unit,
    onThreadDebugClicked: () -> Unit,
    onClearWebViewCacheClicked: () -> Unit,
    onDismissThreadDebugResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = HADimens.SPACE4) + safeBottomPaddingValues(applyHorizontal = false),
        modifier = modifier.testTag(DEVELOPER_SETTINGS_LIST_TEST_TAG),
    ) {
        item {
            HASettingsRow(
                primaryText = stringResource(commonR.string.show_share_logs),
                secondaryText = stringResource(commonR.string.show_share_logs_summary),
                onClicked = onShowLogsClicked,
                modifier = Modifier.padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE1),
            )
        }

        if (uiState.locationTrackingVisible) {
            item {
                HASettingsRow(
                    primaryText = stringResource(commonR.string.location_tracking),
                    secondaryText = stringResource(commonR.string.location_tracking_summary),
                    onClicked = onLocationTrackingClicked,
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE1),
                )
            }
        }

        item {
            RemoteDebuggingRow(
                enabled = uiState.remoteDebuggingEnabled,
                onToggle = onRemoteDebuggingToggled,
                modifier = Modifier.padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE1),
            )
        }

        if (uiState.threadDebugVisible) {
            item {
                HASettingsRow(
                    primaryText = stringResource(commonR.string.thread_debug),
                    secondaryText = stringResource(commonR.string.thread_debug_summary),
                    onClicked = onThreadDebugClicked,
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE1),
                )
            }
        }

        if (uiState.webViewClearCacheVisible) {
            item {
                HASettingsRow(
                    primaryText = stringResource(commonR.string.clear_webview_cache),
                    secondaryText = stringResource(commonR.string.clear_webview_cache_summary),
                    onClicked = onClearWebViewCacheClicked,
                    modifier = Modifier.padding(horizontal = HADimens.SPACE4, vertical = HADimens.SPACE1),
                )
            }
        }
    }

    if (uiState.threadDebugInProgress) {
        ProgressDialog(text = stringResource(commonR.string.thread_debug_active))
    }

    uiState.threadDebugResult?.let { result ->
        ThreadDebugResultDialog(result = result, onDismiss = onDismissThreadDebugResult)
    }

    if (uiState.webViewClearCacheInProgress) {
        ProgressDialog(text = stringResource(commonR.string.clear_webview_cache_active))
    }
}

@Composable
private fun RemoteDebuggingRow(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = LocalHAColorScheme.current
    HASettingsCard(
        modifier = modifier
            .clip(RoundedCornerShape(HARadius.XL))
            .clickable(role = Role.Switch) { onToggle(!enabled) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HADimens.SPACE1),
            ) {
                Text(
                    text = stringResource(commonR.string.remote_debugging),
                    style = HATextStyle.Body,
                    textAlign = TextAlign.Start,
                    color = colorScheme.colorTextPrimary,
                )
                Text(
                    text = stringResource(commonR.string.remote_debugging_summary),
                    style = HATextStyle.BodyMedium,
                    textAlign = TextAlign.Start,
                    color = colorScheme.colorTextSecondary,
                )
            }
            HASwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ProgressDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = { Text(text = text, style = HATextStyle.Body) },
    )
}

@Composable
private fun ThreadDebugResultDialog(result: ThreadDebugResult, onDismiss: () -> Unit) {
    val icon = when (result.success) {
        true -> "✅"
        false -> "⛔"
        null -> "⚠️"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(commonR.string.thread_debug), style = HATextStyle.HeadlineMedium) },
        text = { Text(text = "$icon\n\n${result.message}", style = HATextStyle.Body) },
        confirmButton = {
            HAPlainButton(stringResource(commonR.string.ok), onDismiss)
        },
    )
}

@Preview
@Composable
private fun DeveloperSettingsContentPreview() {
    HAThemeForPreview {
        DeveloperSettingsContent(
            uiState = DeveloperSettingsUiState(
                remoteDebuggingEnabled = false,
                locationTrackingVisible = true,
                threadDebugVisible = true,
                webViewClearCacheVisible = true,
            ),
            onShowLogsClicked = {},
            onLocationTrackingClicked = {},
            onRemoteDebuggingToggled = {},
            onThreadDebugClicked = {},
            onClearWebViewCacheClicked = {},
            onDismissThreadDebugResult = {},
        )
    }
}

@Preview
@Composable
private fun DeveloperSettingsContentThreadResultPreview() {
    HAThemeForPreview {
        DeveloperSettingsContent(
            uiState = DeveloperSettingsUiState(
                remoteDebuggingEnabled = true,
                locationTrackingVisible = true,
                threadDebugVisible = true,
                webViewClearCacheVisible = true,
                threadDebugResult = ThreadDebugResult(
                    message = "Home Assistant and this device use the same network",
                    success = true,
                ),
            ),
            onShowLogsClicked = {},
            onLocationTrackingClicked = {},
            onRemoteDebuggingToggled = {},
            onThreadDebugClicked = {},
            onClearWebViewCacheClicked = {},
            onDismissThreadDebugResult = {},
        )
    }
}

@Preview
@Composable
private fun DeveloperSettingsContentInProgressPreview() {
    HAThemeForPreview {
        DeveloperSettingsContent(
            uiState = DeveloperSettingsUiState(
                locationTrackingVisible = true,
                threadDebugVisible = true,
                webViewClearCacheVisible = true,
                webViewClearCacheInProgress = true,
            ),
            onShowLogsClicked = {},
            onLocationTrackingClicked = {},
            onRemoteDebuggingToggled = {},
            onThreadDebugClicked = {},
            onClearWebViewCacheClicked = {},
            onDismissThreadDebugResult = {},
        )
    }
}

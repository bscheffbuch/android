package io.homeassistant.companion.android.settings.developer

import android.content.Context
import android.content.IntentSender
import android.webkit.WebStorage
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.thread.ThreadManager
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

/** Minimum time the "in progress" dialogs stay visible, to avoid a jarring flash on fast operations. */
private val MIN_PROGRESS_DIALOG_DURATION = 750.milliseconds

/**
 * UI state for the Developer settings (Troubleshooting) screen.
 */
data class DeveloperSettingsUiState(
    val remoteDebuggingEnabled: Boolean = false,
    val locationTrackingVisible: Boolean = BuildConfig.FLAVOR == "full",
    val threadDebugVisible: Boolean = false,
    val webViewClearCacheVisible: Boolean = false,
    val threadDebugInProgress: Boolean = false,
    val threadDebugResult: ThreadDebugResult? = null,
    val webViewClearCacheInProgress: Boolean = false,
    val webViewClearCacheResult: Boolean? = null,
)

/**
 * Outcome of a Thread credential sync attempt, ready to display to the user.
 *
 * @param message Human-readable description of what happened.
 * @param success `true` if credentials were successfully synced, `false` if an error occurred, or
 * `null` if the sync was inconclusive (e.g. no credentials to sync on either side).
 */
data class ThreadDebugResult(val message: String, val success: Boolean?)

/**
 * One-shot events emitted by [DeveloperSettingsViewModel] that require the host (Fragment) to act,
 * since they involve either launching an activity result contract or showing another Fragment.
 */
sealed interface DeveloperSettingsEvent {

    /**
     * Ask the user to pick which server to sync Thread credentials for, since there is more than
     * one registered server. The host is responsible for forwarding the chosen server id back via
     * [DeveloperSettingsViewModel.onServerSelectedForThreadDebug].
     */
    data object RequestServerSelectionForThreadDebug : DeveloperSettingsEvent

    /**
     * Ask the user for permission to export a Thread dataset. The host is responsible for
     * launching [intentSender] and forwarding the result back via
     * [DeveloperSettingsViewModel.onThreadPermissionResult].
     */
    data class RequestThreadPermission(val intentSender: IntentSender) : DeveloperSettingsEvent
}

@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    private val prefsRepository: PrefsRepository,
    private val serverManager: ServerManager,
    private val threadManager: ThreadManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DeveloperSettingsUiState(
            threadDebugVisible = threadManager.appSupportsThread(),
            webViewClearCacheVisible = WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA),
        ),
    )
    val uiState: StateFlow<DeveloperSettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DeveloperSettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DeveloperSettingsEvent> = _events.asSharedFlow()

    private var pendingThreadServerId: Int = ServerManager.SERVER_ID_ACTIVE
    private var pendingThreadIsDeviceOnly: Boolean = true

    init {
        viewModelScope.launch {
            val enabled = prefsRepository.isWebViewDebugEnabled()
            _uiState.update { it.copy(remoteDebuggingEnabled = enabled) }
        }
    }

    /** Toggle WebView remote debugging on or off. */
    fun onRemoteDebuggingToggled(enabled: Boolean) {
        _uiState.update { it.copy(remoteDebuggingEnabled = enabled) }
        viewModelScope.launch { prefsRepository.setWebViewDebugEnabled(enabled) }
    }

    /**
     * Start a Thread credential sync, asking the user to pick a server first if there's more than
     * one registered.
     */
    fun onThreadDebugClicked() {
        viewModelScope.launch {
            if (serverManager.servers().size > 1) {
                _events.emit(DeveloperSettingsEvent.RequestServerSelectionForThreadDebug)
            } else {
                startThreadDebug(ServerManager.SERVER_ID_ACTIVE)
            }
        }
    }

    /** Continue a Thread credential sync after the user picked [serverId] from multiple servers. */
    fun onServerSelectedForThreadDebug(serverId: Int) {
        startThreadDebug(serverId)
    }

    private fun startThreadDebug(serverId: Int) {
        _uiState.update { it.copy(threadDebugInProgress = true) }
        viewModelScope.launch {
            try {
                when (
                    val syncResult = threadManager.syncPreferredDataset(
                        context,
                        serverId,
                        false,
                        CoroutineScope(coroutineContext + SupervisorJob()),
                    )
                ) {
                    is ThreadManager.SyncResult.ServerUnsupported ->
                        postThreadDebugResult(
                            context.getString(commonR.string.thread_debug_result_unsupported_server),
                            false,
                        )

                    is ThreadManager.SyncResult.OnlyOnServer -> {
                        if (syncResult.imported) {
                            postThreadDebugResult(context.getString(commonR.string.thread_debug_result_imported), true)
                        } else {
                            postThreadDebugResult(context.getString(commonR.string.thread_debug_result_error), false)
                        }
                    }

                    is ThreadManager.SyncResult.OnlyOnDevice -> {
                        if (syncResult.exportIntent != null) {
                            requestThreadPermission(syncResult.exportIntent, serverId, isDeviceOnly = true)
                        } // else currently doesn't happen
                    }

                    is ThreadManager.SyncResult.AllHaveCredentials -> {
                        if (syncResult.exportIntent != null) {
                            requestThreadPermission(syncResult.exportIntent, serverId, isDeviceOnly = false)
                        } else if (syncResult.matches == true) {
                            postThreadDebugResult(context.getString(commonR.string.thread_debug_result_match), true)
                        } else if (syncResult.fromApp == true && syncResult.updated == true) {
                            postThreadDebugResult(context.getString(commonR.string.thread_debug_result_updated), true)
                        } else if (syncResult.fromApp == true && syncResult.updated == false) {
                            postThreadDebugResult(context.getString(commonR.string.thread_debug_result_removed), true)
                        } else {
                            postThreadDebugResult(context.getString(commonR.string.thread_debug_result_error), false)
                        }
                    }

                    is ThreadManager.SyncResult.NoneHaveCredentials ->
                        postThreadDebugResult(context.getString(commonR.string.thread_debug_result_none), null)

                    else ->
                        postThreadDebugResult(context.getString(commonR.string.thread_debug_result_error), false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while syncing preferred Thread dataset")
                postThreadDebugResult(context.getString(commonR.string.thread_debug_result_error), false)
            }
        }
    }

    private suspend fun requestThreadPermission(intentSender: IntentSender, serverId: Int, isDeviceOnly: Boolean) {
        pendingThreadServerId = serverId
        pendingThreadIsDeviceOnly = isDeviceOnly
        _events.emit(DeveloperSettingsEvent.RequestThreadPermission(intentSender))
    }

    /** Process the result of a [DeveloperSettingsEvent.RequestThreadPermission] launch. */
    fun onThreadPermissionResult(result: ActivityResult) {
        viewModelScope.launch {
            try {
                val submitted = threadManager.sendThreadDatasetExportResult(result, pendingThreadServerId)
                if (submitted != null) {
                    if (pendingThreadIsDeviceOnly) {
                        postThreadDebugResult(context.getString(commonR.string.thread_debug_result_exported), true)
                    } else {
                        // If we got permission while both had a dataset, the device prefers a different network
                        val out = "${context.getString(commonR.string.thread_debug_result_mismatch)} " +
                            context.getString(commonR.string.thread_debug_result_mismatch_detail, submitted)
                        postThreadDebugResult(out, null)
                    }
                } else {
                    postThreadDebugResult(context.getString(commonR.string.thread_debug_result_error), false)
                }
            } catch (e: Exception) {
                postThreadDebugResult(context.getString(commonR.string.thread_debug_result_error), false)
            }
        }
    }

    private fun postThreadDebugResult(message: String, success: Boolean?) {
        _uiState.update {
            it.copy(threadDebugInProgress = false, threadDebugResult = ThreadDebugResult(message, success))
        }
    }

    /** Dismiss the currently shown Thread debug result dialog. */
    fun onThreadDebugResultDismissed() {
        _uiState.update { it.copy(threadDebugResult = null) }
    }

    /** Clear the WebView frontend cache, if supported on this device. */
    fun onClearWebViewCacheClicked() {
        if (!_uiState.value.webViewClearCacheVisible) return
        _uiState.update { it.copy(webViewClearCacheInProgress = true) }
        viewModelScope.launch {
            val success = try {
                clearBrowsingData()
            } catch (e: RuntimeException) {
                Timber.e(e, "Unable to clear WebView cache")
                false
            }
            delay(MIN_PROGRESS_DIALOG_DURATION)
            _uiState.update { it.copy(webViewClearCacheInProgress = false, webViewClearCacheResult = success) }
        }
    }

    private suspend fun clearBrowsingData(): Boolean = suspendCancellableCoroutine { continuation ->
        WebStorageCompat.deleteBrowsingData(WebStorage.getInstance(), Dispatchers.IO.asExecutor()) {
            continuation.resume(true)
        }
    }

    /** Acknowledge the last WebView cache clear result (e.g. after showing a toast). */
    fun onWebViewClearCacheResultAcknowledged() {
        _uiState.update { it.copy(webViewClearCacheResult = null) }
    }
}

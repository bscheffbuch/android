package io.homeassistant.companion.android.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.IntentCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import eightbitlab.com.blurview.BlurView
import io.homeassistant.companion.android.BaseActivity
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.authenticator.Authenticator
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HATheme
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.overview.ui.HomeBottomNavigationBar
import io.homeassistant.companion.android.overview.ui.HomeContentTab
import io.homeassistant.companion.android.settings.assist.AssistSettingsFragment
import io.homeassistant.companion.android.settings.developer.DeveloperSettingsFragment
import io.homeassistant.companion.android.settings.notification.NotificationHistoryFragment
import io.homeassistant.companion.android.settings.qs.ManageTilesFragment
import io.homeassistant.companion.android.settings.sensor.SensorDetailFragment
import io.homeassistant.companion.android.settings.server.ServerSettingsFragment
import io.homeassistant.companion.android.settings.ssid.SsidFragment
import io.homeassistant.companion.android.settings.websocket.WebsocketSettingFragment
import io.homeassistant.companion.android.util.applySafeDrawingInsets
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

private const val EXTRA_FRAGMENT = "fragment"
private const val EXTRA_SHOW_HOME_NAV_BAR = "show_home_nav_bar"

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    private val viewModel: AppLockViewModel by viewModels()

    private lateinit var authenticator: Authenticator
    private lateinit var blurView: BlurView
    private lateinit var toolbar: Toolbar

    private var authenticating = false
    private var externalAuthCallback: ((Int) -> Boolean)? = null

    companion object {
        /** Key of the [HomeContentTab] extra set on the result [Intent] returned by [finish]. */
        const val EXTRA_SELECTED_TAB = "selected_tab"

        /**
         * @param showHomeNavBar Whether to render [HomeBottomNavigationBar] at the bottom of this
         * Activity, letting the caller be navigated back to a specific [HomeContentTab] via the
         * activity result instead of a plain "up"/back navigation. Only set this for callers that
         * host that same bar themselves, such as the native Overview landing screen.
         */
        fun newInstance(context: Context, screen: Deeplink? = null, showHomeNavBar: Boolean = false): Intent {
            return Intent(context, SettingsActivity::class.java).apply {
                if (screen != null) {
                    putExtra(EXTRA_FRAGMENT, screen)
                }
                if (showHomeNavBar) {
                    putExtra(EXTRA_SHOW_HOME_NAV_BAR, true)
                }
            }
        }
    }

    @Parcelize
    sealed interface Deeplink : Parcelable {
        data object Developer : Deeplink
        data class HomeNetwork(val serverId: Int) : Deeplink
        data object NotificationHistory : Deeplink
        data class QSTile(val tileId: String) : Deeplink
        data class Sensor(val sensorId: String) : Deeplink
        data object Websocket : Deeplink
        data object AssistSettings : Deeplink
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Delegate bottom insets to the fragments
        findViewById<View>(R.id.root).applySafeDrawingInsets(applyBottom = false, consumeInsets = false)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        blurView = findViewById(R.id.blurView)
        blurView.setupWith(window.decorView.rootView as ViewGroup)
            .setBlurRadius(8f)
            .setBlurEnabled(false)

        if (intent.getBooleanExtra(EXTRA_SHOW_HOME_NAV_BAR, false)) {
            findViewById<ComposeView>(R.id.bottomNavComposeView).apply {
                visibility = View.VISIBLE
                setContent {
                    HATheme {
                        HomeBottomNavigationBar(
                            selectedTab = null,
                            onSelectHome = { finishWithSelectedTab(HomeContentTab.HOME) },
                            onSelectAutomationsAndScenes = {
                                finishWithSelectedTab(HomeContentTab.AUTOMATIONS_AND_SCENES)
                            },
                            onOpenSettings = {},
                        )
                    }
                }
            }
        }

        authenticator = Authenticator(this, this, ::settingsActivityAuthenticationResult)

        if (savedInstanceState == null) {
            val settingsNavigation = IntentCompat.getParcelableExtra(intent, EXTRA_FRAGMENT, Deeplink::class.java)
            lifecycleScope.launch {
                supportFragmentManager.commit {
                    replace(
                        R.id.content,
                        when (settingsNavigation) {
                            Deeplink.Websocket -> if (serverManager.servers().size == 1) {
                                WebsocketSettingFragment::class.java
                            } else {
                                SettingsFragment::class.java
                            }
                            Deeplink.Developer -> DeveloperSettingsFragment::class.java
                            is Deeplink.HomeNetwork -> SsidFragment::class.java
                            Deeplink.NotificationHistory -> NotificationHistoryFragment::class.java
                            is Deeplink.Sensor -> SensorDetailFragment::class.java
                            is Deeplink.QSTile -> ManageTilesFragment::class.java
                            Deeplink.AssistSettings -> AssistSettingsFragment::class.java
                            else -> SettingsFragment::class.java
                        },
                        when (settingsNavigation) {
                            is Deeplink.HomeNetwork -> {
                                Bundle().apply { putInt(SsidFragment.EXTRA_SERVER, settingsNavigation.serverId) }
                            }

                            is Deeplink.Sensor -> {
                                SensorDetailFragment.newInstance(settingsNavigation.sensorId).arguments
                            }

                            is Deeplink.QSTile -> {
                                val tileId = settingsNavigation.tileId
                                Bundle().apply { putString("id", tileId) }
                            }

                            Deeplink.Websocket -> {
                                val servers = serverManager.servers()
                                if (servers.size == 1) {
                                    Bundle().apply { putInt(WebsocketSettingFragment.EXTRA_SERVER, servers[0].id) }
                                } else {
                                    null
                                }
                            }

                            else -> {
                                null
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        setAppActive(false)
    }

    override fun onPause() {
        super.onPause()
        setAppActive(false)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            blurView.setBlurEnabled(isAppLocked())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isFinishing) {
            lifecycleScope.launch {
                if (isAppLocked()) {
                    authenticating = true
                    authenticator.authenticate(getString(commonR.string.biometric_title))
                    blurView.setBlurEnabled(true)
                } else {
                    setAppActive(true)
                    blurView.setBlurEnabled(false)
                }
            }
        }
    }

    private fun settingsActivityAuthenticationResult(result: Int) {
        val isExtAuth = (externalAuthCallback != null)
        Timber.d("settingsActivityAuthenticationResult(): authenticating: $authenticating, externalAuth: $isExtAuth")

        externalAuthCallback?.let {
            if (it(result)) {
                externalAuthCallback = null
            }
        }

        if (authenticating) {
            authenticating = false
            when (result) {
                Authenticator.SUCCESS -> {
                    Timber.d("Authentication successful, unlocking app")
                    blurView.setBlurEnabled(false)
                    setAppActive(true)
                }
                Authenticator.CANCELED -> {
                    Timber.d("Authentication canceled by user, closing activity")
                    finishAffinity()
                }
                else -> Timber.d("Authentication failed, retry attempts allowed")
            }
        }
    }

    /**
     * @return `true` if the app is locked for the active server or the currently visible server
     */
    private suspend fun isAppLocked(): Boolean {
        val serverFragment = supportFragmentManager.findFragmentByTag(ServerSettingsFragment.TAG)
        val serverLocked = serverFragment?.let {
            viewModel.isAppLocked((it as ServerSettingsFragment).getServerId())
        } ?: false
        return serverLocked || viewModel.isAppLocked(ServerManager.SERVER_ID_ACTIVE)
    }

    /**
     * Set the app active for the currently active server, and the currently visible server if
     * different
     */
    private fun setAppActive(active: Boolean) {
        val serverFragment = supportFragmentManager.findFragmentByTag(ServerSettingsFragment.TAG)
        serverFragment?.let { viewModel.setAppActive((it as ServerSettingsFragment).getServerId(), active) }
        viewModel.setAppActive(ServerManager.SERVER_ID_ACTIVE, active)
    }

    /**
     * Reports [tab] as the requested destination via the activity result and finishes this
     * Activity, letting a caller that hosts [HomeBottomNavigationBar] itself switch to that tab
     * instead of just resuming whatever tab was active before Settings was opened.
     */
    private fun finishWithSelectedTab(tab: HomeContentTab) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_TAB, tab))
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Shows or hides this Activity's legacy [Toolbar] chrome.
     *
     * [SettingsFragment] (the Settings overview screen) renders its own native
     * `HATopBar` instead of relying on this Activity's shared toolbar, so it hides the toolbar
     * while resumed and restores it when paused (e.g. navigating to another settings sub-screen,
     * which still relies on this toolbar).
     */
    fun setLegacyToolbarVisible(visible: Boolean) {
        toolbar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun requestAuthentication(title: String, callback: (Int) -> Boolean): Boolean {
        return if (BiometricManager.from(this).canAuthenticate(Authenticator.AUTH_TYPES) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            false
        } else {
            externalAuthCallback = callback
            authenticator.authenticate(title)

            true
        }
    }
}

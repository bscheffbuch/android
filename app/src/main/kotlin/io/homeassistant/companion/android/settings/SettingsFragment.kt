package io.homeassistant.companion.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HATheme
import io.homeassistant.companion.android.settings.assist.AssistSettingsFragment
import io.homeassistant.companion.android.settings.controls.ManageControlsSettingsFragment
import io.homeassistant.companion.android.settings.developer.DeveloperSettingsFragment
import io.homeassistant.companion.android.settings.gestures.GesturesFragment
import io.homeassistant.companion.android.settings.license.LicensesFragment
import io.homeassistant.companion.android.settings.notification.NotificationChannelFragment
import io.homeassistant.companion.android.settings.notification.NotificationHistoryFragment
import io.homeassistant.companion.android.settings.qs.ManageTilesFragment
import io.homeassistant.companion.android.settings.sensor.SensorSettingsFragment
import io.homeassistant.companion.android.settings.sensor.SensorUpdateFrequencyFragment
import io.homeassistant.companion.android.settings.server.ServerSettingsFragment
import io.homeassistant.companion.android.settings.shortcuts.ManageShortcutsSettingsFragment
import io.homeassistant.companion.android.settings.vehicle.ManageAndroidAutoSettingsFragment
import io.homeassistant.companion.android.settings.widgets.ManageWidgetsSettingsFragment

/**
 * Top-level Settings screen, hosting [SettingsScreen]. This Fragment only wires navigation between
 * sub-screens (implemented as sibling fragments) and biometric re-authentication for locked
 * servers; all Settings business logic lives in [SettingsViewModel].
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private val appLockViewModel: AppLockViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HATheme {
                    SettingsScreen(
                        viewModel = viewModel,
                        appLockViewModel = appLockViewModel,
                        onNavigate = ::navigateTo,
                        onRequestAuthentication = ::requestAuthentication,
                    )
                }
            }
        }
    }

    private fun requestAuthentication(title: String, callback: (Int) -> Boolean): Boolean {
        return (requireActivity() as SettingsActivity).requestAuthentication(title, callback)
    }

    private fun navigateTo(destination: SettingsDestination) {
        when (destination) {
            SettingsDestination.Sensors -> parentFragmentManager.commit {
                replace(R.id.content, SensorSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.sensors))
            }
            SettingsDestination.SensorUpdateFrequency -> parentFragmentManager.commit {
                replace(R.id.content, SensorUpdateFrequencyFragment::class.java, null)
                addToBackStack(getString(commonR.string.sensor_update_frequency))
            }
            SettingsDestination.Gestures -> parentFragmentManager.commit {
                replace(R.id.content, GesturesFragment::class.java, null)
                addToBackStack(getString(commonR.string.gestures))
            }
            SettingsDestination.AssistSettings -> parentFragmentManager.commit {
                replace(R.id.content, AssistSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.assist))
            }
            SettingsDestination.NotificationChannels -> parentFragmentManager.commit {
                replace(R.id.content, NotificationChannelFragment::class.java, null)
                addToBackStack(getString(commonR.string.notification_channels))
            }
            SettingsDestination.NotificationHistory -> parentFragmentManager.commit {
                replace(R.id.content, NotificationHistoryFragment::class.java, null)
                addToBackStack(getString(commonR.string.notifications))
            }
            SettingsDestination.ManageDeviceControls -> parentFragmentManager.commit {
                replace(R.id.content, ManageControlsSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.controls_setting_title))
            }
            SettingsDestination.ManageTiles -> parentFragmentManager.commit {
                replace(R.id.content, ManageTilesFragment::class.java, null)
                addToBackStack(getString(commonR.string.tiles))
            }
            SettingsDestination.ManageShortcuts -> parentFragmentManager.commit {
                replace(R.id.content, ManageShortcutsSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.shortcuts))
            }
            SettingsDestination.ManageWidgets -> parentFragmentManager.commit {
                replace(R.id.content, ManageWidgetsSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.widgets))
            }
            SettingsDestination.ManageAndroidAuto -> parentFragmentManager.commit {
                replace(R.id.content, ManageAndroidAutoSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.basic_sensor_name_android_auto))
            }
            SettingsDestination.Developer -> parentFragmentManager.commit {
                replace(R.id.content, DeveloperSettingsFragment::class.java, null)
                addToBackStack(getString(commonR.string.troubleshooting))
            }
            SettingsDestination.Licenses -> parentFragmentManager.commit {
                replace(R.id.content, LicensesFragment::class.java, null)
                addToBackStack(getString(commonR.string.licenses))
            }
            is SettingsDestination.ServerSettings -> parentFragmentManager.commit {
                replace(
                    R.id.content,
                    ServerSettingsFragment::class.java,
                    Bundle().apply { putInt(ServerSettingsFragment.EXTRA_SERVER, destination.serverId) },
                    ServerSettingsFragment.TAG,
                )
                addToBackStack(getString(commonR.string.server_settings))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // This screen renders its own native HATopBar (with the same title), so the shared
        // Activity toolbar used by other settings sub-screens is hidden while this is on top.
        (requireActivity() as SettingsActivity).setLegacyToolbarVisible(false)
        viewModel.refreshOnResume()
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as SettingsActivity).setLegacyToolbarVisible(true)
    }
}

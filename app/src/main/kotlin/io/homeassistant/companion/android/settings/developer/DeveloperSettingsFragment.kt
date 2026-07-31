package io.homeassistant.companion.android.settings.developer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HATheme
import io.homeassistant.companion.android.settings.developer.location.LocationTrackingFragment
import io.homeassistant.companion.android.settings.log.LogFragment
import io.homeassistant.companion.android.settings.server.ServerChooserFragment

@AndroidEntryPoint
class DeveloperSettingsFragment : Fragment() {

    private val viewModel: DeveloperSettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HATheme {
                    DeveloperSettingsScreen(
                        viewModel = viewModel,
                        onShowLogsClicked = ::showLogs,
                        onLocationTrackingClicked = ::showLocationTracking,
                        onRequestServerSelection = ::requestServerSelection,
                    )
                }
            }
        }
    }

    private fun showLogs() {
        parentFragmentManager.commit {
            replace(R.id.content, LogFragment::class.java, null)
            addToBackStack(getString(commonR.string.log))
        }
    }

    private fun showLocationTracking() {
        parentFragmentManager.commit {
            replace(R.id.content, LocationTrackingFragment::class.java, null)
            addToBackStack(getString(commonR.string.location_tracking))
        }
    }

    private fun requestServerSelection(onServerSelected: (Int) -> Unit) {
        parentFragmentManager.setFragmentResultListener(ServerChooserFragment.RESULT_KEY, this) { _, bundle ->
            if (bundle.containsKey(ServerChooserFragment.RESULT_SERVER)) {
                onServerSelected(bundle.getInt(ServerChooserFragment.RESULT_SERVER))
            }
            parentFragmentManager.clearFragmentResultListener(ServerChooserFragment.RESULT_KEY)
        }
        ServerChooserFragment().show(parentFragmentManager, ServerChooserFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(commonR.string.troubleshooting)
    }
}

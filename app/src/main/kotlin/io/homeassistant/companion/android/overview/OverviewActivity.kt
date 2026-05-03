package io.homeassistant.companion.android.overview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.BaseActivity
import io.homeassistant.companion.android.common.compose.theme.HATheme
import io.homeassistant.companion.android.overview.ui.OverviewScreen

@AndroidEntryPoint
class OverviewActivity : BaseActivity() {

    private val viewModel: OverviewViewModel by viewModels()

    companion object {
        fun newInstance(context: Context): Intent =
            Intent(context, OverviewActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HATheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                OverviewScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadEntities() },
                    onToggleEntity = { entityId -> viewModel.toggleEntity(entityId) },
                    onBrightnessChange = { entityId, brightness ->
                        viewModel.setBrightness(entityId, brightness)
                    },
                )
            }
        }
    }
}

package io.homeassistant.companion.android.automations.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import io.homeassistant.companion.android.automations.ui.AutomationsScreen
import kotlinx.serialization.Serializable

@Serializable
internal data object AutomationsRoute

internal fun NavController.navigateToAutomations(navOptions: NavOptions? = null) {
    navigate(AutomationsRoute, navOptions)
}

internal fun NavGraphBuilder.automationsScreen(navController: NavController) {
    composable<AutomationsRoute> {
        AutomationsScreen(
            onNavigateBack = { navController.popBackStack() },
            viewModel = hiltViewModel(),
        )
    }
}

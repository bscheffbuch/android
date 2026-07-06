package io.homeassistant.companion.android.automations

import io.homeassistant.companion.android.common.data.integration.Entity

sealed interface AutomationsUiState {
    data object Loading : AutomationsUiState
    data class Success(val automations: List<Entity>, val scenes: List<Entity>, val isRefreshing: Boolean = false) :
        AutomationsUiState
    data class Error(val message: String) : AutomationsUiState
}

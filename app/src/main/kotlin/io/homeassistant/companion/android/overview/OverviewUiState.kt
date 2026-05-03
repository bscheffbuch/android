package io.homeassistant.companion.android.overview

import io.homeassistant.companion.android.common.data.integration.Entity

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(val entities: List<Entity>) : OverviewUiState
    data class Error(val message: String) : OverviewUiState
}

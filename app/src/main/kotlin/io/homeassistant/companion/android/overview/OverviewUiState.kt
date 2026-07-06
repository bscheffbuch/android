package io.homeassistant.companion.android.overview

import io.homeassistant.companion.android.common.data.integration.Entity

data class OverviewLightGroup(
    val id: String,
    val name: String,
    val entityIds: List<String>,
    val colorArgb: Long? = null,
)

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(
        val entities: List<Entity>,
        val lightGroups: List<OverviewLightGroup>,
        val itemOrder: List<String>,
        val expandedLightGroupIds: Set<String>,
        val displayedAsLightEntityIds: Set<String> = emptySet(),
        val isRefreshing: Boolean = false,
        val isEditMode: Boolean = false,
    ) : OverviewUiState
    data class Error(val message: String) : OverviewUiState
}

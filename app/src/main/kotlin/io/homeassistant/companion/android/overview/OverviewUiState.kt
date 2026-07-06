package io.homeassistant.companion.android.overview

import io.homeassistant.companion.android.common.data.integration.Entity

data class OverviewLightGroup(
    val id: String,
    val name: String,
    val entityIds: List<String>,
    val colorArgb: Long? = null,
)

/**
 * On/off entities that can optionally be shown as a lamp in the Overview — a local per-entity
 * display preference set from the entity detail sheet. Covers switches (including outlets) and
 * input_booleans: the toggleable, non-light entities a user commonly wires to a lamp.
 */
internal fun Entity.supportsDisplayAsLight(): Boolean = domain == "switch" || domain == "input_boolean"

/** Whether this switch reports `device_class: outlet` (rendered with an outlet icon by default). */
internal fun Entity.isOutletSwitch(): Boolean = domain == "switch" && attributes["device_class"] == "outlet"

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

package io.homeassistant.companion.android.automations

/**
 * State of the "save current state as a scene" dialog shown from the Automations & Scenes screen.
 */
sealed interface SaveSceneDialogUiState {

    /** The dialog is not shown. */
    data object Hidden : SaveSceneDialogUiState

    /** The dialog is opening and the list of capturable entities is being fetched. */
    data object Loading : SaveSceneDialogUiState

    /**
     * The dialog is shown with the entities that can be captured into the scene.
     *
     * @property candidates Controllable entities offered to the user, all pre-selected by default.
     * @property isSaving True while the scene is being written to the server.
     */
    data class Ready(val candidates: List<SceneEntityCandidate>, val isSaving: Boolean = false) :
        SaveSceneDialogUiState
}

/**
 * A single entity offered in the save-as-scene dialog.
 *
 * @property entityId Home Assistant entity id, used as the selection key and when capturing state.
 * @property friendlyName Human-readable name shown in the list.
 * @property domain Entity domain (for example "light"), used to pick a representative icon.
 */
data class SceneEntityCandidate(val entityId: String, val friendlyName: String, val domain: String)

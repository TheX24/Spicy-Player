package com.tx24.spicyplayer.tageditor.state

import com.tx24.spicyplayer.library.store.model.tags.SongTags

sealed interface TagEditorState {
    data object Loading : TagEditorState
    data class Loaded(
        val tags: SongTags,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val isFailed: Boolean = false,
    ) : TagEditorState
}
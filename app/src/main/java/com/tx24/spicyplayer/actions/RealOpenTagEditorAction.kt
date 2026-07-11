package com.tx24.spicyplayer.actions

import android.net.Uri
import androidx.navigation.NavHostController
import com.tx24.spicyplayer.tageditor.navigation.TAG_EDITOR_GRAPH
import com.tx24.spicyplayer.ui.actions.OpenTagEditorAction


class RealOpenTagEditorAction(
    private val navHostController: NavHostController
) : OpenTagEditorAction {

    override fun open(songUri: Uri) {
        val encodedUri = Uri.encode(songUri.toString())
        navHostController.navigate("$TAG_EDITOR_GRAPH/$encodedUri")
    }

}
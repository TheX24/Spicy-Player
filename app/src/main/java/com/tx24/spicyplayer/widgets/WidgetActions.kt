package com.tx24.spicyplayer.widgets

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tx24.spicyplayer.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) = withConnectedController(context) {
        prepare()
        playWhenReady = !playWhenReady
    }
}

class NextSongAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) = withConnectedController(context) {
        seekToNext()
    }
}

class PreviousSongAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) = withConnectedController(context) {
        seekToPrevious()
    }
}

/**
 * Connects a [MediaController], runs [action] on the main thread, and always
 * releases the controller — each widget press previously leaked a controller
 * and its binder connection to the service.
 */
private suspend fun withConnectedController(
    context: Context,
    action: MediaController.() -> Unit
) = withContext(Dispatchers.IO) {
    val mc = getMediaControllerFuture(context.applicationContext).get()
    withContext(Dispatchers.Main) {
        try {
            mc.action()
        } finally {
            mc.release()
        }
    }
}

private fun getMediaControllerFuture(context: Context): ListenableFuture<MediaController> {
    val sessionToken =
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    return MediaController.Builder(context, sessionToken)
        .setApplicationLooper(context.mainLooper)
        .buildAsync()
}
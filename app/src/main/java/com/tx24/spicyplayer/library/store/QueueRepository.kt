package com.tx24.spicyplayer.library.store

import android.net.Uri
import androidx.core.net.toUri
import com.tx24.spicyplayer.library.database.dao.QueueDao
import com.tx24.spicyplayer.library.database.entities.queue.QueueEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class QueueRepository @Inject constructor(
    private val queueDao: QueueDao
) {

    // Supervised with a handler so one failed write can't kill the singleton
    // scope and silently disable every subsequent write.
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() +
                CoroutineExceptionHandler { _, e -> Timber.e(e, "Queue write failed") }
    )

    suspend fun getQueue(): List<DBQueueItem> =
        queueDao.getQueue()
            .map { it.toDBQueueItem() }

    fun saveQueueFromDBQueueItems(songs: List<DBQueueItem>) {
        scope.launch {
            queueDao.changeQueue(songs.map { it.toQueueEntity() })
        }
    }

    private fun DBQueueItem.toQueueEntity() =
        QueueEntity(
            0,
            songUri.toString(),
            title,
            artist,
            album
        )

    private fun QueueEntity.toDBQueueItem(): DBQueueItem {
        return DBQueueItem(
            songUri = songUri.toUri(),
            title = title,
            artist = artist.orEmpty(),
            album = albumTitle.orEmpty(),
        )
    }

}

data class DBQueueItem(
    val songUri: Uri,
    val title: String,
    val artist: String,
    val album: String
)
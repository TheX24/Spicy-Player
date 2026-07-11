package com.tx24.spicyplayer.library.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tx24.spicyplayer.library.database.dao.ActivityDao
import com.tx24.spicyplayer.library.database.dao.BlacklistedFoldersDao
import com.tx24.spicyplayer.library.database.dao.LyricsDao
import com.tx24.spicyplayer.library.database.dao.PlaylistDao
import com.tx24.spicyplayer.library.database.dao.QueueDao
import com.tx24.spicyplayer.library.database.entities.activity.ListeningSessionEntity
import com.tx24.spicyplayer.library.database.entities.lyrics.LyricsEntity
import com.tx24.spicyplayer.library.database.entities.playlist.PlaylistEntity
import com.tx24.spicyplayer.library.database.entities.playlist.PlaylistsSongsEntity
import com.tx24.spicyplayer.library.database.entities.prefs.BlacklistedFolderEntity
import com.tx24.spicyplayer.library.database.entities.queue.QueueEntity


@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistsSongsEntity::class,
        BlacklistedFolderEntity::class,
        QueueEntity::class,
        ListeningSessionEntity::class,
        LyricsEntity::class
    ],
    version = 5, exportSchema = false
)
abstract class SpicyDatabase : RoomDatabase() {

    abstract fun playlistsDao(): PlaylistDao
    abstract fun blacklistDao(): BlacklistedFoldersDao
    abstract fun queueDao(): QueueDao
    abstract fun activityDao(): ActivityDao
    abstract fun lyricsDao(): LyricsDao

}
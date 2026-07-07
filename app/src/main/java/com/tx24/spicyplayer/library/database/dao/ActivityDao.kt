package com.tx24.spicyplayer.library.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tx24.spicyplayer.library.database.entities.LISTENING_SESSION_TABLE
import com.tx24.spicyplayer.library.database.entities.activity.ListeningSessionEntity


@Dao
interface ActivityDao {

    @Insert
    suspend fun insertListeningSession(l: ListeningSessionEntity)

    @Query("SELECT * FROM $LISTENING_SESSION_TABLE")
    suspend fun getAllListeningSessions(): List<ListeningSessionEntity>
}
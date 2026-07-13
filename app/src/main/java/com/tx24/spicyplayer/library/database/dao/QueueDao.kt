package com.tx24.spicyplayer.library.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tx24.spicyplayer.library.database.entities.queue.QUEUE_TABLE
import com.tx24.spicyplayer.library.database.entities.queue.QueueEntity


@Dao
interface QueueDao {

    @Transaction
    suspend fun changeQueue(queue: List<QueueEntity>) {
        deleteQueue()
        insertQueue(queue)
    }

    @Insert
    suspend fun insertQueue(queue: List<QueueEntity>)

    @Query("DELETE FROM $QUEUE_TABLE")
    suspend fun deleteQueue()

    @Query("SELECT * FROM $QUEUE_TABLE")
    suspend fun getQueue(): List<QueueEntity>

}
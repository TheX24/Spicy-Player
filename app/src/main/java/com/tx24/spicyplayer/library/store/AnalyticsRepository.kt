package com.tx24.spicyplayer.library.store

import com.tx24.spicyplayer.library.database.dao.ActivityDao
import com.tx24.spicyplayer.library.database.entities.activity.ListeningSessionEntity
import com.tx24.spicyplayer.model.activity.ListeningSession
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AnalyticsRepository @Inject constructor(
    private val activityDao: ActivityDao
) {

    // Supervised with a handler so one failed write can't kill the singleton
    // scope and silently disable every subsequent write.
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() +
                CoroutineExceptionHandler { _, e -> Timber.e(e, "Analytics write failed") }
    )

    fun insertListeningSession(l: ListeningSession) {
        scope.launch {
            activityDao.insertListeningSession(l.toDBEntity())
        }
    }

    suspend fun getAllListeningSessions(): List<ListeningSession> {
        return activityDao.getAllListeningSessions().map { it.toModel() }
    }

    private fun ListeningSession.toDBEntity() =
        ListeningSessionEntity(0, songUri, songName, albumName, startTime.time, durationSeconds)

    private fun ListeningSessionEntity.toModel() =
        ListeningSession(Date(startTimeEpoch), durationSeconds, songUri, songName, albumName)

}
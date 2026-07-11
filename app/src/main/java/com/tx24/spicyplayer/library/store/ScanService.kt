package com.tx24.spicyplayer.library.store

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.tx24.spicyplayer.library.store.model.song.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScanService : Service() {

    @Inject lateinit var scanStateRepository: ScanStateRepository

    @Inject lateinit var userPreferencesRepository: com.tx24.spicyplayer.library.store.preferences.UserPreferencesRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    private val _progressFlow = MutableSharedFlow<ScanProgress>(replay = 1)
    val progressFlow = _progressFlow.asSharedFlow()

    private val _resultFlow = MutableSharedFlow<List<Song>>(replay = 1)
    val resultFlow = _resultFlow.asSharedFlow()

    inner class ScanBinder : Binder() {
        fun getService(): ScanService = this@ScanService
    }

    private val binder = ScanBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        ScanNotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scanPath = intent?.getStringExtra("scan_path") ?: "/sdcard/Music/"
        
        val notification = ScanNotificationHelper.buildScanNotification(this, "Preparing to scan...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ScanNotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(ScanNotificationHelper.NOTIFICATION_ID, notification)
        }

        startScan(scanPath)
        
        return START_NOT_STICKY
    }

    private fun startScan(scanPath: String) {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                val excludedFolders = userPreferencesRepository.librarySettingsFlow.map { it.excludedFolders }.first()
                val results = performScan(this@ScanService, scanPath, excludedFolders) { progress ->
                    CoroutineScope(Dispatchers.Main).launch {
                        scanStateRepository.scanProgress.value = progress
                        val currentHistory = scanStateRepository.scanHistory.value.toMutableList()
                        val msg = if (progress.summary.isNotEmpty()) progress.summary else progress.phase
                        if (currentHistory.isEmpty() || currentHistory.last() != msg) {
                            currentHistory.add(msg)
                            if (currentHistory.size > 5) currentHistory.removeAt(0)
                            scanStateRepository.scanHistory.value = currentHistory
                        }
                        
                        // Update Notification
                        val content = if (progress.summary.isNotEmpty()) progress.summary else progress.phase
                        ScanNotificationHelper.updateNotification(
                            this@ScanService, 
                            content, 
                            progress.currentCount, 
                            if (progress.totalCount > 0) progress.totalCount else -1
                        )
                    }
                }
                
                _resultFlow.emit(results)
                Log.d("ScanService", "Scan completed: ${results.size} songs")
            } catch (e: Exception) {
                Log.e("ScanService", "Scan failed", e)
            } finally {
                scanStateRepository.scanProgress.value = null
                stopForeground(true)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

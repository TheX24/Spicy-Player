package com.tx24.spicyplayer.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.tx24.spicyplayer.util.NotificationHelper
import com.tx24.spicyplayer.util.ScanProgress
import com.tx24.spicyplayer.util.performScan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.tx24.spicyplayer.models.Song
import java.io.File

class ScanService : Service() {

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
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scanPath = intent?.getStringExtra("scan_path") ?: "/sdcard/Music/"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                NotificationHelper.buildScanNotification(this, "Preparing to scan..."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                NotificationHelper.buildScanNotification(this, "Preparing to scan...")
            )
        }

        startScan(scanPath)
        
        return START_NOT_STICKY
    }

    private fun startScan(scanPath: String) {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                val results = performScan(this@ScanService, scanPath) { progress ->
                    CoroutineScope(Dispatchers.Main).launch {
                        _progressFlow.emit(progress)
                        
                        // Update Notification
                        val content = if (progress.summary.isNotEmpty()) progress.summary else progress.phase
                        NotificationHelper.updateNotification(
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

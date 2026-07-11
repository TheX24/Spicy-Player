package com.tx24.spicyplayer

import android.app.Application
import com.tx24.spicyplayer.BuildConfig
import com.tx24.spicyplayer.widgets.WidgetManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject


@HiltAndroidApp
class SpicyApplication : Application() {

    @Inject
    lateinit var widgetManager: WidgetManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

}
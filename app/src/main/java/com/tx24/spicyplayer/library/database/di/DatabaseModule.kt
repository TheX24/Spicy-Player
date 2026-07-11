package com.tx24.spicyplayer.library.database.di

import android.content.Context
import androidx.room.Room
import com.tx24.spicyplayer.library.database.SpicyDatabase
import com.tx24.spicyplayer.library.database.entities.DB_NAME
import com.tx24.spicyplayer.library.database.migrations.MIGRATION_3_4
import com.tx24.spicyplayer.library.database.migrations.MIGRATION_4_5
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideRoomDatabase(
        @ApplicationContext context: Context
    ): SpicyDatabase =
        Room.databaseBuilder(context, SpicyDatabase::class.java, name = DB_NAME)
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

    @Singleton
    @Provides
    fun providePlaylistDao(
        appDatabase: SpicyDatabase
    ) = appDatabase.playlistsDao()

    @Singleton
    @Provides
    fun provideBlacklistedFoldersDao(
        appDatabase: SpicyDatabase
    ) = appDatabase.blacklistDao()

    @Singleton
    @Provides
    fun provideQueueDao(
        appDatabase: SpicyDatabase
    ) = appDatabase.queueDao()

    @Singleton
    @Provides
    fun provideActivityDao(
        appDatabase: SpicyDatabase
    ) = appDatabase.activityDao()

    @Singleton
    @Provides
    fun provideLyricsDao(
        appDatabase: SpicyDatabase
    ) = appDatabase.lyricsDao()

}

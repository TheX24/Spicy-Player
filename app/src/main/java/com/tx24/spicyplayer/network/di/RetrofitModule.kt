package com.tx24.spicyplayer.network.di

import android.content.Context
import com.tx24.spicyplayer.network.service.LyricsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


annotation class LyricsRetrofitService

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {


    @Singleton
    @Provides
    fun provideLyricsService(
        @LyricsRetrofitService lyricsRetrofitService: Retrofit
    ) = lyricsRetrofitService.create<LyricsService>()

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

    @LyricsRetrofitService
    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient) = Retrofit.Builder()
        .baseUrl(LyricsService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

}
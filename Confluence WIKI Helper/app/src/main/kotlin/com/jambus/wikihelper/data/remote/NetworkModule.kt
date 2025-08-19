package com.jambus.wikihelper.data.remote

import com.jambus.wikihelper.data.remote.api.DifyApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .build()
                android.util.Log.d("DifyAPI", "Request URL: ${request.url}")
                android.util.Log.d("DifyAPI", "Request method: ${request.method}")
                android.util.Log.d("DifyAPI", "Request headers: ${request.headers}")
                val response = chain.proceed(request)
                android.util.Log.d("DifyAPI", "Response code: ${response.code}")
                if (!response.isSuccessful) {
                    android.util.Log.e("DifyAPI", "Response error: ${response.message}")
                }
                response
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://123.60.144.244/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDifyApiService(retrofit: Retrofit): DifyApiService {
        return retrofit.create(DifyApiService::class.java)
    }
}
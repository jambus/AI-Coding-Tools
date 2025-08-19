package com.jambus.wikihelper.data.remote

import com.jambus.wikihelper.data.remote.api.DifyApiService
import com.jambus.wikihelper.data.security.SecurityManager
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
    fun provideOkHttpClient(securityManager: SecurityManager): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                
                // 动态添加API Key到请求头
                val apiKey = securityManager.getApiKey()
                if (!apiKey.isNullOrEmpty()) {
                    // 确保API Key格式正确，避免重复Bearer前缀
                    val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) {
                        apiKey
                    } else {
                        "Bearer $apiKey"
                    }
                    requestBuilder.addHeader("Authorization", authHeader)
                    
                    // 脱敏显示API Key用于调试
                    val maskedKey = if (authHeader.length > 20) {
                        authHeader.take(20) + "****"
                    } else {
                        authHeader.take(10) + "****"
                    }
                    android.util.Log.d("DifyAPI", "Added Authorization header: $maskedKey")
                } else {
                    android.util.Log.w("DifyAPI", "No API Key found in SecurityManager")
                }
                
                val request = requestBuilder.build()
                android.util.Log.d("DifyAPI", "Request URL: ${request.url}")
                android.util.Log.d("DifyAPI", "Request method: ${request.method}")
                
                // 专门检查Authorization头
                val authHeaderValue = request.header("Authorization")
                if (authHeaderValue != null) {
                    val maskedAuth = if (authHeaderValue.length > 20) {
                        authHeaderValue.take(20) + "****"
                    } else {
                        authHeaderValue.take(10) + "****"
                    }
                    android.util.Log.d("DifyAPI", "✓ Authorization header present: $maskedAuth")
                } else {
                    android.util.Log.e("DifyAPI", "✗ Authorization header missing!")
                }
                
                android.util.Log.d("DifyAPI", "All headers: ${request.headers}")
                
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
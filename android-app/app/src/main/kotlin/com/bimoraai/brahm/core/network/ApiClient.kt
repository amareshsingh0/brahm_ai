package com.bimoraai.brahm.core.network

import com.bimoraai.brahm.BuildConfig
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenDataStore: TokenDataStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        // Minimal plain client used only for token refresh (avoids circular dependency)
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(20, 5, TimeUnit.MINUTES))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Auto-attach JWT token to every request
            .addInterceptor { chain ->
                val token = runBlocking { tokenDataStore.accessToken.firstOrNull() }
                val builder = chain.request().newBuilder()
                    .header("X-Client", "android")
                if (token != null) builder.header("Authorization", "Bearer $token")
                chain.proceed(builder.build())
            }
            // Auto-refresh expired JWT on 401 — retries the original request once
            .authenticator { _, response ->
                // Don't retry if already retried, or if no auth was sent
                if (response.request.header("X-Auth-Retry") != null) return@authenticator null
                val refreshToken = runBlocking { tokenDataStore.refreshToken.firstOrNull() }
                    ?: return@authenticator null

                val body = """{"refresh_token":"$refreshToken"}"""
                    .toRequestBody("application/json".toMediaType())
                val refreshRequest = Request.Builder()
                    .url("${BuildConfig.BASE_URL}auth/refresh")
                    .post(body)
                    .build()

                val refreshResponse = try {
                    refreshClient.newCall(refreshRequest).execute()
                } catch (_: Exception) { return@authenticator null }

                if (!refreshResponse.isSuccessful) return@authenticator null

                val bodyStr = refreshResponse.body?.string() ?: return@authenticator null
                val newAccess  = try { JSONObject(bodyStr).optString("access_token")  } catch (_: Exception) { null }
                val newRefresh = try { JSONObject(bodyStr).optString("refresh_token") } catch (_: Exception) { null }

                if (newAccess.isNullOrBlank()) return@authenticator null

                runBlocking { tokenDataStore.saveTokens(newAccess, newRefresh ?: refreshToken) }

                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .header("X-Auth-Retry", "true")
                    .build()
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}

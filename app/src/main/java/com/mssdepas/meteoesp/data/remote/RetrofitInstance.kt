package com.mssdepas.meteoesp.data.remote

import com.mssdepas.meteoesp.util.AppLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://www.el-tiempo.net/api/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request: Request = chain.request().newBuilder()
                .header("User-Agent", "MeteoEspApp/1.0")
                .build()
            chain.proceed(request)
        })
        .addInterceptor(HttpLoggingInterceptor().apply { // Add this interceptor
            level = HttpLoggingInterceptor.Level.BODY // Or Level.HEADERS, Level.BASIC, Level.NONE
        })
        .build()

    val api: TiempoNetApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(TiempoNetApiService::class.java)
    }
}
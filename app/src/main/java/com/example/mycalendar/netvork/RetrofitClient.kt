package com.example.mycalendar.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"  // Android 에뮬레이터용 localhost 주소

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)  // 반드시 '/'로 끝나야 함
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

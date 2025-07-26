package com.example.mycalendar.network

import android.content.Context // new
import okhttp3.OkHttpClient    // new
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8081/"

    // ✅ NEW: context를 외부에서 넣어주기 위한 초기화
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
    }

    // ✅ NEW: 토큰 인터셉터 적용한 OkHttpClient 생성
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(context))
            .build()
    }

    // ✅ 수정: okHttpClient 추가
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // new
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ApiService 인터페이스 구현체 제공
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

package com.example.mycalendar.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.mycalendar.network.HolidayApiService

object RetrofitClient {

    // ✅ 백엔드 서버 주소 수정 (Spring Boot 0.0.0.0:8080)
    //로컬 주소
    private const val BASE_URL = "http://10.0.2.2:8080/"
    //서버 주소
    //private const val BASE_URL = "https://schedule-backend-vmhb.onrender.com/"


    private var context: Context? = null
    private var prefs: SharedPreferences? = null

    // HTTP 요청/응답 로깅
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // JWT 토큰 자동 추가 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = prefs?.getString("access_token", null)

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
        }

        val response = chain.proceed(newRequest)
        Log.d("RetrofitClient", "🌐 Request to: ${newRequest.url}")
        Log.d("RetrofitClient", "📊 Response code: ${response.code}")

        response
    }
    // 🎌 공휴일 API용 별도 Retrofit 인스턴스 추가
    private val holidayRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 🎌 HolidayApiService 인스턴스 추가
    val holidayApiService: HolidayApiService by lazy {
        holidayRetrofit.create(HolidayApiService::class.java)
    }

    // OkHttp 클라이언트 설정
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit 인스턴스
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API 서비스 인스턴스
    val apiService: ApiService = retrofit.create(ApiService::class.java)

    fun init(context: Context) {
        this.context = context
        this.prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        Log.d("RetrofitClient", "✅ RetrofitClient 초기화 완료")
        Log.d("RetrofitClient", "🔗 BASE_URL: $BASE_URL")
        Log.d("RetrofitClient", "🔑 저장된 토큰: ${prefs?.getString("access_token", "없음")?.take(10)}...")
    }

    // 서버 연결 상태 확인
    fun getServerStatus(): String {
        return "서버 주소: $BASE_URL\n연결 상태: ${if (context != null) "초기화됨" else "미초기화"}"
    }
}
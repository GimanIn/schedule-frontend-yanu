package com.example.mycalendar.network

import com.example.mycalendar.model.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // 🔗 1단계: 서버 연결 확인용
    @GET("api/test")
    fun testConnection(): Call<String>

    // 🆔 2단계: 아이디 중복 확인
    @GET("api/user/check-id")
    fun checkUserId(@Query("userId") userId: String): Call<ApiResponse<Unit>>

    // 👤 3단계: 회원가입
    @POST("api/user/signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    // 🔐 4단계: 로그인
    @POST("/api/user/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>

    // 📱 5단계: SMS 인증 요청
    @POST("api/auth/send-sms")
    fun sendSms(@Body request: SendSMSRequest): Call<ApiResponse<Unit>>

    // ✅ 6단계: 인증번호 확인
    @POST("/api/auth/verify-sms")
    fun verifySms(@Body request: VerifySMSRequest): Call<ApiResponse<Unit>>

    // 📅 일정 관련 ---------------------------------

    @POST("/api/schedules")
    fun createSchedule(@Body request: ScheduleRequest): Call<ApiResponse<ScheduleResponse>>

    @PUT("/api/schedules/{id}")
    fun updateSchedule(@Path("id") scheduleId: Long, @Body request: ScheduleRequest): Call<ApiResponse<ScheduleResponse>>

    @DELETE("/api/schedules/{id}")
    fun deleteSchedule(@Path("id") scheduleId: Long): Call<ApiResponse<Unit>>

    @GET("/api/schedules/{id}")
    fun getSchedule(@Path("id") scheduleId: Long): Call<ApiResponse<ScheduleResponse>>

    @GET("/api/schedules")
    fun getSchedulesByDate(@Query("date") date: String): Call<ApiResponse<List<ScheduleResponse>>>

    @GET("/api/schedules/search")
    fun searchSchedules(@Query("keyword") keyword: String): Call<ApiResponse<List<ScheduleResponse>>>

    // 🤝 공유 관련 ----------------------------------

    @POST("/api/schedules/{id}/share")
    fun shareSchedule(@Path("id") scheduleId: Long, @Query("receiverId") receiverId: Long): Call<ApiResponse<String>>

    @GET("/api/schedules/shared/sent")
    fun getSentSharedSchedules(): Call<ApiResponse<List<SharedScheduleResponse>>>

    @GET("/api/schedules/shared/received")
    fun getReceivedSharedSchedules(): Call<ApiResponse<List<SharedScheduleResponse>>>

    @PUT("/api/schedules/shared/{id}/accept")
    fun acceptShare(@Path("id") shareId: Long): Call<ApiResponse<String>>

    @PUT("/api/schedules/shared/{id}/cancel")
    fun cancelShare(@Path("id") shareId: Long): Call<ApiResponse<String>>

    // 🤖 AI 요약 ------------------------------------

    @GET("/api/ai/summary")
    fun getAiSummary(@Query("date") date: String): Call<ApiResponse<AiSummaryResponse>>

    // 🔄 AccessToken 갱신 (RefreshToken 기반) ✅ NEW
    @POST("/api/auth/refresh-token")
    fun refreshAccessToken(
        @Header("Authorization") refreshToken: String
    ): Call<ApiResponse<LoginResponse>>
}

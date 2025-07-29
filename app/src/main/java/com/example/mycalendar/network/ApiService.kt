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
    fun createSchedule(@Body request: ScheduleRequest): Call<ApiResponse<ScheduleResponse>> // 일정 추가

    // ✅ ✅ ✅ 여기에 추가하세요!
    @GET("/api/schedules/range")
    fun getSchedulesByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Call<ApiResponse<List<ScheduleResponse>>>

    @PUT("/api/schedules/{id}")
    fun updateSchedule(@Path("id") scheduleId: Long, @Body request: ScheduleRequest): Call<ApiResponse<ScheduleResponse>> // 일정 수정

    @DELETE("/api/schedules/{id}")
    fun deleteSchedule(@Path("id") scheduleId: Long): Call<ApiResponse<Unit>> // 일정 삭제

    @GET("/api/schedules/{id}")
    fun getSchedule(@Path("id") scheduleId: Long): Call<ApiResponse<ScheduleResponse>> // 특정 일정 조회

    @GET("/api/schedules")
    fun getSchedulesByDate(@Query("date") date: String): Call<ApiResponse<List<ScheduleResponse>>> // 날짜별 일정 조회

    @GET("/api/schedules/search")
    fun searchSchedules(@Query("keyword") keyword: String): Call<ApiResponse<List<ScheduleResponse>>> // 일정 검색

    // 🤝 공유 관련 ----------------------------------

    @POST("/api/schedules/{id}/share")
    fun shareSchedule(@Path("id") scheduleId: Long, @Query("receiverId") receiverId: Long): Call<ApiResponse<String>> // 일정 공유

    @GET("/api/schedules/shared/sent")
    fun getSentSharedSchedules(): Call<ApiResponse<List<SharedScheduleResponse>>> // 보낸 일정 목록 조회

    @GET("/api/schedules/shared/received")
    fun getReceivedSharedSchedules(): Call<ApiResponse<List<SharedScheduleResponse>>> // 받은 일정 목록 조회

    @PUT("/api/schedules/shared/{id}/accept")
    fun acceptShare(@Path("id") shareId: Long): Call<ApiResponse<String>> // 공유된 일정 수락

    @PUT("/api/schedules/shared/{id}/cancel")
    fun cancelShare(@Path("id") shareId: Long): Call<ApiResponse<String>> // 공유된 일정 취소

    // 🤖 AI 요약 ------------------------------------

    @POST("/api/ai/summary")
    fun getAiSummary(@Query("date") date: String): Call<AiSummaryResponse> // AI 요약

    // 🔄 AccessToken 갱신 (RefreshToken 기반)
    @POST("/api/auth/refresh-token")
    fun refreshAccessToken(
        @Header("Authorization") refreshToken: String
    ): Call<ApiResponse<LoginResponse>> // AccessToken 갱신


    // 👤 마이페이지 관련 ------------------------------

    // 1. 유저 정보 조회 (이름 + 아이디)
    @GET("/api/user/me")
    fun getUserInfo(
        @Header("Authorization") token: String
    ): Call<ApiResponse<UserInfoResponse>>

    // 2. 비밀번호 변경
    @POST("/api/user/change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<ApiResponse<Unit>>

    // 3. 계정 삭제
    @DELETE("/api/user")
    fun deleteAccount(
        @Header("Authorization") token: String
    ): Call<ApiResponse<Unit>>
}

package com.example.mycalendar.network

import com.example.mycalendar.model.LoginRequest
import com.example.mycalendar.model.SignupRequest
import com.example.mycalendar.model.SignupResponse
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.model.SendSMSRequest
import com.example.mycalendar.model.VerifySMSRequest


import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // 1단계: 서버 연결 확인용
    @GET("api/test")
    fun testConnection(): Call<String>

    // 2단계: 아이디 중복 확인
    @GET("api/user/check-id")
    fun checkUserId(@Query("userId") userId: String): Call<ApiResponse<Unit>>

    // 3단계: 회원가입
    @POST("api/user/signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    // 4단계: 로그인
    @POST("api/user/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<String>> // JWT 토큰을 String으로 받음

    // 17단계: SMS 인증 요청
    @POST("api/auth/send-sms")
    fun sendSms(@Body request: SendSMSRequest): Call<ApiResponse<Unit>>

    // 18단계: 인증번호 확인
    @POST("/api/auth/verify-sms")
    fun verifySms(@Body request: VerifySMSRequest): Call<ApiResponse<Unit>>

}

package com.example.mycalendar.model

data class LoginResponse(
    val token: String,
    val refreshToken: String, // ✅ 추가
    val userId: String,
    val name: String,
    val phone: String
)

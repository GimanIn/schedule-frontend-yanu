package com.example.mycalendar.model

data class SignupResponse(
    val success: Boolean,
    val message: String,
    val data: Any?, // 필요 시 UserResponse 등으로 타입 지정 가능
    val code: Int
)

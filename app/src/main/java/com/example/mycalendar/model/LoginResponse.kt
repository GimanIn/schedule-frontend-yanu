package com.example.mycalendar.model

data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
    val phone: String
)

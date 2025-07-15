package com.example.mycalendar.model

data class SignupRequest(
    val userId: String,
    val name: String,
    val phone: String,
    val password: String
)

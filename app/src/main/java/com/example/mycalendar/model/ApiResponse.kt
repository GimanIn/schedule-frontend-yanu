package com.example.mycalendar.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val code: Int
)

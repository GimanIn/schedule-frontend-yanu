package com.example.mycalendar.model

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

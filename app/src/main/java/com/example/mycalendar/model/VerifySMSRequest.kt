package com.example.mycalendar.model

import com.example.mycalendar.model.VerifySMSRequest



data class VerifySMSRequest(
    val phone: String,
    val code: String
)

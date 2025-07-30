package com.example.mycalendar.model

data class AlarmResponse(
    val id: Long,
    val scheduleId: Long,
    val scheduleTitle: String,        // ✅ 백엔드에 포함됨
    val alarmTime: String,            // ✅ LocalDateTime → String (ex: 2025-07-30T09:00:00)
    val message: String,
    val isSent: Boolean,
    val sentAt: String?,              // ✅ nullable
    val createdAt: String             // ✅ 생성일 포함됨
)

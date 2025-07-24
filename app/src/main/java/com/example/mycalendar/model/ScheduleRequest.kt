package com.example.mycalendar.model

// 🔧 NEW: Schedule 클래스 import
import com.example.mycalendar.model.Schedule

// model/ScheduleRequest.kt
data class ScheduleRequest(
    val title: String,
    val memo: String?,
    val location: String?,
    val category: String?,
    val scheduledDate: String?, // 기존 필드 유지 (호환성)
    val startDate: String,      // 새로 추가
    val endDate: String,        // 새로 추가
    val startTime: String?,
    val endTime: String?,
    val allDay: Boolean,
    val isConfirmed: Boolean,
    val color: String,
    val alarmOn: Boolean,
    val copiedFromScheduleId: Long?
)

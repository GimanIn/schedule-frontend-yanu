package com.example.mycalendar.model

data class ScheduleResponse(
    val id: Long,
    val title: String,
    val memo: String?,                // ⚠️ nullable로 변경
    val location: String?,            // ⚠️ nullable로 변경
    val category: String?,            // ⚠️ nullable로 변경
    val scheduleDate: String,         // LocalDate → String
    val startTime: String?,           // LocalTime → String
    val endTime: String?,             // LocalTime → String
    val allDay: Boolean,
    val isConfirmed: Boolean,
    val color: String,
    val alarmOn: Boolean,
    val isDeleted: Boolean,           // ✅ 삭제 여부
    val copiedFromScheduleId: Long?,  // ✅ 복사된 일정의 ID (nullable)
    val createdAt: String?,           // ✅ 생성 시간
    val updatedAt: String?            // ✅ 수정 시간
)
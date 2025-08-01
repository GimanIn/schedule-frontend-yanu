// AlarmResponse.kt → Alarm.kt로 파일명 변경
package com.example.mycalendar.model

data class Alarm(  // AlarmResponse → Alarm으로 변경
    val id: Long,
    val scheduleId: Long,
    val scheduleTitle: String,
    val alarmTime: String,
    val message: String,
    val isSent: Boolean,
    val sentAt: String?,
    val createdAt: String
)
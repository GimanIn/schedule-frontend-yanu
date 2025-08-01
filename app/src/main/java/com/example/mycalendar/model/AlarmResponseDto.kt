// AlarmResponseDto.kt
package com.example.mycalendar.model

data class AlarmResponseDto(

    val id: Long,
    val scheduleId: Long,
    val scheduleTitle: String,
    val alarmTime: String,  // 서버에서 LocalDateTime을 ISO 문자열로 주기 때문에 String으로 받음
    val message: String,
    val isSent: Boolean,
    val sentAt: String?,  // null 허용
    val createdAt: String
)
fun AlarmResponseDto.toAlarm(): Alarm {
    return Alarm(
        id = this.id,
        scheduleId = this.scheduleId,
        scheduleTitle = this.scheduleTitle,
        alarmTime = this.alarmTime,
        message = this.message,
        isSent = this.isSent,
        sentAt = this.sentAt,
        createdAt = this.createdAt
    )
}
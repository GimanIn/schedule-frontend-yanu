// TodayAlarmResponse.kt (새 파일 생성)
package com.example.mycalendar.model

data class TodayAlarmResponse(
    val success: Boolean,
    val message: String,
    val data: TodayAlarmData,
    val code: Int
)

data class TodayAlarmData(
    val alarms: List<AlarmResponseDto>,  // 🔧 여기 수정
    val totalCount: Int,
    val readyToSendCount: Int,
    val alreadySentCount: Int,
    val date: String
)
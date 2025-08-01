// TodayAlarmResponse.kt (새 파일 생성)
package com.example.mycalendar.model

data class TodayAlarmResponse(
    val success: Boolean,
    val message: String,
    val data: TodayAlarmData,
    val code: Int
)

data class TodayAlarmData(
    val date: String,
    val alarms: List<Alarm>,  // 위에서 이름 변경한 Alarm 클래스 사용
    val totalCount: Int,
    val readyToSendCount: Int,
    val alreadySentCount: Int
)
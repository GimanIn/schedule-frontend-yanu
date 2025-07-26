package com.example.mycalendar.model

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import android.graphics.Color


// UI에 표시해야 할 일정 데이터 구조
data class Schedule(
    val id: Long? = null,                  // 서버 DB 식별자
    val title: String,                     // 일정 제목 (필수)
    val memo: String = "",                 // 메모 (기본값 "")
    val location: String? = null,          // 위치 (nullable)
    val category: String? = null,          // 카테고리 (nullable)
    var color: Int = Color.parseColor("#4285F4"),                    // 색상 코드 (Int)
    val startDate: LocalDate,              // 시작 날짜
    val endDate: LocalDate,                // 종료 날짜
    val startTime: LocalTime = LocalTime.of(9, 0),  // 기본 시작 시간 09:00
    val endTime: LocalTime = LocalTime.of(10, 0),   // 기본 종료 시간 10:00
    val isConfirmed: Boolean = false,      // 확정 여부
    var alarmOn: Boolean = false,          // 알람 여부
    val isDeleted: Boolean? = null,        // 삭제 여부 (nullable)
    val copiedFromScheduleId: Long? = null,// 복사된 일정의 ID (nullable)
    val createdAt: String? = null,         // 일정 생성 시간 (nullable)
    val updatedAt: String? = null,         // 일정 수정 시간 (nullable)
    val scheduledDate: LocalDate           // 일정 렌더링 기준 날짜
) : Serializable{
    val startDateTime: LocalDateTime
        get() = LocalDateTime.of(startDate, startTime)

    val endDateTime: LocalDateTime
        get() = LocalDateTime.of(endDate, endTime)
}


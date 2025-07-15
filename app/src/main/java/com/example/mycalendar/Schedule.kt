package com.example.mycalendar

import java.io.Serializable
import java.time.LocalDateTime

// 일정 하나에 대한 모든 정보를 담는 데이터 클래스
data class Schedule(
    val title: String,
    val startDateTime: LocalDateTime?, // 시작 날짜와 시간
    val endDateTime: LocalDateTime?,   // 종료 날짜와 시간
    val color: Int,
    val isAlarmOn: Boolean,
    val memo: String,
    val isConfirmed: Boolean,
    val isPostponed: Boolean
) : Serializable
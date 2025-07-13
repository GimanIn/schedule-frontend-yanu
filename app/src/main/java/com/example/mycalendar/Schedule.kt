package com.example.mycalendar

import java.time.LocalTime

// 일정 하나에 대한 모든 정보를 담는 데이터 클래스
data class Schedule(
    val title: String,
    val startTime: LocalTime?, // 시작 시간
    val endTime: LocalTime?,   // 종료 시간
    val color: Int,
    val isAlarmOn: Boolean,
    val memo: String,
    val isConfirmed: Boolean,
    val isPostponed: Boolean
)
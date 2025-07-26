package com.example.mycalendar.model

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

// UI에 표시해야 할 일정 데이터 구조
data class Schedule(
    val id: Long?,                   // 서버 DB 식별자
    val title: String,               // 일정 제목 (필수)
    val memo: String?,               // 메모 (nullable)
    val location: String?,           // 위치 (nullable)
    val category: String?,           // 카테고리 (nullable)
    val color: Int,                  // 색상 코드 (Int)
    val startDate: LocalDate,        // 시작 날짜 (중요)
    val endDate: LocalDate,          // 종료 날짜 (중요) ← ✅ 오타 수정
    val startTime: LocalTime?,       // 시작 시간 (nullable)
    val endTime: LocalTime?,         // 종료 시간 (nullable)
    val isConfirmed: Boolean,        // 확정 여부
    val alarmOn: Boolean,            // 알람 여부
    val isDeleted: Boolean?,         // 삭제 여부 (nullable)
    val copiedFromScheduleId: Long?, // 복사된 일정의 ID (nullable)
    val createdAt: String?,          // 일정 생성 시간 (nullable)
    val updatedAt: String?,          // 일정 수정 시간 (nullable)
    val scheduledDate: LocalDate     // ✅ 일정 렌더링 기준 날짜 (마지막 → 쉼표 ❌)
) : Serializable

package com.example.mycalendar.mapper

import android.graphics.Color
import com.example.mycalendar.model.Schedule
import com.example.mycalendar.model.ScheduleResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object ScheduleMapper {
    fun toSchedule(responseList: List<ScheduleResponse>): List<Schedule> {
        return responseList.map { response ->
            // ✅ NEW: 날짜/시간 변환 처리
            val startDateTime = response.startTime?.let { LocalDateTime.parse(it) }
            val endDateTime = response.endTime?.let { LocalDateTime.parse(it) }

            Schedule(
                id = response.id,
                title = response.title,
                memo = response.memo,
                location = response.location,
                category = response.category,
                color = Color.parseColor(response.color), // 🔧 String → Int 변환
                startDate = startDateTime?.toLocalDate() ?: LocalDate.now(), // ✅ startDate 필수, 없으면 기본값 사용
                endDate = endDateTime?.toLocalDate() ?: LocalDate.now(),   // ✅ endDate 필수, 없으면 기본값 사용
                startTime = startDateTime?.toLocalTime(), // ✅ startTime, optional
                endTime = endDateTime?.toLocalTime(),     // ✅ endTime, optional
                isConfirmed = response.isConfirmed,
                alarmOn = response.alarmOn,
                isDeleted = response.isDeleted ?: false,  // ✅ isDeleted, 기본값 false 처리
                copiedFromScheduleId = response.copiedFromScheduleId, // ✅ 복사된 일정 ID
                createdAt = response.createdAt,  // ✅ createdAt 추가
                updatedAt = response.updatedAt   // ✅ updatedAt 추가
            )
        }
    }
}

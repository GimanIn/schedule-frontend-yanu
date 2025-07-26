package com.example.mycalendar.mapper

import android.graphics.Color
import com.example.mycalendar.model.Schedule
import com.example.mycalendar.model.ScheduleResponse
import java.time.LocalDate
import java.time.LocalTime

object ScheduleMapper {

    // ✅ 여러 개 변환: List<ScheduleResponse> → List<Schedule>
    fun toSchedule(responseList: List<ScheduleResponse>): List<Schedule> {
        return responseList.map { toSchedule(it) } // 아래 단건 함수 재사용
    }

    // ✅ 단건 변환: ScheduleResponse → Schedule
    fun toSchedule(response: ScheduleResponse): Schedule {
        val startDate = try {
            LocalDate.parse(response.startDate)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val endDate = try {
            LocalDate.parse(response.endDate)
        } catch (e: Exception) {
            startDate
        }

        val startTime = response.startTime?.let {
            try {
                LocalTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        val endTime = response.endTime?.let {
            try {
                LocalTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        val scheduledDate = try {
            LocalDate.parse(response.scheduleDate)
        } catch (e: Exception) {
            startDate
        }

        return Schedule(
            id = response.id,
            title = response.title,
            memo = response.memo,
            location = response.location,
            category = response.category,
            color = Color.parseColor(response.color),
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            isConfirmed = response.isConfirmed,
            alarmOn = response.alarmOn,
            isDeleted = response.isDeleted ?: false,
            copiedFromScheduleId = response.copiedFromScheduleId,
            createdAt = response.createdAt,
            updatedAt = response.updatedAt,
            scheduledDate = scheduledDate
        )
    }
}

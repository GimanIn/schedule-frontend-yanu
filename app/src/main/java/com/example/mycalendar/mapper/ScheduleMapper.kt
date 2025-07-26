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
            memo = response.memo ?: "",
            location = response.location,
            category = response.category,
            color = Color.parseColor(response.color),
            startDate = response.startDate?.let { LocalDate.parse(it) } ?: LocalDate.now(),
            endDate = response.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now(),
            startTime = response.startTime?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 0),
            endTime = response.endTime?.let { LocalTime.parse(it) } ?: LocalTime.of(10, 0),
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

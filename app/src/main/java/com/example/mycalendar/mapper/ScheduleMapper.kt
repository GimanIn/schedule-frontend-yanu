package com.example.mycalendar.mapper

import android.graphics.Color
import com.example.mycalendar.model.Schedule
import com.example.mycalendar.model.ScheduleResponse
import java.time.LocalDate
import java.time.LocalTime

object ScheduleMapper {
    fun toSchedule(responseList: List<ScheduleResponse>): List<Schedule> {
        return responseList.map { response ->

            // ✅ 날짜/시간 파싱
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

            // ✅ 최종 매핑
            Schedule(
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
}

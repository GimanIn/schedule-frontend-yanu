package com.example.mycalendar.service

import android.content.Context
import android.util.Log
import com.example.mycalendar.AlarmManagerUtil
import com.example.mycalendar.model.Alarm
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import com.example.mycalendar.model.TodayAlarmResponse
import com.example.mycalendar.model.AlarmResponseDto
import retrofit2.Response

object AlarmSync {

    fun syncAlarms(context: Context) {
        // 🔧 수정 1: getAlarms() → getAllAlarms()
        // 🔧 수정 2: Call<List<Alarm>> → Call<List<AlarmResponseDto>>
        RetrofitClient.apiService.getAllAlarms().enqueue(object : Callback<List<AlarmResponseDto>> {
            override fun onResponse(call: Call<List<AlarmResponseDto>>, response: Response<List<AlarmResponseDto>>) {
                if (response.isSuccessful) {
                    val alarmDtos = response.body()
                    Log.d("AlarmSync", "받은 알람 개수: ${alarmDtos?.size}")
                    if (!alarmDtos.isNullOrEmpty()) {
                        // 🔧 수정 3: AlarmResponseDto를 Alarm으로 변환
                        val alarms = alarmDtos.map { alarmDto ->
                            Alarm(
                                id = alarmDto.id ?: 0L,
                                scheduleId = alarmDto.scheduleId ?: 0L,
                                scheduleTitle = alarmDto.scheduleTitle ?: "",
                                alarmTime = alarmDto.alarmTime ?: "",
                                message = alarmDto.message ?: "",
                                isSent = alarmDto.isSent ?: false,
                                sentAt = alarmDto.sentAt,
                                createdAt = alarmDto.createdAt ?: ""
                            )
                        }
                        AlarmManagerUtil.scheduleMultipleAlarms(context, alarms)
                    }
                } else {
                    Log.e("AlarmSync", "알람 응답 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<AlarmResponseDto>>, t: Throwable) {
                Log.e("AlarmSync", "알람 불러오기 실패: ${t.message}")
            }
        })
    }

    fun fetchTodayAlarmsAndNotify(context: Context) {
        RetrofitClient.apiService.getTodayAlarms().enqueue(object : Callback<TodayAlarmResponse> {
            override fun onResponse(
                call: Call<TodayAlarmResponse>,
                response: Response<TodayAlarmResponse>
            ) {
                if (response.isSuccessful) {
                    val todayAlarms = response.body()?.data?.alarms
                    Log.d("AlarmSync", "오늘 알림 개수: ${todayAlarms?.size}")

                    todayAlarms?.forEach { alarmDto ->
                        if (!(alarmDto.isSent ?: true)) {  // 🔧 null 안전 처리
                            // 🔧 AlarmResponseDto를 Alarm으로 변환
                            val alarm = Alarm(
                                id = alarmDto.id ?: 0L,
                                scheduleId = alarmDto.scheduleId ?: 0L,
                                scheduleTitle = alarmDto.scheduleTitle ?: "",
                                alarmTime = alarmDto.alarmTime ?: "",
                                message = alarmDto.message ?: "",
                                isSent = alarmDto.isSent ?: false,
                                sentAt = alarmDto.sentAt,
                                createdAt = alarmDto.createdAt ?: ""
                            )
                            //NotificationHelper.showNotification(context, alarm.id)
                             RetrofitClient.apiService.markAlarmAsSent(alarm.id)
                        }
                    }
                } else {
                    Log.w("AlarmSync", "오늘 알림 응답 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<TodayAlarmResponse>, t: Throwable) {
                Log.e("AlarmSync", "오늘 알림 불러오기 실패: ${t.message}")
            }
        })
    }
}
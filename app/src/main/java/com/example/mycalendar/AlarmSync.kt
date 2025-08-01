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
        RetrofitClient.apiService.getAlarms().enqueue(object : Callback<List<Alarm>> {
            override fun onResponse(call: Call<List<Alarm>>, response: Response<List<Alarm>>) {
                if (response.isSuccessful) {
                    val alarms = response.body()
                    Log.d("AlarmSync", "받은 알람 개수: ${alarms?.size}")
                    if (!alarms.isNullOrEmpty()) {
                        AlarmManagerUtil.scheduleMultipleAlarms(context, alarms)
                    }
                } else {
                    Log.e("AlarmSync", "알람 응답 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Alarm>>, t: Throwable) {
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
                    val todayAlarms = response.body()?.data?.alarms  // ✅ 수정된 라인
                    Log.d("AlarmSync", "오늘 알림 개수: ${todayAlarms?.size}")

                    todayAlarms?.forEach { alarm ->
                        if (!alarm.isSent) {
                            NotificationHelper.showNotification(context, alarm)
                            // RetrofitClient.apiService.markAlarmAsSent(alarm.id)
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
package com.example.mycalendar.service

import android.content.Context
import android.util.Log
import com.example.mycalendar.AlarmManagerUtil
import com.example.mycalendar.model.Alarm
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
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
}

package com.example.mycalendar.sync

import android.content.Context
import android.util.Log
import com.example.mycalendar.model.Alarm
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object AlarmSync {
    private const val TAG = "AlarmSync"

    fun syncAlarms(context: Context, onResult: (List<Alarm>?) -> Unit) {
        val call = RetrofitClient.apiService.getAlarms()
        call.enqueue(object : Callback<List<Alarm>> {
            override fun onResponse(
                call: Call<List<Alarm>>,
                response: Response<List<Alarm>>
            ) {
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ 알람 동기화 성공")
                    onResult(response.body())
                } else {
                    Log.e(TAG, "❌ 알람 동기화 실패: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Alarm>>, t: Throwable) {
                Log.e(TAG, "❌ 알람 동기화 에러: ${t.message}")
                onResult(null)
            }
        })
    }
}

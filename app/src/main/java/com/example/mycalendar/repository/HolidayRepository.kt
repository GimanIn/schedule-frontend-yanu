package com.example.mycalendar.repository

import android.util.Log
import com.example.mycalendar.model.HolidayItem
import com.example.mycalendar.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HolidayRepository {

    companion object {
        private const val SERVICE_KEY = "YOUR_API_KEY_HERE" // 🔑 실제 API 키로 교체
    }

    suspend fun getHolidays(year: String, month: String): List<HolidayItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("HolidayAPI", "🌐 API 호출: ${year}년 ${month}월")

                val response = RetrofitClient.holidayApiService.getHolidays(
                    serviceKey = SERVICE_KEY,
                    year = year,
                    month = month.padStart(2, '0')
                ).execute()

                if (response.isSuccessful) {
                    val body = response.body()
                    val holidays: List<HolidayItem> = body?.response?.body?.items?.holidayItems ?: emptyList()

                    Log.d("HolidayAPI", "✅ 성공! 공휴일 ${holidays.size}개")
                    for (holiday in holidays) {
                        Log.d("HolidayAPI", "📅 ${holiday.dateName}: ${holiday.locdate}")
                    }

                    holidays
                } else {
                    Log.e("HolidayAPI", "❌ API 응답 실패: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("HolidayAPI", "❌ 공휴일 API 호출 실패", e)
                emptyList()
            }
        }
    }
}
// ✅ MyCalendarApplication.kt 추가
package com.example.mycalendar

import android.app.Application
import com.example.mycalendar.network.RetrofitClient

class MyCalendarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(applicationContext) // 토큰 초기화용
    }
}

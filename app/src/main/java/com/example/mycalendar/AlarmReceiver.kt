// ✅ AlarmReceiver.kt

package com.example.mycalendar

import android.content.*
import android.util.Log
import android.widget.Toast
//예약된 시각에 알림을 띄우는 역할
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarmId", -1)
        val message = intent.getStringExtra("message")
        Log.d("AlarmReceiver", "📢 알림 수신: $message")

        // 알림 띄우기
        AlarmManagerUtil.showNotification(context, alarmId, message)
    }
}

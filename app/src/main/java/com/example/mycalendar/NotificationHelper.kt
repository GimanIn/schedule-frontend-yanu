package com.example.mycalendar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mycalendar.R
import com.example.mycalendar.model.AlarmResponseDto

object NotificationHelper {
    private const val CHANNEL_ID = "alarm_channel"
    private const val CHANNEL_NAME = "일정 알림"

    fun showNotification(context: Context, alarm: AlarmResponseDto) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 알맞은 아이콘 넣기
            .setContentTitle("🔔 일정 알림")
            .setContentText(alarm.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarm.id.toInt(), notification)
    }
}

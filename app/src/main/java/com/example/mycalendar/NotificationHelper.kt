// NotificationHelper.kt - 기존 코드에 메서드 추가

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

    // 🔔 기존 메서드 (그대로 유지)
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 일정 알림")
            .setContentText(alarm.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarm.id.toInt(), notification)
    }

    // ✨ 새로 추가: 버튼 클릭용 간단한 알림 메서드
    fun showSimpleNotification(context: Context, title: String, message: String, notificationId: Int = 1001) {
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250)) // 진동 추가
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // 🎯 AI 요약 전용 알림
    fun showAiSummaryNotification(context: Context, summaryText: String) {
        showSimpleNotification(
            context = context,
            title = "📋 오늘의 AI 요약 완료",
            message = summaryText,
            notificationId = 2001
        )
    }

    // 📅 일정 알림 (장소 포함)
    fun showScheduleNotification(context: Context, title: String, time: String, location: String? = null) {
        val message = if (location.isNullOrEmpty()) {
            "$time 에 일정이 있습니다."
        } else {
            "$time 에 일정이 있습니다.\n📍 $location"
        }

        showSimpleNotification(
            context = context,
            title = "📅 $title",
            message = message,
            notificationId = 3001
        )
    }

    // ⚠️ 중요 알림
    fun showImportantNotification(context: Context, title: String, message: String) {
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // 긴 텍스트 표시
            .setPriority(NotificationCompat.PRIORITY_MAX) // 최고 우선순위
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 300, 500)) // 강한 진동
            .build()

        notificationManager.notify(4001, notification)
    }
}
// ✅ AlarmManagerUtil.kt

package com.example.mycalendar

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mycalendar.model.AlarmResponse
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter



//*"그 시각에 맞춰 알림을 예약하는 유틸리티"
object AlarmManagerUtil {

    fun scheduleAlarm(context: Context, alarm: AlarmResponse) {
        val alarmTime = alarm.alarmTime ?: return

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val localDateTime = LocalDateTime.parse(alarm.alarmTime, formatter)

        val triggerMillis = localDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alarm.id)
            putExtra("message", alarm.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    // 알림 띄우기
    fun showNotification(context: Context, alarmId: Long, message: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 채널 설정 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel",
                "일정 알림 채널",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("일정 알림")
            .setContentText(message ?: "일정이 있습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmId.toInt(), notification)
    }
}

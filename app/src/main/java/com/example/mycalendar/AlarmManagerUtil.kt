// ✅ AlarmManagerUtil.kt

package com.example.mycalendar

import android.app.*
import android.content.*
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mycalendar.model.Alarm
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 그 시각에 맞춰 알림을 예약하는 유틸리티
 * 개선된 버전 - 권한 체크, 예외 처리, 알람 취소 기능 포함
 */
object AlarmManagerUtil {

    private const val TAG = "AlarmManagerUtil"
    private const val CHANNEL_ID = "alarm_channel"
    private const val CHANNEL_NAME = "일정 알림 채널"

    /**
     * 알람 예약
     */
    fun scheduleAlarm(context: Context, alarm: Alarm): Boolean {
        // 1. 필수 데이터 검증
        val alarmId = alarm.id ?: run {
            Log.e(TAG, "alarm.id가 null입니다. 알람 예약 생략")
            return false
        }

        val alarmTime = alarm.alarmTime ?: run {
            Log.e(TAG, "alarm.alarmTime이 null입니다. 예약 생략")
            return false
        }

        // 2. 권한 체크 (Android 12+)
        if (!checkAlarmPermission(context)) {
            return false
        }

        // 3. 시간 파싱 및 검증
        val triggerMillis = try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val localDateTime = LocalDateTime.parse(alarmTime, formatter)
            val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())
            val millis = zonedDateTime.toInstant().toEpochMilli()

            // 과거 시간 체크
            if (millis <= System.currentTimeMillis()) {
                Log.w(TAG, "과거 시간으로 알람을 설정할 수 없습니다: $alarmTime")
                return false
            }

            millis
        } catch (e: Exception) {
            Log.e(TAG, "시간 파싱 오류: ${e.message}")
            return false
        }

        // 4. PendingIntent 생성
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("message", alarm.message ?: "일정이 있습니다.")
        }

        val pendingIntent = try {
            PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Log.e(TAG, "PendingIntent 생성 오류: ${e.message}")
            return false
        }

        // 5. 알람 예약
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
            Log.d(TAG, "알람 예약 성공: ID=$alarmId, 시간=$alarmTime")
            true
        } catch (e: Exception) {
            Log.e(TAG, "알람 예약 실패: ${e.message}")
            false
        }
    }

    /**
     * 알람 취소
     */
    fun cancelAlarm(context: Context, alarmId: Long): Boolean {
        return try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "알람 취소 성공: ID=$alarmId")
                true
            } else {
                Log.w(TAG, "취소할 알람을 찾을 수 없습니다: ID=$alarmId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "알람 취소 실패: ${e.message}")
            false
        }
    }

    /**
     * 여러 알람 일괄 예약
     */
    fun scheduleMultipleAlarms(context: Context, alarms: List<Alarm>): Int {
        var successCount = 0
        alarms.forEach { alarm ->
            if (scheduleAlarm(context, alarm)) {
                successCount++
            }
        }
        Log.d(TAG, "일괄 알람 예약 완료: ${successCount}/${alarms.size}")
        return successCount
    }

    /**
     * 알림 표시
     */
    fun showNotification(context: Context, alarmId: Long, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 채널 생성 (Android 8.0 이상)
            createNotificationChannel(notificationManager)

            // 메인 액티비티로 이동하는 인텐트
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("fromNotification", true)
                putExtra("alarmId", alarmId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("📅 일정 알림")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            notificationManager.notify(alarmId.toInt(), notification)
            Log.d(TAG, "알림 표시 성공: ID=$alarmId")

        } catch (e: Exception) {
            Log.e(TAG, "알림 표시 실패: ${e.message}")
        }
    }

    /**
     * 알람 권한 체크 (Android 12+)
     */
    private fun checkAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "정확한 알람 설정 권한이 없습니다")
                // 권한 요청을 위한 인텐트 (필요시 활성화)
                // val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                // context.startActivity(intent)
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "캘린더 일정 알림을 위한 채널입니다"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "알림 채널 생성 완료")
            }
        }
    }

    /**
     * 예약된 알람이 있는지 확인
     */
    fun isAlarmScheduled(context: Context, alarmId: Long): Boolean {
        return try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent != null
        } catch (e: Exception) {
            Log.e(TAG, "알람 존재 확인 실패: ${e.message}")
            false
        }
    }
}
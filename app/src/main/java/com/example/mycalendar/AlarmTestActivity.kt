package com.example.mycalendar

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mycalendar.model.Alarm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 🧪 완전 에러 없는 알람 테스트 액티비티
 */
class AlarmTestActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private var testAlarmCounter = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ XML 레이아웃 없이 코드로 UI 생성 (에러 방지)
        createLayoutProgrammatically()

        title = "🧪 알람 테스트"
        checkInitialStatus()
    }

    /**
     * ✅ 코드로 레이아웃 생성 (XML 의존성 제거)
     */
    private fun createLayoutProgrammatically() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        // 상태 텍스트
        statusText = TextView(this).apply {
            text = "🧪 알람 테스트 준비 중..."
            textSize = 14f
            setPadding(20, 20, 20, 20)
        }
        layout.addView(statusText)

        // 10초 테스트 버튼
        val test10sButton = Button(this).apply {
            text = "⚡ 10초 후 알람"
            setBackgroundColor(android.graphics.Color.parseColor("#FF5722"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setOnClickListener { testAlarmAfter(0, 10, "⚡ 10초 테스트") }
        }
        layout.addView(test10sButton)

        // 30초 테스트 버튼
        val test30sButton = Button(this).apply {
            text = "🚀 30초 후 알람"
            setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setOnClickListener { testAlarmAfter(0, 30, "🚀 30초 테스트") }
        }
        layout.addView(test30sButton)

        // 1분 테스트 버튼
        val test1mButton = Button(this).apply {
            text = "⏰ 1분 후 알람"
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setOnClickListener { testAlarmAfter(1, 0, "⏰ 1분 테스트") }
        }
        layout.addView(test1mButton)

        // 상태 체크 버튼
        val checkStatusButton = Button(this).apply {
            text = "📊 상태 체크"
            setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setOnClickListener { checkFullAlarmStatus() }
        }
        layout.addView(checkStatusButton)

        // 권한 설정 버튼
        val settingsButton = Button(this).apply {
            text = "⚙️ 권한 설정"
            setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setOnClickListener { openAlarmSettings() }
        }
        layout.addView(settingsButton)

        // 뒤로가기 버튼
        val backButton = Button(this).apply {
            text = "← 뒤로가기"
            setBackgroundColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
            setOnClickListener { finish() }
        }
        layout.addView(backButton)

        setContentView(layout)
    }

    /**
     * ✅ 완전 안전한 AlarmResponse 생성
     */
    private fun createSafeAlarmResponse(id: Long, alarmTime: String, message: String): Alarm {
        val currentTimeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        return Alarm(
            id = id,
            scheduleId = id + 1,
            scheduleTitle = "🧪 테스트",
            alarmTime = alarmTime,
            message = message,
            isSent = false,
            sentAt = null,
            createdAt = currentTimeStr
        )
    }

    /**
     * 지정된 시간 후 테스트 알람 설정
     */
    private fun testAlarmAfter(minutes: Int, seconds: Int, prefix: String) {
        Log.d("AlarmTest", "=== 알람 테스트 시작 ===")

        // 1. 권한 체크
        if (!checkAllPermissions()) {
            statusText.text = "❌ 권한이 부족합니다. 권한 설정을 눌러주세요."
            Toast.makeText(this, "❌ 권한이 부족합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. 테스트 알람 생성
        try {
            val testTime = LocalDateTime.now()
                .plusMinutes(minutes.toLong())
                .plusSeconds(seconds.toLong())

            val testAlarm = createSafeAlarmResponse(
                id = System.currentTimeMillis() + testAlarmCounter++,
                alarmTime = testTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                message = "$prefix - 성공! 🎉 ${testTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"
            )

            // 3. 알람 예약
            val success = AlarmManagerUtil.scheduleAlarm(this, testAlarm)
            val timeStr = testTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            if (success) {
                statusText.text = """
                    ✅ 알람 설정 성공!
                    ⏰ 예정 시간: $timeStr
                    📱 ${minutes}분 ${seconds}초 후 알림이 옵니다
                    🔔 기다려주세요...
                """.trimIndent()

                Toast.makeText(this, "⏰ ${minutes}분 ${seconds}초 후 알람 설정됨!", Toast.LENGTH_LONG).show()
                Log.d("AlarmTest", "✅ 테스트 알람 설정 성공: $timeStr")
            } else {
                statusText.text = "❌ 알람 설정 실패"
                Toast.makeText(this, "❌ 알람 설정 실패!", Toast.LENGTH_SHORT).show()
                Log.e("AlarmTest", "❌ 테스트 알람 설정 실패")
            }
        } catch (e: Exception) {
            Log.e("AlarmTest", "테스트 알람 생성 중 에러", e)
            statusText.text = "❌ 에러 발생: ${e.message}"
            Toast.makeText(this, "❌ 에러 발생", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 전체 알람 상태 체크
     */
    private fun checkFullAlarmStatus() {
        Log.d("AlarmStatus", "=== 🧪 상세 알람 상태 체크 ===")

        val status = StringBuilder()

        // 1. 안드로이드 버전
        status.append("📱 안드로이드: API ${Build.VERSION.SDK_INT}\n")

        // 2. 알림 권한
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        status.append("🔔 알림 권한: ${if (hasNotificationPermission) "✅ 허용됨" else "❌ 거부됨"}\n")

        // 3. 정확한 알람 권한
        val hasAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true

        status.append("⏰ 정확한 알람: ${if (hasAlarmPermission) "✅ 허용됨" else "❌ 거부됨"}\n")

        // 4. 현재 시간
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        status.append("🕐 현재 시간: $now\n")

        // 5. 전체 상태
        val allGood = hasNotificationPermission && hasAlarmPermission
        status.append("\n${if (allGood) "🎉 테스트 준비 완료!" else "⚠️ 권한 설정 필요"}")

        statusText.text = status.toString()

        // 로그에도 출력
        Log.d("AlarmStatus", status.toString())

        Toast.makeText(this, if (allGood) "✅ 모든 권한 정상" else "❌ 권한 설정 필요", Toast.LENGTH_SHORT).show()
    }

    /**
     * 모든 권한 체크
     */
    private fun checkAllPermissions(): Boolean {
        // 알림 권한 체크
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        // 정확한 알람 권한 체크
        val hasAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true

        return hasNotificationPermission && hasAlarmPermission
    }

    /**
     * 알람 설정 화면으로 이동
     */
    private fun openAlarmSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            } else {
                startActivity(android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                })
            }
        } catch (e: Exception) {
            Log.e("AlarmTest", "설정 화면 열기 실패", e)
            Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 초기 상태 체크
     */
    private fun checkInitialStatus() {
        statusText.text = "🧪 알람 테스트 준비 중..."

        // 0.5초 후 상태 체크
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            checkFullAlarmStatus()
        }, 500)
    }
}
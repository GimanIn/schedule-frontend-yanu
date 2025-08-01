// ✅ AlarmReceiver.kt

package com.example.mycalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 알람이 울릴 때 호출되는 BroadcastReceiver
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "알람 수신: ${intent.action}")

            // 시스템 재부팅 처리
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                handleBootCompleted(context)
                return
            }

            // 일반 알람 처리
            val alarmId = intent.getLongExtra("alarmId", -1)
            val message = intent.getStringExtra("message") ?: "일정이 있습니다."

            if (alarmId == -1L) {
                Log.e(TAG, "유효하지 않은 alarmId입니다")
                return
            }

            Log.d(TAG, "알람 처리 시작: ID=$alarmId, 메시지=$message")

            // 알림 표시
            AlarmManagerUtil.showNotification(context, alarmId, message)

            // 추가 작업 (선택사항)
            // - 데이터베이스에 알람 완료 상태 업데이트
            // - 분석용 로그 전송
            // - 사용자 설정에 따른 추가 액션

        } catch (e: Exception) {
            Log.e(TAG, "알람 처리 중 오류 발생: ${e.message}")
        }
    }

    /**
     * 시스템 재부팅 후 알람 복구
     */
    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "시스템 재부팅 감지 - 알람 복구 시작")

        // TODO: 저장된 알람 목록을 데이터베이스나 SharedPreferences에서 불러와서
        // 다시 예약하는 로직을 구현하세요

        // 예시:
        // val savedAlarms = AlarmRepository.getAllActiveAlarms()
        // savedAlarms.forEach { alarm ->
        //     AlarmManagerUtil.scheduleAlarm(context, alarm)
        // }
    }
}
package com.example.mycalendar

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.graphics.Color
import androidx.appcompat.app.AlertDialog

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var selectedDate: LocalDate
    private var startDateTime: LocalDateTime? = null
    private var endDateTime: LocalDateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        selectedDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("selectedDate", LocalDate::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("selectedDate") as? LocalDate
        } ?: LocalDate.now()

        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val timeSwitch = findViewById<SwitchMaterial>(R.id.timeSwitch)
        val startTimeText = findViewById<TextView>(R.id.startTimeText)
        val endTimeText = findViewById<TextView>(R.id.endTimeText)
        val alarmSwitch = findViewById<SwitchMaterial>(R.id.alarmSwitch)
        val memoEditText = findViewById<EditText>(R.id.memoEditText)
        val saveButton = findViewById<Button>(R.id.save_button)
        val backButton = findViewById<ImageButton>(R.id.back_button)
        val categoryEditText = findViewById<EditText>(R.id.categoryEditText)
        val locationEditText = findViewById<EditText>(R.id.locationEditText)
        val timeDisplayGroup = findViewById<Group>(R.id.time_display_group)

        backButton.setOnClickListener {
            finish()
        }

        // --- 수정됨: 시간 설정 토글 로직 ---
        timeSwitch.setOnCheckedChangeListener { _, isChecked ->
            timeDisplayGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                startDateTime = null
                endDateTime = null
            }
        }

        // --- 수정됨: 시작 시간 설정 (날짜 -> 시간 순서로 선택) ---
        startTimeText.setOnClickListener {
            pickDateTime(selectedDate) { dateTime ->
                startDateTime = dateTime
                val formatter = DateTimeFormatter.ofPattern("M월 d일 a hh:mm", Locale.KOREA)
                startTimeText.text = startDateTime?.format(formatter)
            }
        }

        // --- 수정됨: 종료 시간 설정 (날짜 -> 시간 순서로 선택) ---
        endTimeText.setOnClickListener {
            val initialDate = endDateTime?.toLocalDate() ?: startDateTime?.toLocalDate() ?: selectedDate
            pickDateTime(initialDate) { dateTime ->
                endDateTime = dateTime
                val formatter = DateTimeFormatter.ofPattern("M월 d일 a hh:mm", Locale.KOREA)
                endTimeText.text = endDateTime?.format(formatter)
            }
        }

        var selectedColor = -769226 // 기본 회색
        val colorDot = findViewById<View>(R.id.color_dot)
        colorDot.background.setTint(selectedColor)

        colorDot.setOnClickListener {
            val colors = listOf(
                Color.parseColor("#EF9A9A"), // 빨강
                Color.parseColor("#90CAF9"), // 파랑
                Color.parseColor("#A5D6A7"), // 초록
                Color.parseColor("#FFE082"), // 노랑
                Color.parseColor("#B39DDB")  // 보라
            )
            val colorNames = arrayOf("빨강", "파랑", "초록", "노랑", "보라")

            AlertDialog.Builder(this)
                .setTitle("색상 선택")
                .setItems(colorNames) { _, which ->
                    selectedColor = colors[which]

                    // --- 여기가 수정된 부분입니다 ---
                    // 1. 배경 Drawable을 복제합니다.
                    val background = colorDot.background.mutate()
                    // 2. 복제된 Drawable의 색상만 변경합니다.
                    background.setTint(selectedColor)
                    // 3. 변경된 배경을 다시 적용합니다.
                    colorDot.background = background
                }
                .show()
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 시작 시간이 종료 시간보다 늦으면 오류 처리
            if (startDateTime != null && endDateTime != null && startDateTime!!.isAfter(endDateTime)) {
                Toast.makeText(this, "종료 시간이 시작 시간보다 빠를 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val color = selectedColor

            val memo = "카테고리: ${categoryEditText.text}\n" +
                    "장소: ${locationEditText.text}\n" +
                    "메모: ${memoEditText.text}"

            val newSchedule = Schedule(
                title = title,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                color = color,
                isAlarmOn = alarmSwitch.isChecked,
                memo = memo.trim(),
                isConfirmed = true,
                isPostponed = false
            )

            val resultIntent = Intent()
            resultIntent.putExtra("newSchedule", newSchedule)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    // 날짜와 시간을 순서대로 선택하게 하는 함수
    private fun pickDateTime(initialDate: LocalDate, onDateTimePicked: (LocalDateTime) -> Unit) {
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
            TimePickerDialog(this, { _, hour, minute ->
                val pickedTime = LocalTime.of(hour, minute)
                onDateTimePicked(LocalDateTime.of(pickedDate, pickedTime))
            }, 9, 0, false).show()
        }, initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth).show()
    }
}
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
import android.widget.LinearLayout
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneId

class AddScheduleActivity : AppCompatActivity() {

    // 날짜와 시간을 분리해서 관리
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private var startTime: LocalTime? = null
    private var endTime: LocalTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        // 초기 날짜 설정
        val initialDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("selectedDate", LocalDate::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("selectedDate") as? LocalDate
        } ?: LocalDate.now()
        startDate = initialDate
        endDate = initialDate

        // UI 요소들 찾기 (새로운 레이아웃 기준)
        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val backButton = findViewById<ImageButton>(R.id.back_button)
        val colorDot = findViewById<View>(R.id.color_dot)
        val dateRangeLayout = findViewById<LinearLayout>(R.id.dateRangeLayout)
        val startDateText = findViewById<TextView>(R.id.startDateText)
        val endDateText = findViewById<TextView>(R.id.endDateText)
        val dateArrowText = findViewById<TextView>(R.id.dateArrowText)
        val timeSwitch = findViewById<SwitchMaterial>(R.id.timeSwitch)
        val timePickerLayout = findViewById<LinearLayout>(R.id.timePickerLayout)
        val startTimeText = findViewById<TextView>(R.id.startTimeText)
        val endTimeText = findViewById<TextView>(R.id.endTimeText)
        val alarmSwitch = findViewById<SwitchMaterial>(R.id.alarmSwitch)
        val memoEditText = findViewById<EditText>(R.id.memoEditText)
        val categoryEditText = findViewById<EditText>(R.id.categoryEditText)
        val locationEditText = findViewById<EditText>(R.id.locationEditText)
        val saveButton = findViewById<Button>(R.id.save_button)

        // 초기 UI 설정
        updateDateTextViews() // 날짜 텍스트 업데이트
        timePickerLayout.visibility = View.GONE // 시간 선택기는 숨김

        backButton.setOnClickListener {
            finish()
        }

        // 날짜 설정 레이아웃 클릭 리스너
        dateRangeLayout.setOnClickListener {
            // 1. 시작 날짜를 선택하기 위한 DatePickerDialog를 띄웁니다.
            val startDatePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    startDate = LocalDate.of(year, month + 1, dayOfMonth)

                    // 2. 시작 날짜 선택이 끝나면, 바로 종료 날짜 선택을 위한 DatePickerDialog를 띄웁니다.
                    val endDatePickerDialog = DatePickerDialog(
                        this,
                        { _, endYear, endMonth, endDayOfMonth ->
                            endDate = LocalDate.of(endYear, endMonth + 1, endDayOfMonth)

                            // 시작 날짜가 종료 날짜보다 늦으면, 종료 날짜를 시작 날짜로 맞춰줍니다.
                            if (startDate!! > endDate!!) {
                                endDate = startDate
                            }

                            // 3. 모든 선택이 끝나면 화면의 날짜 텍스트를 업데이트합니다.
                            updateDateTextViews()
                        },
                        endDate!!.year,
                        endDate!!.monthValue - 1,
                        endDate!!.dayOfMonth
                    )
                    endDatePickerDialog.setMessage("종료 날짜 선택")
                    endDatePickerDialog.show()
                },
                startDate!!.year,
                startDate!!.monthValue - 1,
                startDate!!.dayOfMonth
            )
            startDatePickerDialog.setMessage("시작 날짜 선택")
            startDatePickerDialog.show()
        }

        // 시간 설정 토글 리스너
        timeSwitch.setOnCheckedChangeListener { _, isChecked ->
            timePickerLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                // --- 👇 이 부분이 추가되었습니다! ---
                // 토글을 켜면 기본 시간을 설정하고 화면에 표시합니다.
                if (startTime == null) startTime = LocalTime.of(9, 0)
                if (endTime == null) endTime = LocalTime.of(10, 0)
                startTimeText.text = startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                endTimeText.text = endTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                startTime = null
                endTime = null
            }
        }

        // 시작 시간 클릭 리스너
        startTimeText.setOnClickListener {
            // --- 👇 TimePickerDialog 생성자 부분이 수정되었습니다! ---
            val timePickerDialog = TimePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar, // 스피너 스타일 테마 적용
                { _, hour, minute ->
                    startTime = LocalTime.of(hour, minute)
                    startTimeText.text = startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                },
                startTime?.hour ?: 9, startTime?.minute ?: 0, true
            )
            timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // 배경 투명 처리
            timePickerDialog.show()
        }

        // 종료 시간 클릭 리스너
        endTimeText.setOnClickListener {
            // --- 👇 TimePickerDialog 생성자 부분이 수정되었습니다! ---
            val timePickerDialog = TimePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar, // 스피너 스타일 테마 적용
                { _, hour, minute ->
                    endTime = LocalTime.of(hour, minute)
                    endTimeText.text = endTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                },
                endTime?.hour ?: 10, endTime?.minute ?: 0, true
            )
            timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // 배경 투명 처리
            timePickerDialog.show()
        }

        var selectedColor = Color.GRAY
        colorDot.setOnClickListener {
            val colors = listOf(
                Color.parseColor("#EF9A9A"), Color.parseColor("#90CAF9"), Color.parseColor("#A5D6A7"),
                Color.parseColor("#FFE082"), Color.parseColor("#B39DDB")
            )
            val colorNames = arrayOf("빨강", "파랑", "초록", "노랑", "보라")
            AlertDialog.Builder(this)
                .setTitle("색상 선택")
                .setItems(colorNames) { _, which ->
                    selectedColor = colors[which]
                    colorDot.background.mutate().setTint(selectedColor)
                }
                .show()
        }

        // 저장 버튼 로직
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var finalStartDateTime: LocalDateTime? = null
            var finalEndDateTime: LocalDateTime? = null

            if (timeSwitch.isChecked) {
                // 시간 설정 ON: 날짜와 시간을 조합, 시간이 설정 안됐으면 기본값 사용
                finalStartDateTime = LocalDateTime.of(startDate, startTime ?: LocalTime.of(9,0))
                finalEndDateTime = LocalDateTime.of(endDate, endTime ?: LocalTime.of(10, 0))
            } else {
                // 시간 설정 OFF: 하루 종일 일정으로 처리
                finalStartDateTime = startDate?.atStartOfDay()
                finalEndDateTime = endDate?.atTime(23, 59, 59)
            }

            // --- 👇 여기에 시간 순서 유효성 검사 코드를 추가합니다 ---
            if (finalStartDateTime != null && finalEndDateTime != null && finalStartDateTime.isAfter(finalEndDateTime)) {
                Toast.makeText(this, "종료 시간이 시작 시간보다 빠를 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // 저장 과정을 중단
            }

            val memo = "카테고리: ${categoryEditText.text}\n" +
                    "장소: ${locationEditText.text}\n" +
                    "메모: ${memoEditText.text}"

            val newSchedule = Schedule(
                title = title,
                startDateTime = finalStartDateTime,
                endDateTime = finalEndDateTime,
                color = selectedColor,
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

    // 이 함수를 새로 추가하세요.
    private fun updateDateTextViews() {
        val formatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)
        val startDateText = findViewById<TextView>(R.id.startDateText)
        val endDateText = findViewById<TextView>(R.id.endDateText)
        val dateArrowText = findViewById<TextView>(R.id.dateArrowText)

        startDateText.text = startDate?.format(formatter)
        if (startDate == endDate || endDate == null) {
            dateArrowText.visibility = View.GONE
            endDateText.visibility = View.GONE
        } else {
            endDateText.text = endDate?.format(formatter)
            dateArrowText.visibility = View.VISIBLE
            endDateText.visibility = View.VISIBLE
        }
    }
}
package com.example.mycalendar

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.widget.PopupWindow

class AddScheduleActivity : AppCompatActivity() {

    // UI 요소를 멤버 변수로 선언
    private lateinit var titleEditText: EditText
    private lateinit var timeSwitch: SwitchMaterial
    private lateinit var alarmSwitch: SwitchMaterial
    private lateinit var memoEditText: EditText
    private lateinit var categoryEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var timePickerLayout: LinearLayout
    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    private lateinit var colorDot: View

    // 데이터 변수
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private var startTime: LocalTime? = null
    private var endTime: LocalTime? = null
    private var selectedColor: Int = Color.parseColor("#4285F4")
    private var scheduleToEdit: Schedule? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        initViews()
        initData()
        setupListeners()
    }

    private fun initViews() {
        titleEditText = findViewById(R.id.titleEditText)
        colorDot = findViewById(R.id.color_dot)
        timeSwitch = findViewById(R.id.timeSwitch)
        alarmSwitch = findViewById(R.id.alarmSwitch)
        memoEditText = findViewById(R.id.memoEditText)
        categoryEditText = findViewById(R.id.categoryEditText)
        locationEditText = findViewById(R.id.locationEditText)
        timePickerLayout = findViewById(R.id.timePickerLayout)
        startTimeText = findViewById(R.id.startTimeText)
        endTimeText = findViewById(R.id.endTimeText)
    }

    private fun initData() {
        scheduleToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("scheduleToEdit", Schedule::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("scheduleToEdit") as? Schedule
        }

        // --- 👇 '복사 모드'를 처리하는 로직을 여기에 추가합니다 ---
        val isCopyMode = intent.getBooleanExtra("isCopyMode", false)

        val copyButton = findViewById<ImageButton>(R.id.copy_button)

        if (scheduleToEdit != null) { // 수정 또는 복사 모드
            val schedule = scheduleToEdit!!
            titleEditText.setText(schedule.title)
            startDate = schedule.startDateTime?.toLocalDate()
            endDate = schedule.endDateTime?.toLocalDate()
            startTime = schedule.startDateTime?.toLocalTime()
            endTime = schedule.endDateTime?.toLocalTime()
            selectedColor = schedule.color
            alarmSwitch.isChecked = schedule.isAlarmOn
            memoEditText.setText(schedule.memo)
            if (startTime != null) {
                timeSwitch.isChecked = true
            }

            // '복사 모드'라면, UI를 채운 뒤에 '새로 만들기' 상태로 전환
            if (isCopyMode) {
                this.scheduleToEdit = null // this를 붙여 멤버 변수임을 명확히 함
                Toast.makeText(this, "일정이 복사되었습니다. 저장하여 새 일정으로 생성하세요.", Toast.LENGTH_LONG).show()
            }
            copyButton.visibility = View.VISIBLE // 수정 모드일 때만 복사 버튼 보이기
        } else { // 생성 모드
            val initialDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("selectedDate", LocalDate::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("selectedDate") as? LocalDate
            } ?: LocalDate.now()
            startDate = initialDate
            endDate = initialDate
            copyButton.visibility = View.GONE // 생성 모드에서는 숨기기
        }

        updateDateTextViews()
        colorDot.background.mutate().setTint(selectedColor)
        timePickerLayout.visibility = if (timeSwitch.isChecked) View.VISIBLE else View.GONE
        if(timeSwitch.isChecked) {
            startTimeText.text = startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "09:00"
            endTimeText.text = endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "10:00"
        }
    }

    private fun setupListeners() {
        val backButton = findViewById<ImageButton>(R.id.back_button)
        val copyButton = findViewById<ImageButton>(R.id.copy_button)
        val dateRangeLayout = findViewById<LinearLayout>(R.id.dateRangeLayout)
        val saveButton = findViewById<Button>(R.id.save_button)

        backButton.setOnClickListener { finish() }

        // --- 👇 이 두 버튼의 리스너를 아래 코드로 교체해주세요 ---

        // '저장' 버튼: 현재 상태(수정이든 생성이든)를 저장하고 화면을 닫습니다.
        saveButton.setOnClickListener {
            handleSave(isCopy = false)
        }

        // '복사' 버튼: 현재 상태를 '새로운 일정 생성' 모드로 바꾸기만 합니다.
        copyButton.setOnClickListener {
            // "수정 모드"를 해제하여, 다음에 저장할 때 새 ID를 받도록 합니다.
            scheduleToEdit = null

            // 사용자에게 상태가 변경되었음을 알립니다.
            Toast.makeText(this, "일정이 복사되었습니다. 저장 버튼을 눌러 새 일정으로 생성하세요.", Toast.LENGTH_LONG).show()

            // 혼동을 막기 위해 복사 버튼을 비활성화합니다.
            copyButton.isEnabled = false
            copyButton.alpha = 0.5f
        }

        // --- 여기까지 교체 ---

        dateRangeLayout.setOnClickListener { openDateRangePicker() }
        timeSwitch.setOnCheckedChangeListener { _, isChecked -> handleTimeSwitch(isChecked) }
        startTimeText.setOnClickListener { openTimePicker(isStart = true) }
        endTimeText.setOnClickListener { openTimePicker(isStart = false) }
        colorDot.setOnClickListener { openColorPicker() }
    }

    private fun handleSave(isCopy: Boolean) {
        val title = titleEditText.text.toString()
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        var finalStartDateTime: LocalDateTime? = null
        var finalEndDateTime: LocalDateTime? = null

        if (timeSwitch.isChecked) {
            finalStartDateTime = LocalDateTime.of(startDate, startTime ?: LocalTime.of(9, 0))
            finalEndDateTime = LocalDateTime.of(endDate, endTime ?: LocalTime.of(10, 0))
        } else {
            finalStartDateTime = startDate?.atStartOfDay()
            finalEndDateTime = endDate?.atTime(23, 59, 59)
        }

        if (finalStartDateTime != null && finalEndDateTime != null && finalStartDateTime.isAfter(finalEndDateTime)) {
            Toast.makeText(this, "종료 시간이 시작 시간보다 빠를 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val memo = "카테고리: ${categoryEditText.text}\n장소: ${locationEditText.text}\n메모: ${memoEditText.text}"

        val scheduleId = if (scheduleToEdit != null && !isCopy) {
            scheduleToEdit!!.id
        } else {
            java.util.UUID.randomUUID().toString()
        }

        val resultSchedule = Schedule(
            id = scheduleId,
            title = title,
            startDateTime = finalStartDateTime,
            endDateTime = finalEndDateTime,
            color = selectedColor,
            isAlarmOn = alarmSwitch.isChecked,
            memo = memo.trim()
        )

        val resultIntent = Intent()
        val resultKey = if (scheduleToEdit != null && !isCopy) "updatedSchedule" else "newSchedule"
        resultIntent.putExtra(resultKey, resultSchedule)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun openDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker().setTitleText("기간 선택").build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            startDate = Instant.ofEpochMilli(selection.first).atZone(ZoneId.systemDefault()).toLocalDate()
            endDate = Instant.ofEpochMilli(selection.second).atZone(ZoneId.systemDefault()).toLocalDate()
            updateDateTextViews()
        }
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun handleTimeSwitch(isChecked: Boolean) {
        timePickerLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (isChecked) {
            if (startTime == null) startTime = LocalTime.of(9, 0)
            if (endTime == null) endTime = LocalTime.of(10, 0)
            startTimeText.text = startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            endTimeText.text = endTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            startTime = null
            endTime = null
        }
    }

    private fun openTimePicker(isStart: Boolean) {
        val currentTime = if (isStart) startTime else endTime
        val defaultHour = if (isStart) 9 else 10

        val timePickerDialog = TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, { _, hour, minute ->
            val pickedTime = LocalTime.of(hour, minute)
            if (isStart) {
                startTime = pickedTime
                startTimeText.text = pickedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                endTime = pickedTime
                endTimeText.text = pickedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            }
        }, currentTime?.hour ?: defaultHour, currentTime?.minute ?: 0, true)
        timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        timePickerDialog.show()
    }

    private fun openColorPicker() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_color_palette, null)

        // 1. PopupWindow를 생성합니다.
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // 바깥 영역 터치 시 닫히도록 설정
        )
        popupWindow.elevation = 20f

        // 2. 각 색상 원에 실제 색을 칠하고, 클릭 리스너를 설정합니다.
        val colors = listOf(
            Pair(popupView.findViewById<View>(R.id.palette_color_1), "#4285F4"),
            Pair(popupView.findViewById<View>(R.id.palette_color_2), "#34A853"),
            Pair(popupView.findViewById<View>(R.id.palette_color_3), "#EA4335"),
            Pair(popupView.findViewById<View>(R.id.palette_color_4), "#FFBE00"),
            Pair(popupView.findViewById<View>(R.id.palette_color_5), "#A142F4"),
            Pair(popupView.findViewById<View>(R.id.palette_color_6), "#EB6E94")
        )

        colors.forEach { (colorView, colorHex) ->
            (colorView.background.mutate() as? GradientDrawable)?.setColor(Color.parseColor(colorHex))
            colorView.setOnClickListener {
                selectedColor = Color.parseColor(colorHex)
                colorDot.background.mutate().setTint(selectedColor)
                popupWindow.dismiss() // 색상 선택 시 팝업 닫기
            }
        }

        // 3. colorDot을 기준으로 팝업을 보여줍니다.
        popupWindow.showAsDropDown(colorDot)
    }

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
package com.example.mycalendar

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log // ✅ NEW
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mycalendar.model.*
import com.example.mycalendar.network.RetrofitClient
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.switchmaterial.SwitchMaterial
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddScheduleActivity : AppCompatActivity() {

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

    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private var startTime: LocalTime? = null
    private var endTime: LocalTime? = null
    private var selectedColor: Int = Color.parseColor("#4285F4")
    private var scheduleToEdit: Schedule? = null
    private var finalStartDateTime: LocalDateTime? = null
    private var finalEndDateTime: LocalDateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        initViews()
        initData()
        setupListeners()

        Log.d("AddScheduleActivity", "startDate 값: $startDate") // ✅ NEW
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

        val isCopyMode = intent.getBooleanExtra("isCopyMode", false)
        val copyButton = findViewById<ImageButton>(R.id.copy_button)

        if (scheduleToEdit != null) {
            val schedule = scheduleToEdit!!
            titleEditText.setText(schedule.title)
            startDate = schedule.startDate
            endDate = schedule.startDate
            startTime = schedule.startTime
            endTime = schedule.endTime
            selectedColor = schedule.color
            alarmSwitch.isChecked = schedule.alarmOn
            memoEditText.setText(schedule.memo)
            categoryEditText.setText(schedule.category)
            locationEditText.setText(schedule.location)
            if (startTime != null) {
                timeSwitch.isChecked = true
            }

            if (isCopyMode) {
                this.scheduleToEdit = null // ✅ NEW
                Toast.makeText(this, "일정이 복사되었습니다. 저장하여 새 일정으로 생성하세요.", Toast.LENGTH_LONG).show()
            }
            copyButton.visibility = View.VISIBLE
        } else {
            val initialDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("selectedDate", LocalDate::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("selectedDate") as? LocalDate
            } ?: LocalDate.now()
            startDate = initialDate
            endDate = initialDate
            copyButton.visibility = View.GONE
        }

        updateDateTextViews()
        colorDot.background.mutate().setTint(selectedColor)
        timePickerLayout.visibility = if (timeSwitch.isChecked) View.VISIBLE else View.GONE
        if (timeSwitch.isChecked) {
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

        saveButton.setOnClickListener {
            handleSave(isCopy = false)
        }

        copyButton.setOnClickListener {
            scheduleToEdit = null // ✅ NEW
            Toast.makeText(this, "일정이 복사되었습니다. 저장 버튼을 눌러 새 일정으로 생성하세요.", Toast.LENGTH_LONG).show()
            copyButton.isEnabled = false
            copyButton.alpha = 0.5f
        }

        dateRangeLayout.setOnClickListener { openDateRangePicker() }
        timeSwitch.setOnCheckedChangeListener { _, isChecked -> handleTimeSwitch(isChecked) }
        startTimeText.setOnClickListener { openTimePicker(isStart = true) }
        endTimeText.setOnClickListener { openTimePicker(isStart = false) }
        colorDot.setOnClickListener { openColorPicker() }
    }

    private fun handleSave(isCopy: Boolean) {
        // ✅ startDate와 endDate null 체크 및 기본값 설정
        if (startDate == null) startDate = LocalDate.now()
        if (endDate == null) endDate = startDate

        val title = titleEditText.text.toString()
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 날짜 포맷터
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedStartDate = startDate!!.format(dateFormatter)
        val formattedEndDate = endDate!!.format(dateFormatter)

        // ✅ 디버그 로그 추가
        Log.d("AddScheduleActivity", "저장 전 - startDate: $startDate, endDate: $endDate")
        Log.d("AddScheduleActivity", "포맷된 날짜 - startDate: $formattedStartDate, endDate: $formattedEndDate")

        // 복사 모드일 경우 copiedFromScheduleId를 설정
        val copiedFromScheduleId = if (isCopy && scheduleToEdit != null) {
            scheduleToEdit?.id
        } else {
            null
        }

        val request = ScheduleRequest(
            title = title,
            memo = memoEditText.text.toString().trim().ifBlank { null },
            location = locationEditText.text.toString().trim().ifBlank { null },
            category = categoryEditText.text.toString().trim().ifBlank { null },
            scheduledDate = formattedStartDate, // 기존 필드 유지
            startDate = formattedStartDate,     // ✅ 새로 추가
            endDate = formattedEndDate,         // ✅ 새로 추가
            startTime = if (timeSwitch.isChecked) startTime?.toString() else null,
            endTime = if (timeSwitch.isChecked) endTime?.toString() else null,
            allDay = !timeSwitch.isChecked,
            isConfirmed = true,
            color = String.format("#%06X", 0xFFFFFF and selectedColor),
            alarmOn = alarmSwitch.isChecked,
            copiedFromScheduleId = copiedFromScheduleId
        )

        // ✅ 요청 데이터 로그 출력
        Log.d("AddScheduleActivity", "서버 요청 데이터: $request")

        // 시작 및 종료 시간 설정
        if (timeSwitch.isChecked) {
            finalStartDateTime = LocalDateTime.of(startDate, startTime ?: LocalTime.of(9, 0))
            finalEndDateTime = LocalDateTime.of(endDate, endTime ?: LocalTime.of(10, 0))
        } else {
            finalStartDateTime = startDate?.atStartOfDay()
            finalEndDateTime = endDate?.atTime(23, 59, 59)
        }

        val start = finalStartDateTime
        val end = finalEndDateTime
        if (start != null && end != null && start.isAfter(end)) {
            Toast.makeText(this, "종료 시간이 시작 시간보다 빠를 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 서버에 일정 저장 요청
        RetrofitClient.apiService.createSchedule(request).enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<ScheduleResponse>>,
                response: Response<ApiResponse<ScheduleResponse>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("AddScheduleActivity", "일정 저장 성공 ID: ${response.body()?.data?.id}")
                    Toast.makeText(this@AddScheduleActivity, "일정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e("AddScheduleActivity", "저장 실패 - Response: ${response.body()}")
                    Toast.makeText(this@AddScheduleActivity, "저장 실패: ${response.body()?.message ?: "오류"}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                Log.e("AddScheduleActivity", "서버 요청 실패", t)
                Toast.makeText(this@AddScheduleActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
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
        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 20f

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
                popupWindow.dismiss()
            }
        }

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
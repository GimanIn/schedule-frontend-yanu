package com.example.mycalendar

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.util.*
import com.example.mycalendar.mapper.ScheduleMapper
import java.io.Serializable


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
            endDate = schedule.endDate
            startTime = schedule.startTime
            endTime = schedule.endTime
            selectedColor = schedule.color
            alarmSwitch.isChecked = schedule.alarmOn
            memoEditText.setText(schedule.memo)
            categoryEditText.setText(schedule.category)
            locationEditText.setText(schedule.location)
            if (startTime != null) timeSwitch.isChecked = true
            if (isCopyMode) {
                this.scheduleToEdit = null
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
    }

    private fun setupListeners() {
        val backButton = findViewById<ImageButton>(R.id.back_button)
        val copyButton = findViewById<ImageButton>(R.id.copy_button)
        val dateRangeLayout = findViewById<LinearLayout>(R.id.dateRangeLayout)
        val saveButton = findViewById<Button>(R.id.save_button)

        backButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { handleSave(isCopy = false) }
        copyButton.setOnClickListener {
            scheduleToEdit = null
            Toast.makeText(this, "일정이 복사되었습니다. 저장 버튼을 눌러 새 일정으로 생성하세요.", Toast.LENGTH_LONG).show()
            copyButton.isEnabled = false
            copyButton.alpha = 0.5f
        }

        dateRangeLayout.setOnClickListener { openDateRangePicker() }
        timeSwitch.setOnCheckedChangeListener { _, isChecked -> handleTimeSwitch(isChecked) }
        startTimeText.setOnClickListener { openTimePicker(true) }
        endTimeText.setOnClickListener { openTimePicker(false) }
        colorDot.setOnClickListener { openColorPicker() }
    }

    private fun handleSave(isCopy: Boolean) {
        if (startDate == null) startDate = LocalDate.now()
        if (endDate == null) endDate = startDate

        val title = titleEditText.text.toString()
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedStartDate = startDate!!.format(dateFormatter)
        val formattedEndDate = endDate!!.format(dateFormatter)

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

        val request = ScheduleRequest(
            title = title,
            memo = memoEditText.text.toString().trim().ifBlank { null },
            location = locationEditText.text.toString().trim().ifBlank { null },
            category = categoryEditText.text.toString().trim().ifBlank { null },
            scheduledDate = formattedStartDate,
            startDate = formattedStartDate,
            endDate = formattedEndDate,
            startTime = if (timeSwitch.isChecked) startTime?.toString() else null,
            endTime = if (timeSwitch.isChecked) endTime?.toString() else null,
            allDay = !timeSwitch.isChecked,
            isConfirmed = true,
            color = String.format("#%06X", 0xFFFFFF and selectedColor),
            alarmOn = alarmSwitch.isChecked,
            copiedFromScheduleId = if (isCopy) scheduleToEdit?.id else null
        )

        RetrofitClient.apiService.createSchedule(request).enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
            override fun onResponse(call: Call<ApiResponse<ScheduleResponse>>, response: Response<ApiResponse<ScheduleResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val schedule = response.body()?.data?.let { ScheduleMapper.toSchedule(it) }
                    val resultIntent = Intent()
                    // 🔁 이 코드로 수정하세요
                    resultIntent.putExtra("newSchedule", schedule as Serializable)

                    setResult(Activity.RESULT_OK, resultIntent)
                    Toast.makeText(this@AddScheduleActivity, "일정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddScheduleActivity, "저장 실패: ${response.body()?.message ?: "오류"}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                Toast.makeText(this@AddScheduleActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
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

    private fun openDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText("기간 선택").build()
        picker.addOnPositiveButtonClickListener { selection ->
            startDate = Instant.ofEpochMilli(selection.first).atZone(ZoneId.systemDefault()).toLocalDate()
            endDate = Instant.ofEpochMilli(selection.second).atZone(ZoneId.systemDefault()).toLocalDate()
            updateDateTextViews()
        }
        picker.show(supportFragmentManager, "DATE_PICKER")
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
        val current = if (isStart) startTime else endTime
        val defaultHour = if (isStart) 9 else 10
        val dialog = TimePickerDialog(this, { _, hour, minute ->
            val time = LocalTime.of(hour, minute)
            if (isStart) {
                startTime = time
                startTimeText.text = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                endTime = time
                endTimeText.text = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            }
        }, current?.hour ?: defaultHour, current?.minute ?: 0, true)
        dialog.show()
    }

    private fun openColorPicker() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_color_palette, null)
        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 20f

        val colors = listOf(
            Pair(popupView.findViewById<View>(R.id.palette_color_1), "#4285F4"),
            Pair(popupView.findViewById<View>(R.id.palette_color_2), "#34A853"),
            Pair(popupView.findViewById<View>(R.id.palette_color_3), "#EA4335"),
            Pair(popupView.findViewById<View>(R.id.palette_color_4), "#FFBE00"),
            Pair(popupView.findViewById<View>(R.id.palette_color_5), "#A142F4"),
            Pair(popupView.findViewById<View>(R.id.palette_color_6), "#EB6E94")
        )

        colors.forEach { (view, color) ->
            (view.background as? GradientDrawable)?.setColor(Color.parseColor(color))
            view.setOnClickListener {
                selectedColor = Color.parseColor(color)
                colorDot.background.mutate().setTint(selectedColor)
                popup.dismiss()
            }
        }

        popup.showAsDropDown(colorDot)
    }
}

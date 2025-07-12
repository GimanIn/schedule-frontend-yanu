package com.example.mycalendar

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText

    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        monthYearText = findViewById(R.id.monthYearText)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)
        val addButton: ImageButton = findViewById(R.id.addButton)

        findViewById<ImageView>(R.id.searchButton).setOnClickListener {
            Toast.makeText(this, "검색 기능 구현 예정", Toast.LENGTH_SHORT).show()
        }

        addButton.setOnClickListener {
            val scheduleTitle = scheduleEditText.text.toString()
            if (scheduleTitle.isNotEmpty()) {
                val newSchedule = Schedule(
                    title = scheduleTitle,
                    startTime = null,
                    endTime = null,
                    color = Color.GRAY,
                    isAlarmOn = false,
                    memo = "",
                    isConfirmed = true,
                    isPostponed = false
                )
                addSchedule(selectedDate, newSchedule)
                scheduleEditText.text.clear()
            } else {
                showAddScheduleDialog(selectedDate)
            }
        }

        updateCalendar()
    }

    private fun updateCalendar() {
        monthYearText.text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREA))
        val dayList = generateDaysInMonth(YearMonth.from(selectedDate))

        val adapter = CalendarAdapter(dayList, schedules, selectedDate) { date ->
            if (selectedDate == date) {
                val dailySchedules = schedules[date]
                if (dailySchedules.isNullOrEmpty()) {
                    showAddScheduleDialog(date)
                } else {
                    showScheduleListDialog(date, dailySchedules)
                }
            } else {
                selectedDate = date
                updateCalendar()
            }
        }

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = adapter
        updateScheduleHint(selectedDate)
    }

    private fun generateDaysInMonth(yearMonth: YearMonth): ArrayList<LocalDate> {
        val dayList = ArrayList<LocalDate>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7

        for (i in 0 until dayOfWeekOfFirst) { dayList.add(LocalDate.MIN) }
        for (i in 1..yearMonth.lengthOfMonth()) { dayList.add(yearMonth.atDay(i)) }
        return dayList
    }

    private fun updateScheduleHint(date: LocalDate) {
        val hintFormatter = DateTimeFormatter.ofPattern("M월 d일 일정 추가", Locale.KOREA)
        scheduleEditText.hint = hintFormatter.format(date)
    }

    private fun addSchedule(date: LocalDate, schedule: Schedule) {
        schedules.computeIfAbsent(date) { mutableListOf() }.add(schedule)
        updateCalendar()
    }

    private fun showAddScheduleDialog(date: LocalDate) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_schedule, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.titleEditText)
        val timeSwitch = dialogView.findViewById<SwitchMaterial>(R.id.timeSwitch)
        val timeLayout = dialogView.findViewById<LinearLayout>(R.id.timeLayout)
        val startTimeLayout = dialogView.findViewById<View>(R.id.startTimeLayout)
        val endTimeLayout = dialogView.findViewById<View>(R.id.endTimeLayout)
        val startTimeText = dialogView.findViewById<TextView>(R.id.startTimeText)
        val endTimeText = dialogView.findViewById<TextView>(R.id.endTimeText)
        val colorRadioGroup = dialogView.findViewById<RadioGroup>(R.id.colorRadioGroup)
        val alarmSwitch = dialogView.findViewById<SwitchMaterial>(R.id.alarmSwitch)
        val memoEditText = dialogView.findViewById<EditText>(R.id.memoEditText)
        val confirmSwitch = dialogView.findViewById<SwitchMaterial>(R.id.confirmSwitch)
        val postponeSwitch = dialogView.findViewById<SwitchMaterial>(R.id.postponeSwitch)

        var startTime: LocalTime? = LocalTime.of(9, 0)
        var endTime: LocalTime? = LocalTime.of(10, 0)

        timeSwitch.setOnCheckedChangeListener { _, isChecked ->
            timeLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // 시작 시간 레이아웃 클릭 시
        startTimeLayout.setOnClickListener {
            // 스피너 스타일 테마를 직접 지정하여 TimePickerDialog 생성
            val timePickerDialog = TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, { _, hour, minute ->
                startTime = LocalTime.of(hour, minute)
                startTimeText.text = startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            }, startTime?.hour ?: 9, startTime?.minute ?: 0, true)
            // 윈도우 배경을 투명하게 하여 테마가 깨지지 않도록 함
            timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            timePickerDialog.show()
        }

        // 종료 시간 레이아웃 클릭 시
        endTimeLayout.setOnClickListener {
            val timePickerDialog = TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, { _, hour, minute ->
                endTime = LocalTime.of(hour, minute)
                endTimeText.text = endTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            }, endTime?.hour ?: 10, endTime?.minute ?: 0, true)
            timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            timePickerDialog.show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val title = titleEditText.text.toString()
                if (title.isEmpty()) {
                    Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val finalStartTime = if (timeSwitch.isChecked) startTime else null
                val finalEndTime = if (timeSwitch.isChecked) endTime else null

                val selectedColorId = colorRadioGroup.checkedRadioButtonId
                val selectedRadioButton = dialogView.findViewById<RadioButton>(selectedColorId)
                val color = when (selectedRadioButton.text.toString()) {
                    getString(R.string.color_red) -> "#EF9A9A".toColorInt()
                    getString(R.string.color_blue) -> "#90CAF9".toColorInt()
                    getString(R.string.color_green) -> "#A5D6A7".toColorInt()
                    else -> Color.GRAY
                }

                val schedule = Schedule(
                    title = title,
                    startTime = finalStartTime,
                    endTime = finalEndTime,
                    color = color,
                    isAlarmOn = alarmSwitch.isChecked,
                    memo = memoEditText.text.toString(),
                    isConfirmed = confirmSwitch.isChecked,
                    isPostponed = postponeSwitch.isChecked
                )
                addSchedule(date, schedule)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showScheduleListDialog(date: LocalDate, scheduleList: List<Schedule>) {
        val items = scheduleList.map {
            val timeString = if(it.startTime != null && it.endTime != null) {
                "${it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${it.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } else ""
            "${it.title} $timeString"
        }.toTypedArray()

        // 커스텀 타이틀 뷰 설정
        val titleView = layoutInflater.inflate(R.layout.dialog_title_with_close, null)
        titleView.findViewById<TextView>(R.id.titleTextView).text = "${date.monthValue}월 ${date.dayOfMonth}일 일정 목록"

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setItems(items, null)
            // .setPositiveButton("닫기", null) // 👈 이 줄을 삭제했습니다.
            .setPositiveButton("일정 추가") { _, _ -> // 👈 Neutral을 Positive로 변경하여 오른쪽으로 이동
                showAddScheduleDialog(date)
            }
            .create()

        titleView.findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            dialog.dismiss() // 'X' 버튼 클릭 시 닫기
        }

        dialog.show()
    }
}
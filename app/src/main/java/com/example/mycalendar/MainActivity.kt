package com.example.mycalendar

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.os.Build
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: CalendarAdapter // 어댑터를 멤버 변수로 이동

    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    private var selectedDate: LocalDate = LocalDate.now()

    // AddScheduleActivity로부터 결과를 받아 처리할 '런처'
    private val addScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val newSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getSerializableExtra("newSchedule", Schedule::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getSerializableExtra("newSchedule") as? Schedule
            }
            if (newSchedule != null) {
                addSchedule(newSchedule)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 모든 UI 요소를 먼저 찾아서 변수에 할당합니다. 수정함.
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar) // 👈 여기서 toolbar가 초기화됩니다.
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)
        val addButton: ImageButton = findViewById(R.id.addButton)
        val searchButton: ImageView = findViewById(R.id.searchButton)

        // 2. Toolbar 관련 설정을 합니다.
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        val navView = findViewById<NavigationView>(R.id.nav_view)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            val handled = when (menuItem.itemId) {
                R.id.nav_year -> {
                    true
                }

                R.id.nav_month -> {
                    true
                }

                R.id.nav_day -> {
                    true
                }

                R.id.nav_mypage -> {
                    true
                }

                else -> false
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }

        // 3. 버튼 리스너들을 설정합니다.
        searchButton.setOnClickListener {
            Toast.makeText(this, "검색 기능 구현 예정", Toast.LENGTH_SHORT).show()
        }

        // 하단 바 '+' 버튼 클릭 리스너
        addButton.setOnClickListener {
            val scheduleTitle = scheduleEditText.text.toString()
            if (scheduleTitle.isNotEmpty()) {
                val newSchedule = Schedule(
                    title = scheduleTitle,
                    startDateTime = null,
                    endDateTime = null,
                    color = Color.GRAY,
                    isAlarmOn = false,
                    memo = "",
                    isConfirmed = true,
                    isPostponed = false
                )
                addSchedule(newSchedule)
                scheduleEditText.text.clear()
            } else {
                openAddScheduleActivity(selectedDate)
            }
        }

        setupCalendar() // 캘린더 초기 설정
    }

    private fun setupCalendar() {
        adapter = CalendarAdapter(ArrayList(), schedules) { date ->
            // --- 여기가 모든 날짜 클릭 로직을 담당하는 최종 버전입니다 ---

            // Case 2: 이미 선택된 날짜를 다시 클릭했을 경우 (동작)
            if (selectedDate == date) {
                val dailySchedules = schedules[date]
                if (dailySchedules.isNullOrEmpty()) {
                    // 일정이 없으면 -> 일정 추가 화면 열기
                    openAddScheduleActivity(date)
                } else {
                    // 일정이 있으면 -> 일정 목록 다이얼로그 열기
                    val dialog = ScheduleListDialog(date, dailySchedules.toMutableList(), {
                        updateCalendar()
                    }, { clickedDate ->
                        openAddScheduleActivity(clickedDate)
                    })
                    dialog.show(supportFragmentManager, "ScheduleListDialog")
                }
            }
            // Case 1: 새로운 날짜를 클릭했을 경우 (선택)
            else {
                selectedDate = date
                updateCalendar() // 선택 상태를 갱신하고 하단 바 텍스트를 바꾸기 위해 갱신
            }
        }
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = adapter
        updateCalendar() // 앱 실행 시 첫 화면 로드
    }


    private fun updateCalendar() {
        toolbar.title = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREA))
        val dayList = generateDaysInMonth(YearMonth.from(selectedDate))

        // 어댑터에 데이터만 새로 채우고, 갱신을 알립니다.
        adapter.dayList = dayList
        adapter.selectedDate = selectedDate
        adapter.notifyDataSetChanged()

        updateScheduleHint(selectedDate)
    }

    // AddScheduleActivity를 여는 함수
    private fun openAddScheduleActivity(date: LocalDate) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("selectedDate", date)
        addScheduleLauncher.launch(intent)
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

    fun addSchedule(schedule: Schedule) {
        // 일정에 시작 날짜가 지정되어 있으면 그것을 사용하고,
        // 없으면 (하단 바에서 바로 추가한 경우) 현재 선택된 날짜를 사용합니다.
        val startDate = schedule.startDateTime?.toLocalDate() ?: selectedDate

        // 기간이 없는 당일 일정 처리
        if (schedule.endDateTime == null || startDate == schedule.endDateTime.toLocalDate()) {
            schedules.computeIfAbsent(startDate) { mutableListOf() }.add(schedule)
        } else {
            // 기간이 있는 일정 처리
            var currentDate = startDate
            val endDate = schedule.endDateTime.toLocalDate()

            while (!currentDate.isAfter(endDate)) {
                schedules.computeIfAbsent(currentDate) { mutableListOf() }.add(schedule)
                currentDate = currentDate.plusDays(1)
            }
        }
        // --- 디버깅을 위한 코드 ---
        val count = schedules[startDate]?.size ?: 0
        Toast.makeText(this, "${startDate.dayOfMonth}일에 이제 ${count}개의 일정이 있습니다.", Toast.LENGTH_SHORT).show()
        // --- 여기까지 ---
        updateCalendar()
    }

}
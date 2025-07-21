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
import android.view.GestureDetector
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: CalendarAdapter // 어댑터를 멤버 변수로 이동
    private lateinit var gestureDetector: androidx.core.view.GestureDetectorCompat

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
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

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

        // 1. 우리가 만든 제스처 리스너를 사용하여 제스처 감지기를 생성합니다.
        gestureDetector = androidx.core.view.GestureDetectorCompat(this, SwipeGestureListener())

        // 2. 캘린더(RecyclerView)의 터치 이벤트를 제스처 감지기가 처리하도록 설정합니다.
        // 이 방법은 클릭과 스와이프를 모두 온전히 지원합니다.
        calendarRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
        // --- 여기까지 추가 ---
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

    // --- 👇 1. 수정 전용 결과 처리기를 새로 추가합니다. ---
    val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // 1. "updatedSchedule" 키로 수정된 일정이 있는지 먼저 확인
            val updatedSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getSerializableExtra("updatedSchedule", Schedule::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getSerializableExtra("updatedSchedule") as? Schedule
            }

            if (updatedSchedule != null) {
                // 수정된 일정이 있다면 -> 기존 것 삭제 후 새로 추가
                removeSchedule(updatedSchedule)
                addSchedule(updatedSchedule)
            } else {
                // 2. "updatedSchedule"가 없다면, "newSchedule" 키로 복사된 새 일정이 있는지 확인
                val copiedSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.getSerializableExtra("newSchedule", Schedule::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data?.getSerializableExtra("newSchedule") as? Schedule
                }
                if (copiedSchedule != null) {
                    // 복사된 새 일정이 있다면 -> 그냥 추가
                    addSchedule(copiedSchedule)
                }
            }
        }
    }

    // --- 👇 2. AddScheduleActivity를 '수정 모드'로 여는 함수를 추가합니다. ---
    fun openEditScheduleActivity(schedule: Schedule, isCopy: Boolean) {
        val intent = Intent(this, AddScheduleActivity::class.java).apply {
            putExtra("scheduleToEdit", schedule)
            putExtra("isCopyMode", isCopy)
        }
        editScheduleLauncher.launch(intent)
    }

    // --- 👇 3. 기존 일정을 삭제하는 함수를 추가합니다. ---
    fun removeSchedule(scheduleToRemove: Schedule) {
        // 모든 날짜를 순회하며 해당 일정을 찾아서 삭제
        val entries = schedules.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            val scheduleList = entry.value
            // 👇 제목/시간 대신, 고유 ID로 일치하는 것을 찾아 삭제합니다.
            scheduleList.removeAll { it.id == scheduleToRemove.id }
            if (scheduleList.isEmpty()) {
                entries.remove()
            }
        }
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
        // 변경될 수 있는 var 변수를 변경 불가능한 val 지역 변수에 담아서 사용합니다.
        val startDateTime = schedule.startDateTime
        val endDateTime = schedule.endDateTime

        val startDate = startDateTime?.toLocalDate() ?: selectedDate

        if (endDateTime == null || startDate == endDateTime.toLocalDate()) {
            schedules.computeIfAbsent(startDate) { mutableListOf() }.add(schedule)
        } else {
            var currentDate = startDate
            val endDate = endDateTime.toLocalDate()

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


    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // e1이 null이면 시작점을 알 수 없으므로 무시
            if (e1 == null) return false

            val diffX = e2.x - e1.x
            // 스와이프 방향과 속도를 감지
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    // 오른쪽으로 스와이프 -> 이전 달
                    selectedDate = selectedDate.minusMonths(1)
                    updateCalendar()
                } else {
                    // 왼쪽으로 스와이프 -> 다음 달
                    selectedDate = selectedDate.plusMonths(1)
                    updateCalendar()
                }
                return true // 이벤트 처리를 완료했음을 알림
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

}
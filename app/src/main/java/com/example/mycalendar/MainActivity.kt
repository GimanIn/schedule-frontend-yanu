package com.example.mycalendar

import android.util.Log // ✅ 로그 사용을 위한 필수 import
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.* // ✅ Schedule, ScheduleResponse, toSchedule 등 포함됨
import com.example.mycalendar.network.RetrofitClient
import com.example.mycalendar.mapper.ScheduleMapper // new 변환 함수 매퍼 추가
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.mycalendar.LoginActivity.Companion.PREFS_NAME
import com.example.mycalendar.LoginActivity.Companion.KEY_ACCESS_TOKEN
import com.example.mycalendar.LoginActivity.Companion.KEY_REFRESH_TOKEN
import com.example.mycalendar.LoginActivity.Companion.KEY_TOKEN_EXPIRY
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog


class MainActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: CalendarAdapter
    private lateinit var gestureDetector: androidx.core.view.GestureDetectorCompat
    private lateinit var prefs: SharedPreferences

    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    private var selectedDate: LocalDate = LocalDate.now()

    // ✅ FIX: copiedFromScheduleId 변수 추가
    private var copiedFromScheduleId: Long? = null

    private val addScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchSchedulesForDate(selectedDate) // ✅ NEW
        }
    }

    private val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchSchedulesForDate(selectedDate) // ✅ NEW
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE) // ✅ NEW: LoginActivity와 동일하게

        // ⬇️ 💥 반드시 View들 먼저 초기화해야 함
        toolbar = findViewById(R.id.toolbar)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)
        val addButton: ImageButton = findViewById(R.id.addButton)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        // ✅ NEW: 현재 저장된 토큰 로그로 출력해 보기
        val debugAccessToken = prefs.getString(LoginActivity.KEY_ACCESS_TOKEN, "없음")
        Log.d("MainActivity", "🧪 저장된 access_token: $debugAccessToken")

        // ✅ NEW: 로그인 상태 확인 및 토큰 자동 갱신
        checkLoginAndRefreshTokenIfNeeded {
            setupCalendar()
            fetchSchedulesForDate(selectedDate)
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            val handled = when (menuItem.itemId) {
                R.id.nav_year -> {
                    startActivity(Intent(this, YearActivity::class.java))
                    true
                }

                R.id.nav_month -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }

                R.id.nav_day -> {
                    val schedulesForDay = getSchedulesForDate(LocalDate.now())
                    val dialog = ScheduleListDialog(
                        selectedDate,
                        schedulesForDay.toMutableList(),
                        onDataChanged = { updateCalendar() },
                        onAddNewSchedule = { date -> openAddScheduleActivity(date) }
                    )
                    dialog.show(supportFragmentManager, "ScheduleListDialog")
                    true
                }

                R.id.nav_mypage -> {
                    startActivity(Intent(this, MypageActivity::class.java))
                    true
                }

                else -> false
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }

        searchButton.setOnClickListener {
            val allSchedules = schedules.values.flatten().distinctBy { it.id }
            val intent = Intent(this, SearchActivity::class.java).apply {
                putExtra("allSchedules", ArrayList(allSchedules))
            }
            startActivity(intent)
        }

        addButton.setOnClickListener {

            val title = scheduleEditText.text.toString()
            if (title.isNotBlank()) {
                // ✅ FIX: ScheduleRequest에 필요한 모든 필드 추가
                val request = ScheduleRequest(
                    title = scheduleEditText.text.toString(), // ✅ 사용자 입력
                    memo = "",                                // 또는 memoEditText.text.toString()
                    location = "",                            // locationEditText.text.toString()
                    category = "",                            // categorySpinner.selectedItem.toString()
                    scheduledDate = selectedDate.toString(),  // 기존 필드 (호환성)
                    startDate = selectedDate.toString(),      // ✅ FIX: 새로 추가된 필드
                    endDate = selectedDate.toString(),        // ✅ FIX: 새로 추가된 필드
                    startTime = LocalTime.of(9, 0).toString(), // ✅ NEW: 명시적이고 타입 안전한 방식
                    endTime = LocalTime.of(10, 0).toString(),  // ✅ NEW
                    allDay = false,
                    isConfirmed = true,
                    color = "#4285F4",
                    alarmOn = true,
                    copiedFromScheduleId = copiedFromScheduleId // ✅ NEW: 복사된 일정 ID를 전달
                )

                RetrofitClient.apiService.createSchedule(request) // ✅ NEW
                    .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                        override fun onResponse(
                            call: Call<ApiResponse<ScheduleResponse>>,
                            response: Response<ApiResponse<ScheduleResponse>>
                        ) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                fetchSchedulesForDate(selectedDate) // ✅ NEW
                                Toast.makeText(this@MainActivity, "일정 추가됨", Toast.LENGTH_SHORT).show()
                                scheduleEditText.text.clear()
                            } else {
                                Toast.makeText(this@MainActivity, "일정 저장 실패", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                            Toast.makeText(this@MainActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                openAddScheduleActivity(selectedDate)
            }
        }

        val aiButton = findViewById<ImageButton>(R.id.aiButton)

        aiButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_summary, null)
            val summaryDateText = dialogView.findViewById<TextView>(R.id.summaryDateText)
            val summaryContentText = dialogView.findViewById<TextView>(R.id.summaryContentText)

            val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
            summaryDateText.text = "오늘 ${LocalDate.now().format(formatter)}"

            RetrofitClient.apiService.getAiSummary(LocalDate.now().toString())
                .enqueue(object : Callback<ApiResponse<AiSummaryResponse>> {
                    override fun onResponse(
                        call: Call<ApiResponse<AiSummaryResponse>>,
                        response: Response<ApiResponse<AiSummaryResponse>>
                    ) {
                        val summary = response.body()?.data?.summaryText
                        summaryContentText.text = summary ?: "요약을 가져오지 못했습니다."
                    }

                    override fun onFailure(
                        call: Call<ApiResponse<AiSummaryResponse>>,
                        t: Throwable
                    ) {
                        summaryContentText.text = "서버 오류 발생: ${t.localizedMessage}"
                    }
                })

            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("닫기", null)
                .show()
        }

        // ✅ 딥링크로 공유 일정 열기
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null && uri.scheme == "mycalendar" && uri.host == "schedule") {
                try {
                    val title = uri.getQueryParameter("title")?.takeIf { it.isNotBlank() } ?: "제목 없음"
                    val startStr = uri.getQueryParameter("start")?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                        ?: selectedDate.toString()
                    val endStr = uri.getQueryParameter("end") ?: selectedDate.toString()
                    val colorStr = uri.getQueryParameter("color") ?: "#4285F4"
                    val category = uri.getQueryParameter("category") ?: ""
                    val location = uri.getQueryParameter("location") ?: ""
                    val memo = uri.getQueryParameter("memo") ?: ""

                    val request = ScheduleRequest(
                        title = title,
                        memo = memo,
                        location = location,
                        category = category,
                        scheduledDate = startStr,
                        startDate = startStr,
                        endDate = endStr,
                        startTime = "09:00",
                        endTime = "10:00",
                        allDay = false,
                        isConfirmed = true,
                        color = colorStr,
                        alarmOn = true,
                        copiedFromScheduleId = copiedFromScheduleId
                    )

                    RetrofitClient.apiService.createSchedule(request)
                        .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                            override fun onResponse(
                                call: Call<ApiResponse<ScheduleResponse>>,
                                response: Response<ApiResponse<ScheduleResponse>>
                            ) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    fetchSchedulesForDate(selectedDate)
                                    Toast.makeText(this@MainActivity, "공유된 일정이 추가되었습니다.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "공유 일정 저장 실패", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                                Toast.makeText(this@MainActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        })
                } catch (e: Exception) {
                    Toast.makeText(this, "일정을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchSchedulesForDate(date: LocalDate) {
        RetrofitClient.apiService.getSchedulesByDate(date.toString())
            .enqueue(object : Callback<ApiResponse<List<ScheduleResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<ScheduleResponse>>>,
                    response: Response<ApiResponse<List<ScheduleResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val responseList = response.body()?.data ?: emptyList()
                        val fetchedList = ScheduleMapper.toSchedule(responseList)
                        schedules[date] = fetchedList.toMutableList()
                        updateCalendar()
                    } else {
                        Toast.makeText(this@MainActivity, "일정 불러오기 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<List<ScheduleResponse>>>,
                    t: Throwable
                ) {
                    Toast.makeText(this@MainActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupCalendar() {
        adapter = CalendarAdapter(ArrayList(), schedules) { date ->
            if (selectedDate == date) {
                val dailySchedules = schedules[date]
                if (dailySchedules.isNullOrEmpty()) {
                    openAddScheduleActivity(date)
                } else {
                    val dialog = ScheduleListDialog(date, dailySchedules.toMutableList(), {
                        updateCalendar()
                    }, { clickedDate ->
                        openAddScheduleActivity(clickedDate)
                    })
                    dialog.show(supportFragmentManager, "ScheduleListDialog")
                }
            } else {
                selectedDate = date
                updateCalendar()
            }
        }
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = adapter

        gestureDetector = androidx.core.view.GestureDetectorCompat(this, SwipeGestureListener())
        calendarRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        updateCalendar()
    }

    private fun updateCalendar() {
        toolbar.title = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREA))
        val dayList = generateDaysInMonth(YearMonth.from(selectedDate))
        adapter.dayList = dayList
        adapter.selectedDate = selectedDate
        adapter.notifyDataSetChanged()
        updateScheduleHint(selectedDate)
    }

    private fun openAddScheduleActivity(date: LocalDate) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("selectedDate", date)
        addScheduleLauncher.launch(intent)
    }

    fun openEditScheduleActivity(schedule: Schedule, isCopy: Boolean = false) {
        val intent = Intent(this, AddScheduleActivity::class.java).apply {
            putExtra("scheduleToEdit", schedule)
            putExtra("isCopyMode", isCopy)
        }
        editScheduleLauncher.launch(intent)
    }

    fun removeSchedule(scheduleToRemove: Schedule) {
        val scheduleId = scheduleToRemove.id
        if (scheduleId != null) {
            RetrofitClient.apiService.deleteSchedule(scheduleId.toLong())
                .enqueue(object : Callback<ApiResponse<Unit>> {
                    override fun onResponse(
                        call: Call<ApiResponse<Unit>>,
                        response: Response<ApiResponse<Unit>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // 로컬에서도 삭제
                            val entries = schedules.iterator()
                            while (entries.hasNext()) {
                                val entry = entries.next()
                                val scheduleList = entry.value
                                scheduleList.removeAll { it.id == scheduleToRemove.id }
                                if (scheduleList.isEmpty()) {
                                    entries.remove()
                                }
                            }
                            Toast.makeText(this@MainActivity, "일정 삭제 완료", Toast.LENGTH_SHORT).show()
                            updateCalendar()
                        } else {
                            Toast.makeText(this@MainActivity, "서버 삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "삭제 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "삭제할 일정 ID가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun addSchedule(schedule: Schedule) {
        val startDate = schedule.startDate
        val endDate = if (schedule.startDate == selectedDate && schedule.endTime != null && schedule.endTime < schedule.startTime) {
            startDate.plusDays(1) // 다음날까지 일정이 이어지는 경우
        } else {
            startDate
        }

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            schedules.computeIfAbsent(currentDate) { mutableListOf() }.add(schedule)
            currentDate = currentDate.plusDays(1)
        }

        val count = schedules[startDate]?.size ?: 0
        Toast.makeText(this, "${startDate.dayOfMonth}일에 이제 ${count}개의 일정이 있습니다.", Toast.LENGTH_SHORT).show()
        updateCalendar()
    }

    private fun generateDaysInMonth(yearMonth: YearMonth): ArrayList<LocalDate> {
        val dayList = ArrayList<LocalDate>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7

        for (i in 0 until dayOfWeekOfFirst) {
            dayList.add(LocalDate.MIN)
        }
        for (i in 1..yearMonth.lengthOfMonth()) {
            dayList.add(yearMonth.atDay(i))
        }
        return dayList
    }

    private fun updateScheduleHint(date: LocalDate) {
        val hintFormatter = DateTimeFormatter.ofPattern("M월 d일 일정 추가", Locale.KOREA)
        scheduleEditText.hint = hintFormatter.format(date)
    }

    private fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        return schedules[date] ?: emptyList()
    }

    // ✅ NEW: 토큰 자동 갱신 + 로그인 상태 확인 함수
    private fun checkLoginAndRefreshTokenIfNeeded(onSuccess: () -> Unit) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE) // ✅ LoginActivity에서 가져온 상수

        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        val now = System.currentTimeMillis() // ✅ 꼭 필요!

        if (accessToken.isNullOrEmpty() || expiryTime == 0L || now > expiryTime) {
            // 🔄 만료된 경우 → refreshToken으로 재요청
            if (!refreshToken.isNullOrEmpty()) {
                RetrofitClient.apiService.refreshAccessToken("Bearer $refreshToken")
                    .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                        override fun onResponse(call: Call<ApiResponse<LoginResponse>>, response: Response<ApiResponse<LoginResponse>>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                val loginData = response.body()?.data
                                if (loginData != null) {
                                    with(prefs.edit()) {
                                        putString("access_token", loginData.token)
                                        putString("refresh_token", loginData.refreshToken)
                                        putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + 1000 * 60 * 60) // 1시간 후
                                        apply()
                                    }
                                    onSuccess()
                                } else {
                                    Toast.makeText(this@MainActivity, "토큰 갱신 실패", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                                    finish()
                                }
                            } else {
                                Toast.makeText(this@MainActivity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                                finish()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                            Toast.makeText(this@MainActivity, "네트워크 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finish()
                        }
                    })
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } else {
            // ✅ 토큰 아직 유효
            onSuccess()
        }
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    selectedDate = selectedDate.minusMonths(1)
                } else {
                    selectedDate = selectedDate.plusMonths(1)
                }
                updateCalendar()
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }
}
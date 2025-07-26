package com.example.mycalendar

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.databinding.ActivityMainBinding
import com.example.mycalendar.mapper.ScheduleMapper
import com.example.mycalendar.model.*
import com.example.mycalendar.network.RetrofitClient
import com.example.mycalendar.network.ApiService
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var gestureDetector: GestureDetector
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    private var selectedDate: LocalDate = LocalDate.now()
    private var copiedFromScheduleId: Long? = null

    private val addScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newSchedule = result.data?.getSerializableExtra("newSchedule") as? Schedule
            if (newSchedule != null) {
                addScheduleToMap(newSchedule) // ✅ UI에 바로 반영
            } else {
                fetchAllSchedulesForMonth()
            }
        }
    }

    private val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchSchedulesForDate(selectedDate)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RetrofitClient 초기화
        RetrofitClient.init(this)

        prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        toolbar = findViewById(R.id.toolbar)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)

        val addButton: ImageButton = findViewById(R.id.addButton)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        checkLoginAndRefreshTokenIfNeeded {
            setupCalendar()
            fetchAllSchedulesForMonth()
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Navigation Drawer 메뉴 처리
        findViewById<NavigationView>(R.id.nav_view).setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_year -> {
                    startActivity(Intent(this, YearActivity::class.java))
                    true
                }
                R.id.nav_month -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_day -> {
                    openDayView()
                    true
                }
                R.id.nav_mypage -> {
                    startActivity(Intent(this, MypageActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    handleNotificationSettings()
                    true
                }
                else -> false
            }.also { drawerLayout.closeDrawer(GravityCompat.START) }
        }

        searchButton.setOnClickListener { openSearchActivity() }
        addButton.setOnClickListener { handleAddButtonClick() }

        // AI 요약 버튼
        findViewById<ImageButton>(R.id.aiButton)?.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_summary, null)
            val summaryDateText = dialogView.findViewById<TextView>(R.id.summaryDateText)
            val summaryContentText = dialogView.findViewById<TextView>(R.id.summaryContentText)
            // TODO: AI 요약 호출 로직
        }

        gestureDetector = GestureDetector(this, SwipeGestureListener())

        calendarRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    fun removeSchedule(schedule: Schedule) {
        val list = schedules[schedule.scheduledDate]
        list?.remove(schedule)
        updateCalendar()
    }

    fun addSchedule(schedule: Schedule) {
        val key = schedule.scheduledDate
        val list = schedules[key] ?: mutableListOf()
        list.add(schedule)
        schedules[key] = list
        updateCalendar()
    }

    fun addSchedule(request: ScheduleRequest) {
        RetrofitClient.apiService.createSchedule(request)
            .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<ScheduleResponse>>,
                    response: Response<ApiResponse<ScheduleResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val scheduleResponse = response.body()?.data
                        val schedule = scheduleResponse?.let { ScheduleMapper.toSchedule(it) }

                        if (schedule != null) {
                            addScheduleToMap(schedule)
                        }

                        Toast.makeText(this@MainActivity, "일정 추가 성공", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "일정 추가 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ✅ 일정 맵에 추가
    fun addScheduleToMap(schedule: Schedule) {
        var current = schedule.startDate
        val end = schedule.endDate

        while (!current.isAfter(end)) {
            if (!schedules.containsKey(current)) {
                schedules[current] = mutableListOf()
            }
            schedules[current]?.add(schedule)
            current = current.plusDays(1)
        }
        updateCalendar()
    }

    fun fetchAllSchedulesForMonth() {
        val currentYearMonth = YearMonth.from(selectedDate)
        val startDate = currentYearMonth.atDay(1)
        val endDate = currentYearMonth.atEndOfMonth()

        RetrofitClient.apiService.getSchedulesByDateRange(
            startDate.toString(), endDate.toString()
        ).enqueue(object : Callback<ApiResponse<List<ScheduleResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<ScheduleResponse>>>,
                response: Response<ApiResponse<List<ScheduleResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = ScheduleMapper.toSchedule(response.body()?.data ?: emptyList())
                    schedules.clear()
                    list.forEach { schedule ->
                        val key = schedule.scheduledDate
                        schedules[key] = (schedules[key] ?: mutableListOf()).apply {
                            add(schedule)
                        }
                    }
                    updateCalendar()
                } else {
                    Log.e("MainActivity", "❌ 전체 일정 조회 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ScheduleResponse>>>, t: Throwable) {
                Log.e("MainActivity", "❌ 서버 오류", t)
            }
        })
    }

    private fun fetchSchedulesForDate(date: LocalDate) {
        RetrofitClient.apiService.getSchedulesByDate(date.toString())
            .enqueue(object : Callback<ApiResponse<List<ScheduleResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<ScheduleResponse>>>,
                    response: Response<ApiResponse<List<ScheduleResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val list = ScheduleMapper.toSchedule(response.body()?.data ?: emptyList())
                        schedules[date] = mutableListOf()
                        list.forEach { schedule ->
                            val key = schedule.scheduledDate
                            if (schedules[key] == null) schedules[key] = mutableListOf()
                            schedules[key]?.add(schedule)
                        }
                        updateCalendar()
                    } else {
                        Log.e("MainActivity", "❌ API 응답 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ScheduleResponse>>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "일정 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter(generateDaysInMonth(YearMonth.from(selectedDate)), schedules) { date ->
            if (selectedDate == date) {
                openAddOrListDialog(date)
            } else {
                selectedDate = date
                updateCalendar()
            }
        }

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = calendarAdapter
        updateCalendar()
    }

    private fun updateCalendar() {
        toolbar.title = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREA))
        calendarAdapter.dayList = generateDaysInMonth(YearMonth.from(selectedDate))
        calendarAdapter.selectedDate = selectedDate
        calendarAdapter.notifyDataSetChanged()
        updateScheduleHint(selectedDate)
    }

    private fun openAddScheduleActivity(date: LocalDate) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("selectedDate", date)
        addScheduleLauncher.launch(intent)
    }

    private fun openDayView() {
        val schedulesForDay = getSchedulesForDate(LocalDate.now())
        ScheduleListDialog(
            selectedDate,
            schedulesForDay.toMutableList(),
            onDataChanged = { updateCalendar() },
            onAddNewSchedule = { date -> openAddScheduleActivity(date) }
        ).show(supportFragmentManager, "ScheduleListDialog")
    }

    private fun openSearchActivity() {
        val allSchedules = schedules.values.flatten().distinctBy { it.id }
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("allSchedules", ArrayList(allSchedules))
        }
        startActivity(intent)
    }

    private fun handleAddButtonClick() {
        val title = scheduleEditText.text.toString()
        if (title.isNotBlank()) {
            val request = ScheduleRequest(
                title = title,
                memo = "",
                location = "",
                category = "",
                scheduledDate = selectedDate.toString(),
                startDate = selectedDate.toString(),
                endDate = selectedDate.toString(),
                startTime = LocalTime.of(9, 0).toString(),
                endTime = LocalTime.of(10, 0).toString(),
                allDay = false,
                isConfirmed = true,
                color = "#4285F4",
                alarmOn = true,
                copiedFromScheduleId = copiedFromScheduleId
            )
            addSchedule(request)
            scheduleEditText.text.clear()
        } else {
            openAddScheduleActivity(selectedDate)
        }
    }

    private fun openAddOrListDialog(date: LocalDate) {
        val dailySchedules = schedules[date]
        if (dailySchedules.isNullOrEmpty()) {
            openAddScheduleActivity(date)
        } else {
            ScheduleListDialog(date, dailySchedules.toMutableList(), {
                updateCalendar()
            }, { clickedDate ->
                openAddScheduleActivity(clickedDate)
            }).show(supportFragmentManager, "ScheduleListDialog")
        }
    }

    private fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        return schedules[date] ?: emptyList()
    }

    private fun generateDaysInMonth(yearMonth: YearMonth): ArrayList<LocalDate?> {
        val dayList = ArrayList<LocalDate?>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7

        for (i in 0 until dayOfWeekOfFirst) dayList.add(null)
        for (i in 1..daysInMonth) dayList.add(LocalDate.of(yearMonth.year, yearMonth.month, i))

        return dayList
    }

    private fun updateScheduleHint(date: LocalDate) {
        scheduleEditText.hint = date.format(DateTimeFormatter.ofPattern("M월 d일 일정 추가", Locale.KOREA))
    }

    private fun handleNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                showNotificationDialog()
            } else {
                goToAppNotificationSettings()
            }
        } else {
            goToAppNotificationSettings()
        }
    }

    private fun showNotificationDialog() {
        AlertDialog.Builder(this)
            .setTitle("알림 권한 필요")
            .setMessage("일정 알림을 받으려면 알림 권한이 필요합니다.")
            .setPositiveButton("설정하기") { _, _ ->
                goToAppNotificationSettings()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun goToAppNotificationSettings() {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", packageName)
        }
        startActivity(intent)
    }

    private fun checkLoginAndRefreshTokenIfNeeded(onSuccess: () -> Unit) {
        val accessToken = prefs.getString(LoginActivity.KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(LoginActivity.KEY_REFRESH_TOKEN, null)
        val expiryTime = prefs.getLong(LoginActivity.KEY_TOKEN_EXPIRY, 0L)
        val now = System.currentTimeMillis()

        if (accessToken.isNullOrEmpty() || expiryTime == 0L || now > expiryTime) {
            if (!refreshToken.isNullOrEmpty()) {
                RetrofitClient.apiService.refreshAccessToken("Bearer $refreshToken")
                    .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                        override fun onResponse(call: Call<ApiResponse<LoginResponse>>, response: Response<ApiResponse<LoginResponse>>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                val loginData = response.body()?.data
                                loginData?.let {
                                    with(prefs.edit()) {
                                        putString(LoginActivity.KEY_ACCESS_TOKEN, it.token)
                                        putString(LoginActivity.KEY_REFRESH_TOKEN, it.refreshToken)
                                        putLong(LoginActivity.KEY_TOKEN_EXPIRY, now + 60 * 60 * 1000)
                                        apply()
                                    }
                                    onSuccess()
                                }
                            } else {
                                goToLogin()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                            goToLogin()
                        }
                    })
            } else {
                goToLogin()
            }
        } else {
            onSuccess()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        setResult(Activity.RESULT_OK)
        finish()
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                selectedDate = if (diffX > 0) selectedDate.minusMonths(1) else selectedDate.plusMonths(1)
                fetchAllSchedulesForMonth()
                updateCalendar()
                return true
            }
            return false
        }
    }
}

package com.example.mycalendar

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.mapper.ScheduleMapper
import com.example.mycalendar.model.*
import com.example.mycalendar.network.RetrofitClient
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.mycalendar.network.ApiService
import com.example.mycalendar.model.ScheduleRequest
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.model.ScheduleResponse
import com.example.mycalendar.model.AiSummaryResponse
import com.example.mycalendar.model.LoginResponse
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch



class MainActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: CalendarAdapter
    private lateinit var gestureDetector: GestureDetector
    private lateinit var prefs: SharedPreferences

    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    private var selectedDate: LocalDate = LocalDate.now()
    private var copiedFromScheduleId: Long? = null

    private val addScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchSchedulesForDate(selectedDate) // ✅ request 제거
        }
    }

    private val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchSchedulesForDate(selectedDate) // ✅ request 제거
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // new: RetrofitClient 초기화
        RetrofitClient.init(this) // new

        prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)

        toolbar = findViewById(R.id.toolbar)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)
        val addButton: ImageButton = findViewById(R.id.addButton)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        Log.d("MainActivity", "🧪 저장된 access_token: ${prefs.getString(LoginActivity.KEY_ACCESS_TOKEN, "없음")}")

        checkLoginAndRefreshTokenIfNeeded {
            setupCalendar()
            fetchSchedulesForDate(selectedDate) // ✅ FIXED
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

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
                    val schedulesForDay = getSchedulesForDate(LocalDate.now())
                    ScheduleListDialog(
                        selectedDate,
                        schedulesForDay.toMutableList(),
                        onDataChanged = { updateCalendar() },
                        onAddNewSchedule = { date -> openAddScheduleActivity(date) }
                    ).show(supportFragmentManager, "ScheduleListDialog")
                    true
                }
                R.id.nav_mypage -> {
                    startActivity(Intent(this, MypageActivity::class.java))
                    true
                }
                else -> false
            }.also { drawerLayout.closeDrawer(GravityCompat.START) }
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

                RetrofitClient.apiService.createSchedule(request)
                    .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                        override fun onResponse(
                            call: Call<ApiResponse<ScheduleResponse>>,
                            response: Response<ApiResponse<ScheduleResponse>>
                        ) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                fetchSchedulesForDate(selectedDate)
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

        findViewById<ImageButton>(R.id.aiButton).setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_summary, null)
            val summaryDateText = dialogView.findViewById<TextView>(R.id.summaryDateText)
            val summaryContentText = dialogView.findViewById<TextView>(R.id.summaryContentText)

            val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
            summaryDateText.text = "오늘 ${LocalDate.now().format(formatter)}"

            RetrofitClient.apiService.getAiSummary(LocalDate.now().toString())
                .enqueue(object : Callback<ApiResponse<AiSummaryResponse>> {
                    override fun onResponse(call: Call<ApiResponse<AiSummaryResponse>>, response: Response<ApiResponse<AiSummaryResponse>>) {
                        summaryContentText.text = response.body()?.data?.summaryText ?: "요약을 가져오지 못했습니다."
                    }

                    override fun onFailure(call: Call<ApiResponse<AiSummaryResponse>>, t: Throwable) {
                        summaryContentText.text = "서버 오류 발생: ${t.localizedMessage}"
                    }
                })

            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("닫기", null)
                .show()
        }

        // 딥링크 처리
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW && uri.scheme == "mycalendar" && uri.host == "schedule") {
                try {
                    val request = ScheduleRequest(
                        title = uri.getQueryParameter("title") ?: "제목 없음",
                        memo = uri.getQueryParameter("memo") ?: "",
                        location = uri.getQueryParameter("location") ?: "",
                        category = uri.getQueryParameter("category") ?: "",
                        scheduledDate = uri.getQueryParameter("start") ?: selectedDate.toString(),
                        startDate = uri.getQueryParameter("start") ?: selectedDate.toString(),
                        endDate = uri.getQueryParameter("end") ?: selectedDate.toString(),
                        startTime = "09:00",
                        endTime = "10:00",
                        allDay = false,
                        isConfirmed = true,
                        color = uri.getQueryParameter("color") ?: "#4285F4",
                        alarmOn = true,
                        copiedFromScheduleId = copiedFromScheduleId
                    )

                    RetrofitClient.apiService.createSchedule(request)
                        .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                            override fun onResponse(call: Call<ApiResponse<ScheduleResponse>>, response: Response<ApiResponse<ScheduleResponse>>) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    fetchSchedulesForDate(selectedDate)
                                    Toast.makeText(this@MainActivity, "공유된 일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()
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

    private fun setupCalendar() {
        adapter = CalendarAdapter(ArrayList(), schedules) { date ->
            if (selectedDate == date) {
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
            } else {
                selectedDate = date
                updateCalendar()
            }
        }

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = adapter

        gestureDetector = GestureDetector(this, SwipeGestureListener())
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
        adapter.dayList = generateDaysInMonth(YearMonth.from(selectedDate))
        adapter.selectedDate = selectedDate
        adapter.notifyDataSetChanged()
        updateScheduleHint(selectedDate)
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

                        // ✅ 수정: 특정 날짜의 일정만 업데이트
                        schedules[date] = mutableListOf()

                        list.forEach { schedule ->
                            val key = schedule.scheduledDate
                            if (schedules[key] == null) schedules[key] = mutableListOf()
                            schedules[key]?.add(schedule)
                        }

                        // 디버그 로그 추가
                        Log.d("MainActivity", "📅 날짜 ${date}의 일정 ${list.size}개 로드됨")
                        Log.d("MainActivity", "📋 전체 일정 맵 크기: ${schedules.size}")

                        updateCalendar()
                    } else{
                        Log.e("MainActivity", "❌ API 응답 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ScheduleResponse>>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "일정 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openAddScheduleActivity(date: LocalDate) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("selectedDate", date)
        addScheduleLauncher.launch(intent)
    }

    private fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        return schedules[date] ?: emptyList()
    }

    private fun generateDaysInMonth(yearMonth: YearMonth): ArrayList<LocalDate> {
        val dayList = ArrayList<LocalDate>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7

        for (i in 0 until dayOfWeekOfFirst) dayList.add(LocalDate.MIN)
        for (i in 1..yearMonth.lengthOfMonth()) dayList.add(yearMonth.atDay(i))

        return dayList
    }

    private fun updateScheduleHint(date: LocalDate) {
        scheduleEditText.hint = date.format(DateTimeFormatter.ofPattern("M월 d일 일정 추가", Locale.KOREA))
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
                updateCalendar()
                return true
            }
            return false
        }
    }

    fun addSchedule(schedule: ScheduleRequest) {
        RetrofitClient.apiService.createSchedule(schedule)
            .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<ScheduleResponse>>,
                    response: Response<ApiResponse<ScheduleResponse>>
                ) {
                    if (response.isSuccessful) {
                        fetchSchedulesForDate(selectedDate) // UI 갱신
                    } else {
                        Log.e("MainActivity", "일정 추가 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                    Log.e("MainActivity", "일정 추가 예외 발생", t)
                }
            })
    }

    // 새로운: Schedule 타입을 받는 함수 (UI용 Schedule 처리)
    fun addSchedule(schedule: Schedule) {
        // UI에서 사용할 Schedule 처리
        schedules[selectedDate]?.add(schedule)  // 예시: 날짜에 맞는 일정 리스트에 추가
        updateCalendar() // 일정 추가 후 캘린더 갱신
    }

    fun removeSchedule(schedule: Schedule) {
        RetrofitClient.apiService.deleteSchedule(schedule.id ?: return)
            .enqueue(object : Callback<ApiResponse<Unit>> {
                override fun onResponse(
                    call: Call<ApiResponse<Unit>>,
                    response: Response<ApiResponse<Unit>>
                ) {
                    if (response.isSuccessful) {
                        selectedDate = schedule.startDate // <-- 이거 매우 중요
                        fetchSchedulesForDate(selectedDate)
                    } else {
                        Log.e("MainActivity", "삭제 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                    Log.e("MainActivity", "서버 오류", t)
                }
            })
    }

    fun openAddScheduleActivity(schedule: Schedule, isCopy: Boolean) {
        val intent = Intent(this, AddScheduleActivity::class.java).apply {
            putExtra("schedule", schedule)
            putExtra("isCopy", isCopy)
        }
        startActivity(intent)
    }
}

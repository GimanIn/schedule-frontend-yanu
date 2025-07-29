package com.example.mycalendar

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.model.LoginResponse
import com.example.mycalendar.model.Schedule
import com.example.mycalendar.model.ScheduleRequest
import com.example.mycalendar.model.ScheduleResponse
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
import java.util.Locale

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

    // ✅ [첫 번째 파일에서 가져온 정교한 권한 관리]
    private var isContentLoaded = false
    private var guidanceDialog: AlertDialog? = null

    // ✅ [두 번째 파일의 간단한 권한 처리 + 첫 번째의 정교한 관리 결합]
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 권한 요청 결과 처리는 onResume()에서 일괄 처리
        if (isGranted) {
            Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val addScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newSchedule = result.data?.getSerializableExtra("newSchedule") as? Schedule
            if (newSchedule != null) {
                addScheduleToMap(newSchedule)
            } else {
                fetchAllSchedulesForMonth()
            }
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

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        // ✅ [첫 번째 파일의 모듈화된 구조 적용]
        initializeUI(drawerLayout)
        askNotificationPermission()
    }

    // ✅ [첫 번째 파일의 onResume 권한 체크 적용]
    override fun onResume() {
        super.onResume()
        checkPermissionAndLoadContent()
    }

    // ✅ [첫 번째 파일의 모듈화된 UI 초기화]
    private fun initializeUI(drawerLayout: DrawerLayout) {
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
                    val selectedDate = LocalDate.now()
                    val schedulesForDay = getSchedulesForDate(selectedDate)
                    val dialog = ScheduleListDialog(
                        date = selectedDate,
                        dailySchedules = schedulesForDay.toMutableList(),
                        onDataChanged = { /* 일정 수정되었을 때 처리 */ },
                        onAddNewSchedule = { date ->
                            val intent = Intent(this, AddScheduleActivity::class.java)
                            intent.putExtra("selectedDate", date.toString())
                            startActivity(intent)
                        }
                    )
                    dialog.show(supportFragmentManager, "ScheduleListDialog")
                    true
                }
                R.id.nav_mypage -> {
                    startActivity(Intent(this, MypageActivity::class.java))
                    true
                }
                else -> false
            }.also { drawerLayout.closeDrawer(GravityCompat.START) }
        }

        // ✅ 사이드 메뉴 하단 텍스트뷰 클릭 처리 (이용약관 / 개인정보)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val textTerms = navigationView.findViewById<TextView>(R.id.textTerms)
        val textPrivacy = navigationView.findViewById<TextView>(R.id.textPrivacy)

        textTerms.setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        textPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<ImageView>(R.id.searchButton).setOnClickListener { openSearchActivity() }
        findViewById<ImageButton>(R.id.addButton).setOnClickListener { handleAddButtonClick() }

        // ✅ [두 번째 파일의 실제 AI 요약 기능 유지]
        findViewById<ImageButton>(R.id.aiButton)?.setOnClickListener {
            Log.d("AI_SUMMARY", "AI 요약 버튼 클릭됨")

            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("AI가 일정을 요약하고 있습니다...")
                .setCancelable(false)
                .create()

            loadingDialog.show()
            val today = LocalDate.now().toString()
            RetrofitClient.apiService.getAiSummary(today)
                .enqueue(object : Callback<AiSummaryResponse>  {
                    override fun onResponse(
                        call: Call<AiSummaryResponse>,
                        response: Response<AiSummaryResponse>
                    ) {
                        loadingDialog.dismiss()

                        Log.d("AI_SUMMARY", "HTTP 상태 코드: ${response.code()}")
                        Log.d("AI_SUMMARY", "응답 성공 여부: ${response.isSuccessful}")

                        if (response.isSuccessful) {
                            val summaryResponse = response.body()
                            Log.d("AI_SUMMARY", "응답 데이터: $summaryResponse")

                            val summaryText = summaryResponse?.summary

                            if (!summaryText.isNullOrBlank()) {
                                Log.d("AI_SUMMARY", "✅ AI 요약 성공!")
                                Log.d("AI_SUMMARY", "요약 내용 길이: ${summaryText.length}")
                                showAiSummaryDialog(summaryText)
                            } else {
                                Log.e("AI_SUMMARY", "❌ 요약 내용이 비어있음")
                                Toast.makeText(this@MainActivity, "AI 요약 내용이 비어있습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("AI_SUMMARY", "❌ HTTP 오류 - 코드: ${response.code()}")
                            Log.e("AI_SUMMARY", "에러 바디: $errorBody")

                            when (response.code()) {
                                401 -> Toast.makeText(this@MainActivity, "인증이 만료되었습니다.", Toast.LENGTH_SHORT).show()
                                404 -> Toast.makeText(this@MainActivity, "AI 요약 서비스를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                500 -> Toast.makeText(this@MainActivity, "서버 내부 오류입니다.", Toast.LENGTH_SHORT).show()
                                else -> Toast.makeText(this@MainActivity, "AI 요약 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<AiSummaryResponse>, t: Throwable) {
                        loadingDialog.dismiss()
                        Log.e("AI_SUMMARY", "❌ 네트워크 오류", t)

                        val errorMessage = when {
                            t.message?.contains("timeout") == true -> "서버 응답 시간 초과"
                            t.message?.contains("connect") == true -> "서버 연결 실패"
                            else -> "네트워크 오류: ${t.localizedMessage}"
                        }

                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
        }
        // ✅ 클래스 멤버 함수로 올바르게 정의


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

    // ✅ [첫 번째 파일의 정교한 권한 체크 로직]
    private fun checkPermissionAndLoadContent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우
                guidanceDialog?.dismiss()
                if (!isContentLoaded) {
                    loadMainContent()
                }
            } else {
                // 권한이 거부된 경우 - 두 번째 파일의 유연한 접근 방식 적용
                if (!isContentLoaded) {
                    // ✅ 권한이 없어도 앱은 사용할 수 있도록 함 (두 번째 파일의 장점)
                    loadMainContent()
                    // 단, 권한 안내는 표시
                    showPermissionGuidanceDialog()
                }
            }
        } else {
            // 안드로이드 13 미만은 설치 시 자동 권한 부여
            if (!isContentLoaded) {
                loadMainContent()
            }
        }
    }

    // ✅ [첫 번째 파일의 콘텐츠 로딩 분리]
    private fun loadMainContent() {
        checkLoginAndRefreshTokenIfNeeded {
            setupCalendar()
            fetchAllSchedulesForMonth()
            isContentLoaded = true
        }
    }

    // ✅ [첫 번째 파일의 권한 안내 다이얼로그 - 수정: 강제성 완화]
    private fun showPermissionGuidanceDialog() {
        if (guidanceDialog != null && guidanceDialog!!.isShowing) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("알림 권한 권장")
        builder.setMessage("더 나은 사용 경험을 위해 알림 권한을 허용해주세요. 일정 알림을 받을 수 있습니다.")
        builder.setPositiveButton("설정으로 이동") { _, _ ->
            goToAppNotificationSettings()
        }
        // ✅ 강제성 완화: "나중에" 옵션 추가
        builder.setNegativeButton("나중에") { _, _ ->
            // 다이얼로그 닫기만 함
        }
        builder.setCancelable(true) // 뒤로가기로도 닫을 수 있게 함

        guidanceDialog = builder.create()
        guidanceDialog?.show()
    }

    // ✅ [첫 번째 파일의 설정 화면 이동]
    private fun goToAppNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    // ✅ [두 번째 파일의 실제 AI 요약 다이얼로그]
    private fun showAiSummaryDialog(summaryText: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_summary, null)
        val summaryDateText = dialogView.findViewById<TextView>(R.id.summaryDateText)
        val summaryContentText = dialogView.findViewById<TextView>(R.id.summaryContentText)

        val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
        summaryDateText.text = "오늘 ${LocalDate.now().format(formatter)}"
        summaryContentText.text = summaryText

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("닫기", null)
            .show()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
                        addScheduleToMap(schedule)
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
        val yearMonth = YearMonth.from(selectedDate)
        val daysInMonth = generateDaysInMonth(yearMonth)

        calendarAdapter = CalendarAdapter(
            dayList = daysInMonth,
            schedules = schedules
        ) { date ->
            if (selectedDate == date) {
                val dailySchedules = schedules[date]
                if (dailySchedules.isNullOrEmpty()) {
                    openAddScheduleActivity(date)
                } else {
                    val dialog = ScheduleListDialog(
                        date,
                        dailySchedules.toMutableList(),
                        onDataChanged = { updateCalendar() },
                        onAddNewSchedule = { clickedDate ->
                            openAddScheduleActivity(clickedDate)
                        }
                    )
                    dialog.show(supportFragmentManager, "ScheduleListDialog")
                }
            } else {
                selectedDate = date
                updateCalendar()
            }
        }

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = calendarAdapter

        // ✅ 오늘 날짜 위치로 스크롤 이동
        val today = LocalDate.now()
        val todayIndex = daysInMonth.indexOf(today)
        if (todayIndex != -1) {
            calendarRecyclerView.scrollToPosition(todayIndex)
        }

        updateCalendar()
    }

    fun openEditScheduleActivity(schedule: Schedule, isCopy: Boolean) {
        val intent = Intent(this, AddScheduleActivity::class.java).apply {
            putExtra("scheduleToEdit", schedule)
            putExtra("isCopyMode", isCopy)
        }
        editScheduleLauncher.launch(intent)
    }

    private fun updateCalendar() {
        // ✅ [첫 번째 파일의 방어 코드 적용]
        if (!isContentLoaded) return

        toolbar.title = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREA))
        calendarAdapter.dayList = generateDaysInMonth(YearMonth.from(selectedDate))
        calendarAdapter.selectedDate = selectedDate
        calendarAdapter.notifyDataSetChanged()
        updateScheduleHint(selectedDate)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        updateCalendar()
    }

    private fun handleIntent(intent: Intent) {
        val y = intent.getIntExtra("targetYear", LocalDate.now().year)
        val m = intent.getIntExtra("targetMonth", LocalDate.now().monthValue)
        selectedDate = LocalDate.of(y, m, 1)
    }

    private fun openAddScheduleActivity(date: LocalDate) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("selectedDate", date)
        addScheduleLauncher.launch(intent)
    }

    val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("EDIT", "=== 결과: ${result.resultCode} ===")
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val updatedSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getSerializableExtra("updatedSchedule", Schedule::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getSerializableExtra("updatedSchedule") as? Schedule
            }

            if (updatedSchedule != null) {
                Log.d("EDIT", "수정됨: ${updatedSchedule.title}")
                removeSchedule(updatedSchedule)
                addScheduleToMap(updatedSchedule)
            } else {
                val copiedSchedule =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        data?.getSerializableExtra("newSchedule", Schedule::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        data?.getSerializableExtra("newSchedule") as? Schedule
                    }
                if (copiedSchedule != null) {
                    Log.d("EDIT", "복사됨: ${copiedSchedule.title}")
                    addSchedule(copiedSchedule)
                }
            }
        }
    }

    fun removeSchedule(scheduleToRemove: Schedule) {
        val entries = schedules.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            val scheduleList = entry.value

            scheduleList.removeAll {
                (scheduleToRemove.id != null && it.id == scheduleToRemove.id) ||
                        (scheduleToRemove.id == null &&
                                it.title == scheduleToRemove.title &&
                                it.startDate == scheduleToRemove.startDate &&
                                it.startTime == scheduleToRemove.startTime)
            }

            if (scheduleList.isEmpty()) {
                entries.remove()
            }
        }
        updateCalendar()
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
        val title = scheduleEditText.text.toString().trim()  // trim() 추가
        Log.d("ADD_BUTTON", "=== 추가 버튼 클릭 ===")
        Log.d("ADD_BUTTON", "입력된 제목: '$title'")
        Log.d("ADD_BUTTON", "선택된 날짜: $selectedDate")

        if (title.isNotBlank()) {
            Log.d("ADD_BUTTON", "제목이 있음 - API 요청 생성")

            // ✅ 현재 시간 기반으로 기본 시간 설정 (선택사항)
            val currentTime = LocalTime.now()
            val startTime = currentTime.withMinute(0).format(DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = currentTime.plusHours(1).withMinute(0).format(DateTimeFormatter.ofPattern("HH:mm"))

            val request = ScheduleRequest(
                title = title,
                memo = "",
                category = "",
                location = "",
                scheduledDate = selectedDate.toString(),
                startDate = selectedDate.toString(),
                endDate = selectedDate.toString(),
                startTime = startTime,  // 또는 그냥 "09:00" 유지
                endTime = endTime,      // 또는 그냥 "12:00" 유지
                allDay = false,
                isConfirmed = true,
                color = "blue",
                alarmOn = true,
                copiedFromScheduleId = copiedFromScheduleId
            )

            Log.d("ADD_BUTTON", "생성된 요청 객체: $request")
            Log.d("ADD_BUTTON", "시간 설정: $startTime ~ $endTime")  // 시간 로깅 추가

            addSchedule(request)
            scheduleEditText.text.clear()
            Log.d("ADD_BUTTON", "입력창 정리 완료")
        } else {
            Log.d("ADD_BUTTON", "제목이 비어있음 - AddScheduleActivity 열기")
            openAddScheduleActivity(selectedDate)
        }
    }

    fun getSchedulesForDate(date: LocalDate): List<Schedule> {
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

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffX = e2.x - e1.x
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    selectedDate = selectedDate.minusMonths(1)
                    updateCalendar()
                    fetchAllSchedulesForMonth()
                } else {
                    selectedDate = selectedDate.plusMonths(1)
                    updateCalendar()
                    fetchAllSchedulesForMonth()
                }
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }
}
package com.example.mycalendar

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

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

    // ✅ [추가] 알림 권한 요청 결과를 처리하는 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
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
                addScheduleToMap(newSchedule) // ✅ UI에 바로 반영
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
                    val selectedDate = LocalDate.now() // 또는 선택된 날짜

                    val schedulesForDay = getSchedulesForDate(selectedDate) // 해당 날짜의 일정 리스트

                    val dialog = ScheduleListDialog(
                        date = selectedDate,
                        dailySchedules = schedulesForDay.toMutableList(), // MutableList<Schedule>
                        onDataChanged = {
                            // 일정 수정되었을 때 처리
                        },
                        onAddNewSchedule = { date ->
                            // 일정 추가 화면 이동 등
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
                R.id.nav_settings -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            //권한이 없을 경우 → 다이얼로그로 유도
                            showNotificationDialog()
                        } else {
                            // 이미 권한 있음 → 설정 화면으로 이동
                            goToAppNotificationSettings()
                        }
                    } else {
                        goToAppNotificationSettings()
                    }
                    true
                }
                else -> false
            }.also { drawerLayout.closeDrawer(GravityCompat.START) }
        }

        searchButton.setOnClickListener {
            openSearchActivity()
        }
        addButton.setOnClickListener { handleAddButtonClick() }

        // ✅ AI 요약 버튼 - 백엔드 연동
        findViewById<ImageButton>(R.id.aiButton)?.setOnClickListener {
            Log.d("AI_SUMMARY", "AI 요약 버튼 클릭됨")

            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("AI가 일정을 요약하고 있습니다...")
                .setCancelable(false)
                .create()

            loadingDialog.show()
            val today = LocalDate.now().toString()
            RetrofitClient.apiService.getAiSummary(today)
                .enqueue(object : Callback<AiSummaryResponse> {
                    // ✅ 메서드 시그니처 수정: Call<AiSummaryResponse>, Response<AiSummaryResponse>
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

                            if (summaryResponse != null && !summaryResponse.summary.isNullOrBlank()) {
                                Log.d("AI_SUMMARY", "✅ AI 요약 성공!")
                                Log.d("AI_SUMMARY", "요약 내용 길이: ${summaryResponse.summary.length}")
                                showAiSummaryDialog(summaryResponse.summary)
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

                    // ✅ 메서드 시그니처 수정: Call<AiSummaryResponse>
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


        gestureDetector = GestureDetector(this, SwipeGestureListener())

        calendarRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        // ✅ [추가] onCreate가 끝날 때 알림 권한을 요청합니다.
        askNotificationPermission()
    }

    // ✅ AI 요약 다이얼로그를 표시하는 메서드
    private fun showAiSummaryDialog(summaryText: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_summary, null)
        val summaryDateText = dialogView.findViewById<TextView>(R.id.summaryDateText)
        val summaryContentText = dialogView.findViewById<TextView>(R.id.summaryContentText)

        // 현재 날짜를 표시
        val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
        summaryDateText.text = "오늘 ${LocalDate.now().format(formatter)}"

        // 백엔드에서 받은 AI 요약 내용을 표시
        summaryContentText.text = summaryText

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("닫기", null)
            .show()
    }

    // ✅ [추가] 알림 권한을 요청하는 함수
    private fun askNotificationPermission() {
        // 안드로이드 13 (Tiramisu, API 33) 이상인지 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 권한이 이미 부여되었는지 확인
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // 이미 권한이 있으면 아무것도 하지 않음
                Log.d("Permission", "알림 권한이 이미 허용되어 있습니다.")
            } else {
                // 권한이 없다면, 사용자에게 권한 요청 대화상자를 띄웁니다.
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
                        addScheduleToMap(schedule)  // ✅ startDate ~ endDate 사이 날짜에 모두 저장
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

        // 1. 우리가 만든 제스처 리스너를 사용하여 제스처 감지기를 생성합니다.
        var gestureDetector: GestureDetector
        gestureDetector = GestureDetector(this, SwipeGestureListener())

        // 2. 캘린더(RecyclerView)의 터치 이벤트를 제스처 감지기가 처리하도록 설정합니다.
        // 이 방법은 클릭과 스와이프를 모두 온전히 지원합니다.

        // --- 여기까지 추가 ---
        updateCalendar() // 앱 실행 시 첫 화면 로드
    }

    // --- 👇 2. AddScheduleActivity를 '수정 모드'로 여는 함수를 추가합니다. ---
    fun openEditScheduleActivity(schedule: Schedule, isCopy: Boolean) {
        val intent = Intent(this, AddScheduleActivity::class.java).apply {
            putExtra("scheduleToEdit", schedule)
            putExtra("isCopyMode", isCopy)
        }
        editScheduleLauncher.launch(intent)
    }

    private fun updateCalendar() {
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

    // --- 👇 1. 수정 전용 결과 처리기를 새로 추가합니다. ---
    val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("EDIT", "=== 결과: ${result.resultCode} ===")
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
                Log.d("EDIT", "수정됨: ${updatedSchedule.title}")
                // 수정된 일정이 있다면 -> 기존 것 삭제 후 새로 추가
                removeSchedule(updatedSchedule)
                addScheduleToMap(updatedSchedule)
            } else {
                // 2. "updatedSchedule"가 없다면, "newSchedule" 키로 복사된 새 일정이 있는지 확인
                val copiedSchedule =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        data?.getSerializableExtra("newSchedule", Schedule::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        data?.getSerializableExtra("newSchedule") as? Schedule
                    }
                if (copiedSchedule != null) {
                    Log.d("EDIT", "복사됨: ${copiedSchedule.title}")
                    // 복사된 새 일정이 있다면 -> 그냥 추가
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
                // ID가 있으면 ID 기준, 없으면 날짜/시간/제목 비교
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

    // MainActivity의 handleAddButtonClick 메서드를 이것으로 교체

    private fun handleAddButtonClick() {
        val title = scheduleEditText.text.toString()
        Log.d("ADD_BUTTON", "=== 추가 버튼 클릭 ===")
        Log.d("ADD_BUTTON", "입력된 제목: '$title'")
        Log.d("ADD_BUTTON", "선택된 날짜: $selectedDate")

        if (title.isNotBlank()) {
            Log.d("ADD_BUTTON", "제목이 있음 - API 요청 생성")

            // ✅ 시간 차이를 더 크게 설정하여 검증 통과
            val request = ScheduleRequest(
                title = title,
                memo = "",
                category = "",
                location = "",
                scheduledDate = selectedDate.toString(),
                startDate = selectedDate.toString(),
                endDate = selectedDate.toString(),
                startTime = "09:00",                     // 시작: 09:00
                endTime = "10:00",                       // 종료: 12:00 (3시간 차이)
                allDay = false,                          // ✅ 명시적으로 false
                isConfirmed = true,
                color = "blue",
                alarmOn = true,
                copiedFromScheduleId = copiedFromScheduleId
            )

            Log.d("ADD_BUTTON", "생성된 요청 객체: $request")
            Log.d("ADD_BUTTON", "startTime: ${request.startTime}")
            Log.d("ADD_BUTTON", "endTime: ${request.endTime}")
            Log.d("ADD_BUTTON", "시간 차이: ${request.startTime} -> ${request.endTime}")
            Log.d("ADD_BUTTON", "API 호출 시작...")

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
                    fetchAllSchedulesForMonth()
                } else {
                    // 왼쪽으로 스와이프 -> 다음 달
                    selectedDate = selectedDate.plusMonths(1)
                    updateCalendar()
                    fetchAllSchedulesForMonth()
                }
                return true // 이벤트 처리를 완료했음을 알림
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }
}
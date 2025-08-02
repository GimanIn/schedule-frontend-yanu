package com.example.mycalendar

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import com.example.mycalendar.service.AlarmSync

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
import com.example.mycalendar.model.HolidayResponse
import com.example.mycalendar.network.HolidayApiService
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    // 🎈 추가: 공휴일 데이터를 담을 Map
    private var holidayMap = mapOf<LocalDate, String>()

    // 🎈 추가: 공공데이터포털 API 서비스를 사용하기 위한 Retrofit 인스턴스
    private val holidayApiService: HolidayApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HolidayApiService::class.java)
    }

    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    private var selectedDate: LocalDate = LocalDate.now()
    private var copiedFromScheduleId: Long? = null

    // ✅ [첫 번째 파일에서 가져온 정교한 권한 관리]
    private var isContentLoaded = false
    private var guidanceDialog: AlertDialog? = null

    // ✅ 🚨 알람 관련 개선 변수들 추가 🚨
    private lateinit var alarmPrefs: SharedPreferences
    private var lastAlarmScheduleDate: String? = null
    private var isAlarmScheduling = false  // 알람 스케줄링 중복 방지

    // ✅ 딥링크 처리용 변수 (간소화)
    private var pendingDeepLinkScheduleId: Long? = null

    // ✅ [두 번째 파일의 간단한 권한 처리 + 첫 번째의 정교한 관리 결합]
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
                addScheduleToMap(newSchedule)
            } else {
                fetchAllSchedulesForMonth()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DeepLink", "onCreate 실행 - URI: ${intent?.data}")
        AlarmSync.fetchTodayAlarmsAndNotify(this)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RetrofitClient.init(this)
        prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)

        // ✅ 🚨 알람 전용 SharedPreferences 초기화 🚨
        alarmPrefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        lastAlarmScheduleDate = alarmPrefs.getString("last_scheduled_date", null)

        // MainActivity의 onCreate()에 임시로 추가
        Log.d("KEY_CHECK", "=== SharedPreferences 키 확인 ===")
        Log.d("KEY_CHECK", "PREFS_NAME: ${LoginActivity.PREFS_NAME}")
        Log.d("KEY_CHECK", "KEY_ACCESS_TOKEN: ${LoginActivity.KEY_ACCESS_TOKEN}")
        Log.d("KEY_CHECK", "실제 저장된 토큰: ${prefs.getString(LoginActivity.KEY_ACCESS_TOKEN, "없음")?.take(10)}...")
        Log.d("KEY_CHECK", "모든 키 목록: ${prefs.all.keys.joinToString(", ")}")

        toolbar = findViewById(R.id.toolbar)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)

        initializeUI()
        askNotificationPermission()

        // ✅ 딥링크 처리 (onCreate 시점)
        handleDeepLink(intent)

        checkPermissionAndLoadContent()
        AlarmSync.syncAlarms(this)


        requestAlarmPermission()

        // 테스트 액티비티 자동 시작 (필요시 제거)
        // startActivity(Intent(this, AlarmTestActivity::class.java))
    }
     fun shareSchedule(scheduleId: Long, receiverId: Long) {
        RetrofitClient.apiService.shareSchedule(scheduleId, receiverId)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(
                    call: Call<ApiResponse<String>>,
                    response: Response<ApiResponse<String>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@MainActivity, "✅ 일정이 성공적으로 공유되었습니다", Toast.LENGTH_SHORT).show()
                        Log.d("공유", "공유 성공: ${response.body()?.data}")
                    } else {
                        Toast.makeText(this@MainActivity, "❌ 공유 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e("공유", "응답 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "❌ 네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("공유", "네트워크 오류", t)
                }
            })
    }

    fun shareScheduleViaApi(schedule: Schedule) {
        val scheduleId = schedule.id ?: return
        val receiverId = 123L // 예시용 ID, 실제 동작에서는 로그인 사용자 또는 선택된 사용자로 설정해야 함

        RetrofitClient.apiService.shareSchedule(scheduleId, receiverId)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    if (response.isSuccessful) {
                        Log.d("Share", "공유 성공: ${response.body()?.data}")
                    } else {
                        Log.e("Share", "공유 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Log.e("Share", "네트워크 오류: ${t.message}")
                }
            })
    }





    // MainActivity의 onCreate() 또는 onResume()에 추가
    private fun requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // 권한 요청 다이얼로그 표시
                AlertDialog.Builder(this)
                    .setTitle("알람 권한 필요")
                    .setMessage("정확한 시간에 알림을 받으려면 알람 권한을 허용해주세요.")
                    .setPositiveButton("설정하기") { _, _ ->
                        try {
                            // 이 코드가 실행되면 앱이 알람 권한 목록에 나타남!
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            startActivity(intent)
                        } catch (e: Exception) {
                            // 대체 방법
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", packageName, null)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("나중에", null)
                    .show()
            }
        }
    }



    // ✅ 🚨 NEW: onResume 알람 로직 완전 개선 🚨
    override fun onResume() {
        super.onResume()

        // 권한 상태 재확인
        checkPermissionAndLoadContent()

        // 하루가 바뀌었는지 체크 후 알람 재설정
        checkAndScheduleAlarmsIfNeeded()
    }

    // ✅ 새로운 인텐트 처리 (앱이 실행 중일 때)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("DeepLink", "🔗 onNewIntent 호출됨: ${intent?.data}")

        // 새로운 딥링크 처리
        handleDeepLink(intent)
    }

    // ✅ 통합된 딥링크 처리 함수
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        Log.d("DeepLink", "🔍 딥링크 데이터: $data")

        if (data != null && data.scheme == "mycalendar") {
            when (data.host) {
                "schedule" -> {
                    val scheduleIdStr = data.getQueryParameter("id")
                    Log.d("DeepLink", "📅 스케줄 ID 문자열: $scheduleIdStr")

                    if (!scheduleIdStr.isNullOrEmpty()) {
                        try {
                            val scheduleId = scheduleIdStr.toLong()
                            Log.d("DeepLink", "📅 파싱된 스케줄 ID: $scheduleId")

                            // 즉시 처리하거나 대기
                            if (isContentLoaded) {
                                navigateToSchedule(scheduleId)
                            } else {
                                pendingDeepLinkScheduleId = scheduleId
                                Log.d("DeepLink", "📅 딥링크 대기 중: $scheduleId")
                            }
                        } catch (e: NumberFormatException) {
                            Log.e("DeepLink", "잘못된 스케줄 ID: $scheduleIdStr")
                            Toast.makeText(this, "잘못된 스케줄 ID입니다", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w("DeepLink", "스케줄 ID가 없습니다")
                        Toast.makeText(this, "스케줄 ID가 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    Log.w("DeepLink", "알 수 없는 딥링크: ${data.host}")
                }
            }
        }
    }

    // ✅ 스케줄로 이동하는 함수
    private fun navigateToSchedule(scheduleId: Long) {
        Log.d("DeepLink", "🚀 스케줄로 이동: ID=$scheduleId")

        // 로딩 다이얼로그
        val loadingDialog = android.app.ProgressDialog(this).apply {
            setMessage("스케줄을 불러오는 중...")
            setCancelable(false)
            show()
        }

        // 스케줄 상세 조회
        RetrofitClient.apiService.getPublicSchedule(scheduleId)
            .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<ScheduleResponse>>,
                response: Response<ApiResponse<ScheduleResponse>>
            ) {
                loadingDialog.dismiss()

                if (response.isSuccessful && response.body()?.success == true) {
                    val scheduleResponse = response.body()?.data
                    if (scheduleResponse != null) {
                        val schedule = ScheduleMapper.toSchedule(scheduleResponse)
                        Log.d("DeepLink", "✅ 스케줄 로드 성공: ${schedule.title}")
                        showScheduleDetail(schedule)
                    } else {
                        showError("스케줄 데이터가 없습니다")
                    }
                } else {
                    showError("스케줄을 찾을 수 없습니다 (${response.code()})")
                }
            }

            override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                loadingDialog.dismiss()
                Log.e("DeepLink", "네트워크 오류: ${t.message}")
                showError("네트워크 오류가 발생했습니다")
            }
        })
    }

    // ✅ 클래스 상단에 추가 (MainActivity 안에 변수 선언)
    var isShowingImportDialog = false

    // ✅ 중복 방지 로직 포함
    private fun showScheduleDetail(schedule: Schedule) {
        if (isShowingImportDialog) return // 이미 다이얼로그 띄운 상태면 실행 안 함
        isShowingImportDialog = true

        Log.d("DeepLink", "✅ ScheduleImportFragment 표시: ${schedule.title}")

        val importFragment = ScheduleImportFragment.newInstance(schedule)

        importFragment.setOnScheduleImportListener(object : OnScheduleImportListener {
            override fun onScheduleImport(importedSchedule: Schedule) {
                importScheduleToMyCalendar(importedSchedule)
                isShowingImportDialog = false // 다이얼로그 닫히면 초기화
            }
        })

        importFragment.show(supportFragmentManager, "ScheduleImportFragment")
    }

    // ✅ 캘린더 날짜로 이동
    private fun navigateToCalendarDate(date: LocalDate) {
        selectedDate = date
        updateCalendar()
        fetchAllSchedulesForMonth()
        Log.d("DeepLink", "📅 캘린더 날짜 이동: $date")
    }

    // ✅ 내 캘린더에 일정 추가
    private fun importScheduleToMyCalendar(schedule: Schedule) {
        val isAllDayEvent = schedule.startTime == null

        val request = ScheduleRequest(
            title = schedule.title,
            memo = schedule.memo,
            location = schedule.location,
            category = schedule.category,
            scheduledDate = schedule.scheduledDate.toString(),
            startDate = schedule.startDate.toString(),
            endDate = schedule.endDate.toString(),
            startTime = if (isAllDayEvent) "00:00" else schedule.startTime.toString(),
            endTime = if (isAllDayEvent) "23:59" else schedule.endTime.toString(),
            allDay = isAllDayEvent,
            isConfirmed = true,
            color = String.format("#%06X", 0xFFFFFF and schedule.color),
            alarmOn = schedule.alarmOn,
            copiedFromScheduleId = schedule.id
        )

        addSchedule(request)
        Toast.makeText(this, "일정이 내 캘린더에 추가되었습니다", Toast.LENGTH_SHORT).show()
    }

    // ✅ 에러 표시
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ✅ 🚨 NEW: 하루 변경 체크 후 알람 스케줄링 🚨
    private fun checkAndScheduleAlarmsIfNeeded() {
        val today = LocalDate.now().toString()

        // 같은 날이면 중복 실행 방지
        if (lastAlarmScheduleDate == today) {
            Log.d("AlarmSchedule", "오늘 이미 알람 스케줄링 완료: $today")
            return
        }

        // 새로운 날이면 알람 스케줄링 실행
        Log.d("AlarmSchedule", "새로운 날 감지: $lastAlarmScheduleDate -> $today")
        scheduleTodayAlarms()
    }

    // ✅ 🚨 NEW: 완전히 개선된 알람 스케줄링 로직 🚨
    private fun scheduleTodayAlarms() {
        // 중복 실행 방지
        if (isAlarmScheduling) {
            Log.d("AlarmSchedule", "이미 알람 스케줄링 중입니다.")
            return
        }

        isAlarmScheduling = true

        // 1. 권한 체크 먼저
        if (!checkAlarmPermissions()) {
            isAlarmScheduling = false
            return
        }

        // 2. 기존 알람들 정리
        cancelPreviousAlarms()

        // 3. 서버에서 오늘 알람 조회 및 등록
        RetrofitClient.apiService.getTodayAlarms().enqueue(object : Callback<TodayAlarmResponse> {
            override fun onResponse(
                call: Call<TodayAlarmResponse>,
                response: Response<TodayAlarmResponse>
            ) {
                isAlarmScheduling = false

                if (response.isSuccessful && response.body()?.success == true) {
                    val todayAlarmResponse = response.body()!!
                    val alarmDtos = todayAlarmResponse.data.alarms  // 🔧 한 번만 선언

                    if (alarmDtos.isNotEmpty()) {
                        Log.d("AlarmSchedule", "서버에서 ${alarmDtos.size}개 알람 조회됨")

                        val successfulAlarmIds = mutableListOf<Long>()
                        var successCount = 0

                        for (alarmDto in alarmDtos) {
                            val alarm = Alarm(
                                id = alarmDto.id ?: 0L,
                                scheduleId = alarmDto.scheduleId ?: 0L,
                                scheduleTitle = alarmDto.scheduleTitle ?: "",
                                alarmTime = alarmDto.alarmTime ?: "",
                                message = alarmDto.message ?: "",
                                isSent = alarmDto.isSent ?: false,
                                sentAt = alarmDto.sentAt,
                                createdAt = alarmDto.createdAt ?: ""
                            )
                            if (AlarmManagerUtil.scheduleAlarm(this@MainActivity, alarm)) {
                                successCount++
                                successfulAlarmIds.add(alarm.id)  // 🔧 !! 제거
                            }
                        }

                        saveScheduledAlarmIds(successfulAlarmIds)

                        val today = LocalDate.now().toString()
                        alarmPrefs.edit()
                            .putString("last_scheduled_date", today)
                            .apply()
                        lastAlarmScheduleDate = today

                        Log.d("AlarmSchedule", "✅ 오늘 알람 등록 완료: ${successCount}/${alarmDtos.size}")  // 🔧 수정

                        if (successCount > 0) {
                            Toast.makeText(this@MainActivity,
                                "오늘 알람 ${successCount}개가 설정되었습니다.",
                                Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("AlarmSchedule", "오늘 예정된 알람이 없습니다.")
                    }
                } else {
                    Log.e("AlarmSchedule", "❌ 알람 조회 실패: ${response.code()}")
                    Toast.makeText(this@MainActivity,
                        "알람 설정 중 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TodayAlarmResponse>, t: Throwable) {
                isAlarmScheduling = false
                Log.e("AlarmFetch", "❌ 알람 불러오기 실패: ${t.message}")
                Toast.makeText(this@MainActivity,
                    "네트워크 오류로 알람 설정에 실패했습니다.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ✅ 🚨 NEW: 알람 권한 종합 체크 🚨
    private fun checkAlarmPermissions(): Boolean {
        // 1. 안드로이드 12+ 정확한 알람 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmSchedule", "정확한 알람 권한이 없습니다")
                showAlarmPermissionDialog()
                return false
            }
        }

        // 2. 안드로이드 13+ 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w("AlarmSchedule", "알림 권한이 없습니다")
                return false
            }
        }

        return true
    }

    // ✅ 🚨 NEW: 알람 권한 요청 다이얼로그 🚨
    private fun showAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("알람 권한 필요")
            .setMessage("정확한 시간에 알림을 받으려면 알람 권한이 필요합니다.")
            .setPositiveButton("설정하기") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e: Exception) {
                        Log.e("AlarmPermission", "알람 권한 설정 화면 열기 실패", e)
                        Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("나중에", null)
            .show()
    }

    // ✅ 🚨 NEW: 이전 알람들 정리 로직 🚨
    private fun cancelPreviousAlarms() {
        try {
            val alarmIds = alarmPrefs.getStringSet("scheduled_alarm_ids", emptySet()) ?: emptySet()

            if (alarmIds.isNotEmpty()) {
                Log.d("AlarmCancel", "이전 알람 ${alarmIds.size}개 취소 시작")

                var cancelCount = 0
                alarmIds.forEach { idStr ->
                    val alarmId = idStr.toLongOrNull()
                    if (alarmId != null) {
                        if (AlarmManagerUtil.cancelAlarm(this, alarmId)) {
                            cancelCount++
                        }
                    }
                }

                Log.d("AlarmCancel", "✅ 이전 알람 취소 완료: ${cancelCount}/${alarmIds.size}")
            }

            // 정리 후 저장소 초기화
            alarmPrefs.edit().remove("scheduled_alarm_ids").apply()

        } catch (e: Exception) {
            Log.e("AlarmCancel", "이전 알람 취소 중 오류", e)
        }
    }

    // ✅ 🚨 NEW: 성공한 알람 ID들 저장 🚨
    private fun saveScheduledAlarmIds(alarmIds: List<Long>) {
        try {
            val idSet = alarmIds.map { it.toString() }.toSet()
            alarmPrefs.edit().putStringSet("scheduled_alarm_ids", idSet).apply()
            Log.d("AlarmSave", "✅ 알람 ID ${alarmIds.size}개 저장 완료")
        } catch (e: Exception) {
            Log.e("AlarmSave", "알람 ID 저장 실패", e)
        }
    }

    // ✅ UI 초기화 (파라미터 제거)
    private fun initializeUI() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
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

    // ✅ 콘텐츠 로딩 완료 후 대기 중인 딥링크 처리
    private fun loadMainContent() {
        checkLoginAndRefreshTokenIfNeeded {
            setupCalendar()
            fetchAllSchedulesForMonth()
            loadHolidaysForMonth()
            isContentLoaded = true

            // 대기 중인 딥링크 처리
            pendingDeepLinkScheduleId?.let { scheduleId ->
                Log.d("DeepLink", "📅 대기 중인 딥링크 처리: $scheduleId")
                navigateToSchedule(scheduleId)
                pendingDeepLinkScheduleId = null
            }
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
            schedules = schedules,
            holidayMap = this.holidayMap
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

        // 🧪 공휴일 데이터 테스트 로그 추가
        Log.d("HolidayTest", "현재 holidayMap 크기: ${holidayMap.size}")
        holidayMap.forEach { (date, name) ->
            Log.d("HolidayTest", "공휴일: $date -> $name")
        }

        toolbar.title = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREA))
        calendarAdapter.dayList = generateDaysInMonth(YearMonth.from(selectedDate))
        calendarAdapter.selectedDate = selectedDate

        // 🎌 이 부분 추가: CalendarAdapter에 공휴일 데이터 업데이트
        calendarAdapter.updateHolidayMap(holidayMap)

        calendarAdapter.notifyDataSetChanged()
        updateScheduleHint(selectedDate)
    }

    private fun loadHolidaysForMonth() {
        val year = selectedDate.year.toString()
        val month = String.format("%02d", selectedDate.monthValue)

        // 🎈 중요: 공공데이터포털에서 발급받은 '일반 인증키(Decoding)'를 사용해야 합니다.
        val serviceKey = "iKhl4kZB/Uuv4WeD9Dh1Mty25lohK3rO+CAi5UF6uUinCu9POa3mMWbwUnN3rFKW7l/fRbakWGlSdbJiXFALjQ=="

        holidayApiService.getHolidays(serviceKey, year, month).enqueue(object : Callback<HolidayResponse> {
            override fun onResponse(call: Call<HolidayResponse>, response: Response<HolidayResponse>) {
                if (response.isSuccessful) {
                    val items = response.body()?.response?.body?.items?.holidayItems ?: emptyList()
                    Log.d("HolidayAPI", "✅ API 응답 성공: ${items.size}개의 공휴일 수신")

                    // API 결과를 <LocalDate, String> 맵으로 변환
                    holidayMap = items.associate {
                        val dateStr = it.locdate.toString()
                        val holidayDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                        holidayDate to it.dateName
                    }

                    Log.d("HolidayAPI", "✅ ${year}년 ${month}월 공휴일 로딩 성공: ${holidayMap.size}개")
                    updateCalendar() // 공휴일 정보로 캘린더 UI 갱신

                } else {
                    Log.e("HolidayAPI", "❌ 공휴일 API 응답 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<HolidayResponse>, t: Throwable) {
                Log.e("HolidayAPI", "❌ 공휴일 API 호출 실패", t)
            }
        })
    }

    private fun openAddScheduleActivity(date: LocalDate) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("selectedDate", date)
        addScheduleLauncher.launch(intent)
    }

    // ✅ 3. Deprecated getSerializableExtra 수정
    val editScheduleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("EDIT", "=== 결과: ${result.resultCode} ===")
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

            // ✅ API 레벨에 따른 안전한 처리
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
                val copiedSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    // 서버에서 일정 삭제하는 새 메서드
    fun deleteScheduleFromServer(schedule: Schedule, onSuccess: () -> Unit) {
        if (schedule.id == null) {
            // 로컬 일정은 바로 삭제
            removeSchedule(schedule)
            onSuccess()
            return
        }

        // 서버에 삭제 요청
        RetrofitClient.apiService.deleteSchedule(schedule.id!!)
            .enqueue(object : Callback<ApiResponse<Unit>> {
                override fun onResponse(
                    call: Call<ApiResponse<Unit>>,
                    response: Response<ApiResponse<Unit>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // 서버 삭제 성공 시 로컬에서도 제거
                        removeSchedule(schedule)
                        onSuccess()
                        Toast.makeText(this@MainActivity, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "일정 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
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
                    // 🎈 추가
                    loadHolidaysForMonth()
                } else {
                    selectedDate = selectedDate.plusMonths(1)
                    updateCalendar()
                    fetchAllSchedulesForMonth()
                    // 🎈 추가
                    loadHolidaysForMonth()
                }
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    // ✅ 딥링크 관련 인터페이스
    interface OnScheduleCopiedListener {
        fun onScheduleCopied(schedule: Schedule)
    }
}
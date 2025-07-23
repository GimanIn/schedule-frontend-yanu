package com.example.mycalendar

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.os.Build
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import com.google.android.material.appbar.MaterialToolbar
import java.time.LocalDateTime
import androidx.appcompat.app.AlertDialog
import com.example.mycalendar.Schedule
import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mycalendar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var gestureDetector: androidx.core.view.GestureDetectorCompat
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var binding: ActivityMainBinding

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

        // ✅ 1. binding 먼저 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ 2. binding 사용
        // ✅ 날짜 리스트 생성
        val yearMonth = YearMonth.from(selectedDate)
        val daysInMonth = generateDaysInMonth(yearMonth)
        // ✅ adapter 생성
        calendarAdapter = CalendarAdapter(
            dayList = daysInMonth,
            schedules = schedules,
            onItemClicked = { date ->
                selectedDate = date
                updateCalendar()
            }
        )
        binding.calendarRecyclerView.adapter = calendarAdapter

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

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

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
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }

        // 3. 버튼 리스너들을 설정합니다.
        searchButton.setOnClickListener {
            // 모든 일정을 하나의 리스트로 만듭니다.
            val allSchedules = schedules.values.flatten().distinctBy { it.id }

            val intent = Intent(this, SearchActivity::class.java).apply {
                // 직렬화 가능한 형태로 전달하기 위해 ArrayList로 변환
                putExtra("allSchedules", ArrayList(allSchedules))
            }
            startActivity(intent)
        }

        // 하단 바 '+' 버튼 클릭 리스너
        addButton.setOnClickListener {
            val scheduleTitle = scheduleEditText.text.toString()
            if (scheduleTitle.isNotEmpty()) {
                val newSchedule = Schedule(
                    title = scheduleTitle,
                    startDateTime = null,
                    endDateTime = null,
                    color = Color.parseColor("#4285F4"),
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

        val aiButton = findViewById<ImageButton>(R.id.aiButton)
        aiButton.setOnClickListener {
            // AlertDialog를 사용해 커스텀 뷰를 띄웁니다.
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_summary, null)
            val summaryDateText = dialogView.findViewById<TextView>(R.id.summaryDateText)
            val summaryContentText = dialogView.findViewById<TextView>(R.id.summaryContentText)

            // 현재 날짜를 표시
            val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
            summaryDateText.text = "오늘 ${LocalDate.now().format(formatter)}"

            // TODO: 여기에 나중에 백엔드로부터 AI 요약 내용을 받아와
            // summaryContentText.text에 설정하는 코드가 들어갑니다.

            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("닫기", null)
                .show()
        }

        setupCalendar() // 캘린더 초기 설정

        // --- 딥링크를 통해 앱이 실행되었는지 확인 ---
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null && uri.host == "mycalendar.example.com") {
                try {
                    val title = uri.getQueryParameter("title") ?: "제목 없음"
                    val startStr = uri.getQueryParameter("start")
                    val endStr = uri.getQueryParameter("end")
                    val colorStr = uri.getQueryParameter("color")
                    val category = uri.getQueryParameter("category") ?: ""
                    val location = uri.getQueryParameter("location") ?: ""
                    val memo = uri.getQueryParameter("memo") ?: ""

                    val newSchedule = Schedule(
                        title = title,
                        startDateTime = if (startStr != null) LocalDateTime.parse(startStr) else null,
                        endDateTime = if (endStr != null) LocalDateTime.parse(endStr) else null,
                        color = colorStr?.toInt() ?: Color.parseColor("#4285F4"),
                        category = category,
                        location = location,
                        memo = memo
                    )

                    addSchedule(newSchedule)
                    Toast.makeText(this, "공유된 일정이 추가되었습니다.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "일정을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        handleIntent(intent)
        updateCalendar()
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

    private fun generateDaysInMonth(yearMonth: YearMonth): ArrayList<LocalDate?> {
        val dayList = ArrayList<LocalDate?>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7

        // 빈 칸을 null로 채움
        for (i in 0 until dayOfWeekOfFirst) {
            dayList.add(null)
        }
        // 날짜 채우기
        for (i in 1..daysInMonth) {
            dayList.add(LocalDate.of(yearMonth.year, yearMonth.month, i))
        }
        return dayList
    }

    private fun updateCalendar() {
        val yearMonth = YearMonth.from(selectedDate)
        val dayList = generateDaysInMonth(yearMonth)

        calendarAdapter.dayList = dayList
        calendarAdapter.selectedDate = selectedDate
        calendarAdapter.notifyDataSetChanged()

        binding.monthYearText.text = "${selectedDate.year}년 ${selectedDate.monthValue}월"
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
                val copiedSchedule =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        Toast.makeText(
            this,
            "${startDate.dayOfMonth}일에 이제 ${count}개의 일정이 있습니다.",
            Toast.LENGTH_SHORT
        ).show()
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

    private fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        return schedules[date] ?: emptyList()
    }
    private fun goToAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "알림이 꺼져있어요. 설정 > 알림에서 직접 켜주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }
    // 설정 인텐트로 이동
    private fun showNotificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_notification_permission, null)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        val btnSettings = dialogView.findViewById<MaterialButton>(R.id.btnToSettings)
        btnSettings.setOnClickListener {
            dialog.dismiss()
            goToAppNotificationSettings() // ← 설정 인텐트로 이동
        }
    }
    // 상단 알림
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_id", // ← sendTestNotification()에서 쓰는 ID와 동일해야 함
                "일정 알림 채널",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "일정을 알려주는 푸시 알림 채널입니다."
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    private fun sendTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
                return // 권한 없으면 여기서 끝
            }
        }
        // 알림 인텐트 설정
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // 알림 구성
        val builder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("일정 알림")
            .setContentText("내일 스터디 일정이 있어요.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        // 알림 전송
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2001, builder.build())
    }
}


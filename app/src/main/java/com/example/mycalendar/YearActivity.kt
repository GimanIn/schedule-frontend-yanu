package com.example.mycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mycalendar.databinding.ActivityYearBinding
import java.time.LocalDate
import java.time.YearMonth
import android.graphics.Typeface
import android.widget.GridLayout
import androidx.core.view.setMargins
import android.content.res.Resources

class YearActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYearBinding
    private val schedules = mutableMapOf<LocalDate, MutableList<Schedule>>()
    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
    fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYearBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentYear = LocalDate.now().year
        val today = LocalDate.now()
        val cellSize = 18.dpToPx()
        val cellWidth = 2.dpToPx()
        val cellHeight = 100.dpToPx()

        // 1. 연도 텍스트 표시
        binding.textYear.text = "${currentYear}년"
        binding.textYear.setTextColor(
            if (currentYear == today.year) Color.RED else Color.BLACK
        )

        // 2. 연간 달력 동적으로 yearContainer에 추가
        val container = binding.yearContainer
        for (month in 1..12) {
            val monthLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                setOnClickListener {
                    val intent = Intent(this@YearActivity, MainActivity::class.java)
                    intent.putExtra("selectedMonth", month)
                    startActivity(intent)
                }
            }

            val title = TextView(this).apply {
                text = "${month}월"
                textSize = 14f
                setPadding(0,10,0,10)
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setOnClickListener {
                    val intent = Intent(this@YearActivity, MainActivity::class.java).apply {
                        putExtra("targetMonth", month)  // 1~12
                        putExtra("targetYear", currentYear)    // 현재 연도
                    }
                    startActivity(intent)
                }
            }

            val grid = GridLayout(this).apply {
                rowCount = 6
                columnCount = 7
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4.dpToPx(), 0, 2.dpToPx())
                }
            }

            val daysInMonth = YearMonth.of(currentYear, month).lengthOfMonth()
            val firstDayOfWeek = LocalDate.of(currentYear, month, 1).dayOfWeek.value % 7


            // 빈칸
            for (i in 0 until firstDayOfWeek) {
                val emptyView = TextView(this).apply {
                    text = ""
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellWidth
                        height = (cellHeight * 0.2).toInt()
                    }
                }
                grid.addView(emptyView)
            }

            // 날짜
            for (day in 1..daysInMonth) {
                val dayView = TextView(this).apply {
                    text = day.toString()
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 0)
                    setLineSpacing(0f, 1f)
                    setTextColor(
                        if (currentYear == today.year && month == today.monthValue && day == today.dayOfMonth)
                            Color.RED else Color.BLACK
                    )

                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellSize
                        height = cellSize
                        setMargins(0,6,0,6)
                    }
                }
                grid.addView(dayView)
            }

            monthLayout.addView(title)
            monthLayout.addView(grid)
            container.addView(monthLayout)
        }

        // 3. 툴바 햄버거 메뉴
        binding.toolbar.setNavigationOnClickListener {
            val popup = PopupMenu(this, binding.toolbar)
            popup.menuInflater.inflate(R.menu.drawer_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.nav_year -> {
                        startActivity(Intent(this, YearActivity::class.java)); true
                    }
                    R.id.nav_month -> {
                        startActivity(Intent(this, MainActivity::class.java)); true
                    }
                    R.id.nav_day -> {
                        val selectedDate = LocalDate.now()
                        val schedulesForDay = getSchedulesForDate(selectedDate)

                        val dialog = ScheduleListDialog(
                            date = selectedDate,
                            dailySchedules = schedulesForDay.toMutableList(),
                            onDataChanged = { },
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
                        startActivity(Intent(this, MypageActivity::class.java)); true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // 4. 검색 버튼
        binding.searchButton.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // 5. 일정 입력창
        binding.btnAddEvent.setOnClickListener {
            val text = binding.inputEvent.text.toString()
            if (text.isNotBlank()) {
                Toast.makeText(this, "일정 추가됨: $text", Toast.LENGTH_SHORT).show()
                binding.inputEvent.text.clear()
            } else {
                Toast.makeText(this, "일정을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        return schedules[date] ?: emptyList()
    }
}
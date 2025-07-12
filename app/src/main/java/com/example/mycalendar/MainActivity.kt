package com.example.mycalendar

import android.os.Bundle
import android.widget.EditText
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarGridView: GridView
    private lateinit var scheduleEditText: EditText
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var calendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        monthYearText = findViewById(R.id.monthYearText)
        calendarGridView = findViewById(R.id.calendarGridView)
        scheduleEditText = findViewById(R.id.scheduleEditText)

        calendar = Calendar.getInstance()

        updateCalendar()

        // GridView의 날짜 클릭 이벤트 처리
        calendarGridView.setOnItemClickListener { parent, view, position, id ->
            val selectedDay = parent.getItemAtPosition(position) as String
            if (selectedDay.isNotEmpty()) {
                // 선택 효과 적용
                calendarAdapter.setSelectedPosition(position)

                // 하단 EditText의 hint 텍스트 업데이트
                val monthFormat = SimpleDateFormat("M", Locale.KOREA)
                val currentMonth = monthFormat.format(calendar.time)
                scheduleEditText.hint = "${currentMonth}월 ${selectedDay}일 일정 추가"
            }
        }
    }

    private fun updateCalendar() {
        val sdf = SimpleDateFormat("yyyy년 MMMM", Locale.KOREA)
        monthYearText.text = sdf.format(calendar.time)

        val dayList = ArrayList<String>()
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfMonth = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1

        for (i in 0 until firstDayOfMonth) {
            dayList.add("")
        }

        val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            dayList.add(i.toString())
        }

        calendarAdapter = CalendarAdapter(this, dayList)
        calendarGridView.adapter = calendarAdapter
    }
}
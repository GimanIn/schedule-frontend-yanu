package com.example.mycalendar

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var scheduleEditText: EditText
    private lateinit var calendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소들 연결
        monthYearText = findViewById(R.id.monthYearText)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        scheduleEditText = findViewById(R.id.scheduleEditText)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val addButton: ImageButton = findViewById(R.id.addButton)

        calendar = Calendar.getInstance()
        updateCalendar()

        // 버튼 클릭 리스너 (기능은 비워둠)
        searchButton.setOnClickListener {
            Toast.makeText(this, "검색 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }
        addButton.setOnClickListener {
            Toast.makeText(this, "일정 추가 버튼 클릭됨", Toast.LENGTH_SHORT).show()
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

        // 어댑터를 생성할 때, 클릭 시 실행될 동작을 함께 전달
        val adapter = CalendarAdapter(dayList) { day, position ->
            // 하단 EditText의 hint 텍스트 업데이트
            val monthFormat = SimpleDateFormat("M", Locale.KOREA)
            val currentMonth = monthFormat.format(calendar.time)
            scheduleEditText.hint = "${currentMonth}월 ${day}일 일정 추가"
        }

        // RecyclerView 설정
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = adapter
    }
}
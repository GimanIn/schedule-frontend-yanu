package com.example.mycalendar


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.databinding.ActivityYearBinding
import com.example.mycalendar.databinding.ItemMonthCardBinding
import java.time.LocalDate
import java.time.YearMonth
import android.view.Gravity
import android.widget.TextView

class YearActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYearBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYearBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedYear = 2025
        val currentDate = LocalDate.now()

        // 연도 텍스트 색상 설정
        binding.textYear.text = "${selectedYear}년"
        binding.textYear.setTextColor(
            if (selectedYear == currentDate.year) Color.RED else Color.BLACK
        )

        binding.yearRecyclerView.adapter = MonthAdapter(selectedYear, currentDate) { month ->
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("selectedMonth", month)
            startActivity(intent)
        }
        binding.yearRecyclerView.layoutManager = GridLayoutManager(this, 3)
    }

    inner class MonthAdapter(
        private val year: Int,
        private val today: LocalDate,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

        private val months = (1..12).toList()

        inner class MonthViewHolder(val binding: ItemMonthCardBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(month: Int) {
                val monthName = "${month}월"
                binding.textMonth.text = monthName

                // 월 강조 색상
                binding.textMonth.setTextColor(
                    if (today.year == year && today.monthValue == month) Color.RED
                    else Color.BLACK
                )

                val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
                val firstDayOfWeek = LocalDate.of(year, month, 1).dayOfWeek.value % 7

                val gridLayout = binding.gridDays
                gridLayout.removeAllViews()  // 이전 내용 지우기

                // 빈칸 먼저 채우기 (1일 이전 칸)
                for (i in 0 until firstDayOfWeek) {
                    val emptyView = TextView(binding.root.context).apply {
                        text = ""
                        width = 80
                        height = 80
                    }
                    gridLayout.addView(emptyView)
                }

                // 날짜 넣기
                for (day in 1..daysInMonth) {
                    val dayView = TextView(binding.root.context).apply {
                        text = day.toString()
                        width = 100
                        height = 100
                        gravity = Gravity.CENTER
                        setPadding(8, 8, 8, 8)

                        if (today.year == year && today.monthValue == month && today.dayOfMonth == day) {
                            setTextColor(Color.RED)
                        } else {
                            setTextColor(Color.BLACK)
                        }
                    }
                    gridLayout.addView(dayView)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemMonthCardBinding.inflate(inflater, parent, false)
            return MonthViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
            holder.bind(months[position])
        }

        override fun getItemCount(): Int = months.size
    }
}
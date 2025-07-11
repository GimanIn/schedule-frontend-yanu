package com.example.mycalendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity // 👈 Gravity 임포트
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class CalendarAdapter(
    private val dayList: ArrayList<LocalDate>,
    private val schedules: Map<LocalDate, List<Schedule>>,
    private var selectedDate: LocalDate,
    private val onItemClicked: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.dayText)
        val scheduleText: TextView = itemView.findViewById(R.id.scheduleText)
        val selectionView: View = itemView.findViewById(R.id.selectionView)

        init {
            // XML 대신 코드로 직접 gravity 설정!
            dayText.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            dayText.setPadding(0, 16, 0, 0)
        }

        fun bind(date: LocalDate) {
            dayText.text = date.dayOfMonth.toString()

            // 선택 효과
            if (date == selectedDate) {
                selectionView.visibility = View.VISIBLE
                dayText.setTextColor(Color.WHITE)
            } else {
                selectionView.visibility = View.GONE
                dayText.setTextColor(Color.BLACK)
            }

            // 일정 바 표시
            val dailySchedules = schedules[date]
            if (!dailySchedules.isNullOrEmpty()) {
                val firstSchedule = dailySchedules[0]
                scheduleText.text = firstSchedule.title

                val background = scheduleText.background.mutate() as GradientDrawable
                background.setColor(firstSchedule.color)
                scheduleText.background = background

                scheduleText.visibility = View.VISIBLE
            } else {
                scheduleText.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClicked(date) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_cell, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = dayList[position]
        if (date != LocalDate.MIN) {
            holder.bind(date)
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = dayList.size
}
package com.example.mycalendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class CalendarAdapter(
    var dayList: ArrayList<LocalDate>, // var로 변경
    private val schedules: Map<LocalDate, List<Schedule>>,
    private val onItemClicked: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    var selectedDate: LocalDate = LocalDate.now() // var로 변경

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.dayText)
        val scheduleContainer: LinearLayout = itemView.findViewById(R.id.scheduleContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_cell, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = dayList.getOrNull(position)
        if (date != null && date != LocalDate.MIN) {
            holder.dayText.text = date.dayOfMonth.toString()
            holder.itemView.visibility = View.VISIBLE

            // --- UI 상태 초기화 ---
            holder.dayText.setTextColor(Color.BLACK)
            holder.dayText.background = null
            holder.itemView.background = null // itemView의 배경도 초기화

            // --- 상태에 따라 UI 변경 (수정된 최종 로직) ---

            // 1. 선택된 날짜일 경우 -> 테두리 표시
            if (date == selectedDate) {
                holder.itemView.setBackgroundResource(R.drawable.selected_day_border)
            }

            // 2. 오늘 날짜일 경우 -> 파란 동그라미와 흰색 글씨
            if (date == LocalDate.now()) {
                holder.dayText.setBackgroundResource(R.drawable.selected_day_background)
                holder.dayText.setTextColor(Color.WHITE)
            }


            // --- 일정 바 그리는 로직 (기존과 동일) ---
            holder.scheduleContainer.removeAllViews()
            schedules[date]?.take(2)?.forEach { schedule ->
                val scheduleView = TextView(holder.itemView.context).apply {
                    text = schedule.title
                    textSize = 10f
                    isSingleLine = true
                    setTextColor(Color.WHITE)

                    val background = ContextCompat.getDrawable(context, R.drawable.schedule_bar_background)?.mutate() as? GradientDrawable
                    background?.setColor(schedule.color)
                    this.background = background

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(4, 2, 4, 2)
                    this.layoutParams = layoutParams
                }
                holder.scheduleContainer.addView(scheduleView)
            }

            // 클릭 이벤트는 MainActivity로 전달
            holder.itemView.setOnClickListener { onItemClicked(date) }

        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = dayList.size
}
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

            // UI 초기화
            holder.dayText.setTextColor(Color.BLACK)
            holder.dayText.background = null
            holder.itemView.background = null

            // 선택된 날짜
            if (date == selectedDate) {
                holder.itemView.setBackgroundResource(R.drawable.selected_day_border)
            }
            // 오늘 날짜
            if (date == LocalDate.now()) {
                holder.dayText.setBackgroundResource(R.drawable.selected_day_background)
                holder.dayText.setTextColor(Color.WHITE)
            }

            holder.scheduleContainer.removeAllViews()
            schedules[date]?.distinctBy { it }?.take(2)?.forEach { schedule ->
                val startDate = schedule.startDateTime?.toLocalDate()
                val endDate = schedule.endDateTime?.toLocalDate()

                if (startDate == null || endDate == null || startDate == endDate) {
                    addScheduleBar(holder, schedule, R.drawable.schedule_bar_single, schedule.title)
                    return@forEach
                }

                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    val prevDayInSchedule = date.minusDays(1) >= startDate
                    val nextDayInSchedule = date.plusDays(1) <= endDate
                    val isFirstDayOfRow = date.dayOfWeek == java.time.DayOfWeek.SUNDAY
                    val isLastDayOfRow = date.dayOfWeek == java.time.DayOfWeek.SATURDAY

                    val startsOnThisCell = !prevDayInSchedule || isFirstDayOfRow
                    val endsOnThisCell = !nextDayInSchedule || isLastDayOfRow

                    val backgroundResId = when {
                        startsOnThisCell && endsOnThisCell -> R.drawable.schedule_bar_single
                        startsOnThisCell -> R.drawable.schedule_bar_start
                        endsOnThisCell -> R.drawable.schedule_bar_end
                        else -> R.drawable.schedule_bar_middle
                    }

                    // --- 여기가 수정된 최종 제목 표시 로직입니다 ---
                    val title = if (date == startDate) schedule.title else ""

                    addScheduleBar(holder, schedule, backgroundResId, title)
                }
            }
            holder.itemView.setOnClickListener { onItemClicked(date) }
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }

    // 일정 바 View를 생성하고 추가하는 헬퍼 함수
    private fun addScheduleBar(holder: DayViewHolder, schedule: Schedule, backgroundResId: Int, title: String) {
        val scheduleView = TextView(holder.itemView.context).apply {
            text = title
            textSize = 10f
            isSingleLine = true
            setTextColor(Color.WHITE)
            setPadding(8, 2, 8, 2)

            val background = ContextCompat.getDrawable(context, backgroundResId)?.mutate() as? GradientDrawable
            background?.setColor(schedule.color)
            this.background = background

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // --- 여기가 수정된 최종 여백(margin) 로직입니다 ---
            val isStartPiece = backgroundResId == R.drawable.schedule_bar_start || backgroundResId == R.drawable.schedule_bar_single
            val isEndPiece = backgroundResId == R.drawable.schedule_bar_end || backgroundResId == R.drawable.schedule_bar_single

            val leftMargin = if (isStartPiece) 4 else 0
            val rightMargin = if (isEndPiece) 4 else 0

            // 시작 부분은 왼쪽 여백만, 끝 부분은 오른쪽 여백만, 중간은 여백 없음
            layoutParams.setMargins(leftMargin, 2, rightMargin, 2)
            this.layoutParams = layoutParams
        }
        holder.scheduleContainer.addView(scheduleView)
    }


    override fun getItemCount(): Int = dayList.size
}
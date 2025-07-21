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

    private val MAX_SCHEDULES_PER_DAY = 4

    var selectedDate: LocalDate = LocalDate.now() // var로 변경
    // --- 👇 "어떤 일정이 몇 번째 줄에 있는지" 기억하는 메모장 ---
    private val scheduleIdToSlotMap = mutableMapOf<String, Int>()

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
            if (date == selectedDate) {
                holder.itemView.setBackgroundResource(R.drawable.selected_day_border)
            }
            if (date == LocalDate.now()) {
                holder.dayText.setBackgroundResource(R.drawable.selected_day_background)
                holder.dayText.setTextColor(Color.WHITE)
            }

            holder.scheduleContainer.removeAllViews()
            val dailySchedules = schedules[date]?.distinctBy { it.id }?.sortedBy { it.startDateTime } ?: emptyList()
            val scheduleSlots = arrayOfNulls<Schedule>(MAX_SCHEDULES_PER_DAY)

            // 1. 오늘 일정들을 순회하며, 메모장에 기억된 줄이 있는지 확인
            dailySchedules.forEach { schedule ->
                if (scheduleIdToSlotMap.containsKey(schedule.id)) {
                    val slot = scheduleIdToSlotMap[schedule.id]!!
                    if (slot < scheduleSlots.size) {
                        scheduleSlots[slot] = schedule
                    }
                }
            }

            // 2. 기억에 없는 새로운 일정들을 빈 줄에 배치하고, 메모장에 새로 기억시킴
            dailySchedules.forEach { schedule ->
                if (scheduleSlots.none { it?.id == schedule.id }) {
                    for (i in scheduleSlots.indices) {
                        if (scheduleSlots[i] == null) {
                            scheduleSlots[i] = schedule
                            scheduleIdToSlotMap[schedule.id] = i // 새 위치를 메모장에 기억
                            break
                        }
                    }
                }
            }

            // 3. 최종 슬롯 순서대로 그리기
            scheduleSlots.forEach { schedule ->
                if (schedule != null) {
                    addScheduleBar(holder, schedule, date)
                } else {
                    addEmptyBar(holder)
                }
            }
            holder.itemView.setOnClickListener { onItemClicked(date) }
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }

    // 빈 공간을 그리는 헬퍼 함수
    private fun addEmptyBar(holder: DayViewHolder) {
        val emptyView = View(holder.itemView.context).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (20 * holder.itemView.context.resources.displayMetrics.density).toInt() // 높이 고정
            )
            layoutParams = params
        }
        holder.scheduleContainer.addView(emptyView)
    }

    // 어댑터의 데이터가 바뀔 때마다 메모장을 초기화하는 함수
    override fun onViewRecycled(holder: DayViewHolder) {
        super.onViewRecycled(holder)
        if (holder.layoutPosition == 0) {
            scheduleIdToSlotMap.clear()
        }
    }


    // 일정 바 View를 생성하고 추가하는 헬퍼 함수
    private fun addScheduleBar(holder: DayViewHolder, schedule: Schedule, date: LocalDate) {
        val startDate = schedule.startDateTime?.toLocalDate()
        val endDate = schedule.endDateTime?.toLocalDate()

        val prevDayInSchedule = startDate != null && date.minusDays(1) >= startDate
        val nextDayInSchedule = endDate != null && date.plusDays(1) <= endDate
        val isFirstDayOfRow = date.dayOfWeek == java.time.DayOfWeek.SUNDAY
        val isLastDayOfRow = date.dayOfWeek == java.time.DayOfWeek.SATURDAY

        val startsOnThisCell = !prevDayInSchedule || isFirstDayOfRow
        val endsOnThisCell = !nextDayInSchedule || isLastDayOfRow

        val backgroundResId = when {
            startDate == null || endDate == null || startDate == endDate -> R.drawable.schedule_bar_single
            startsOnThisCell && endsOnThisCell -> R.drawable.schedule_bar_single
            startsOnThisCell -> R.drawable.schedule_bar_start
            endsOnThisCell -> R.drawable.schedule_bar_end
            else -> R.drawable.schedule_bar_middle
        }

        val title =
            if (date == startDate || (isFirstDayOfRow && !date.isBefore(startDate))) schedule.title else ""

        val scheduleView = TextView(holder.itemView.context).apply {
            text = title
            textSize = 10f
            isSingleLine = true
            setTextColor(Color.WHITE)
            setPadding(8, 2, 8, 2)

            val background = ContextCompat.getDrawable(holder.itemView.context, backgroundResId)
                ?.mutate() as? GradientDrawable
            background?.setColor(schedule.color)
            this.background = background

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (20 * holder.itemView.context.resources.displayMetrics.density).toInt()
            )
            val isStartPiece =
                backgroundResId == R.drawable.schedule_bar_start || backgroundResId == R.drawable.schedule_bar_single
            val isEndPiece =
                backgroundResId == R.drawable.schedule_bar_end || backgroundResId == R.drawable.schedule_bar_single
            val leftMargin = if (isStartPiece) 4 else 0
            val rightMargin = if (isEndPiece) 4 else 0
            layoutParams.setMargins(leftMargin, 0, rightMargin, 2)
            this.layoutParams = layoutParams
        }
        holder.scheduleContainer.addView(scheduleView)
    }

    override fun getItemCount(): Int = dayList.size
}
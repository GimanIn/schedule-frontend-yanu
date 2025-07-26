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
import com.example.mycalendar.model.Schedule
import java.time.LocalDate
import java.time.DayOfWeek

class CalendarAdapter(
    var dayList: List<LocalDate?>, // 날짜 리스트 (nullable 허용)
    private val schedules: Map<LocalDate, List<Schedule>>, // 날짜별 일정 map
    private val onItemClicked: (LocalDate) -> Unit // 날짜 클릭 이벤트
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private val MAX_SCHEDULES_PER_DAY = 4 // 한 칸에 최대 4개

    var selectedDate: LocalDate = LocalDate.now()

    // schedule.id → 슬롯에 대응하는 map
    private val scheduleIdToSlotMap = mutableMapOf<Long, Int>()

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

        if (date == null || date == LocalDate.MIN) {
            holder.itemView.visibility = View.INVISIBLE
            return
        }

        holder.dayText.text = date.dayOfMonth.toString()
        holder.itemView.visibility = View.VISIBLE

        // 선택 날짜와 오늘 표시
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

        // startTime 기준 정렬 (nullable 처리 포함)
        val dailySchedules = schedules[date]
            ?.sortedWith(
                compareBy<Schedule> { it.startTime ?: java.time.LocalTime.MIN }
            ) ?: emptyList()

        val scheduleSlots = arrayOfNulls<Schedule>(MAX_SCHEDULES_PER_DAY)

        // id 기준으로 슬롯 재배치 (이전 스케줄 유지)
        dailySchedules.forEach { schedule ->
            schedule.id?.let { id ->
                if (scheduleIdToSlotMap.containsKey(id)) {
                    val slot = scheduleIdToSlotMap[id]!!
                    if (slot < scheduleSlots.size) {
                        scheduleSlots[slot] = schedule
                    }
                }
            }
        }

        // 아직 슬롯에 없는 스케줄 배치
        dailySchedules.forEach { schedule ->
            schedule.id?.let { id ->
                if (scheduleSlots.none { it?.id == id }) {
                    for (i in scheduleSlots.indices) {
                        if (scheduleSlots[i] == null) {
                            scheduleSlots[i] = schedule
                            scheduleIdToSlotMap[id] = i
                            break
                        }
                    }
                }
            }
        }

        // 렌더링
        scheduleSlots.forEach { schedule ->
            if (schedule != null) {
                addScheduleBar(holder, schedule, date)
            } else {
                addEmptyBar(holder)
            }
        }

        holder.itemView.setOnClickListener { onItemClicked(date) }
    }

    private fun addEmptyBar(holder: DayViewHolder) {
        val emptyView = View(holder.itemView.context).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (20 * holder.itemView.context.resources.displayMetrics.density).toInt()
            )
            layoutParams = params
        }
        holder.scheduleContainer.addView(emptyView)
    }

    override fun onViewRecycled(holder: DayViewHolder) {
        super.onViewRecycled(holder)
        // Do nothing or consider resetting per date instead
    }

    fun setBaseDate(date: LocalDate) {
        this.selectedDate = date
    }

    // 일정 바 View를 생성하고 추가하는 헬퍼 함수
    private fun addScheduleBar(holder: DayViewHolder, schedule: Schedule, date: LocalDate) {
        val startDate = schedule.startDate // Schedule.kt 기준
        val endDate = schedule.endDate   // endDate가 따로 없으므로 startDate만 사용

        // 범위 비교 (단일 날짜 기반 처리)
        val prevDayInSchedule = date.minusDays(1) >= startDate
        val nextDayInSchedule = date.plusDays(1) <= endDate

        val isFirstDayOfRow = date.dayOfWeek == DayOfWeek.SUNDAY
        val isLastDayOfRow = date.dayOfWeek == DayOfWeek.SATURDAY

        val startsOnThisCell = !prevDayInSchedule || isFirstDayOfRow
        val endsOnThisCell = !nextDayInSchedule || isLastDayOfRow

        val backgroundResId = when {
            startDate == endDate -> R.drawable.schedule_bar_single
            startsOnThisCell && endsOnThisCell -> R.drawable.schedule_bar_single
            startsOnThisCell -> R.drawable.schedule_bar_start
            endsOnThisCell -> R.drawable.schedule_bar_end
            else -> R.drawable.schedule_bar_middle
        }

        val categoryPrefix = schedule.category?.takeIf { it.isNotBlank() }?.let {
            "${it.first()}) "
        } ?: ""

        val titleText = if (date == startDate || (isFirstDayOfRow && !date.isBefore(startDate))) schedule.title else ""
        val title = "$categoryPrefix$titleText"

        val scheduleView = TextView(holder.itemView.context).apply {
            text = title
            textSize = 10f
            isSingleLine = true
            setTextColor(Color.WHITE)
            setPadding(8, 2, 8, 2)

            val background = ContextCompat.getDrawable(holder.itemView.context, backgroundResId)
                ?.mutate() as? GradientDrawable
            val safeColor = schedule.color
            background?.setColor(safeColor)

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

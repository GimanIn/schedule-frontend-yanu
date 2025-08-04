package com.example.mycalendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.Schedule
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.LocalTime

class CalendarAdapter(
    var dayList: List<LocalDate?>, // 날짜 리스트 (nullable 허용)
    var schedules: MutableMap<LocalDate, MutableList<Schedule>>,
    private var holidayMap: Map<LocalDate, String>, // 🔧 var로 변경!
    private val onItemClicked: (LocalDate) -> Unit // 날짜 클릭 이벤트
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private val maxSchedulesPerDay = 3

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
        val currentDate = dayList.getOrNull(position)

        if (currentDate == null || currentDate == LocalDate.MIN) {
            holder.itemView.visibility = View.INVISIBLE
            return
        }

        holder.dayText.text = currentDate.dayOfMonth.toString()
        holder.itemView.visibility = View.VISIBLE

        // --- 🎈 공휴일 및 주말 색상 처리 로직 ---
        val isHoliday = holidayMap.containsKey(currentDate)

        if (isHoliday) {
            // 공휴일이면 빨간색
            holder.dayText.setTextColor(Color.RED)
        } else if (currentDate.dayOfWeek == DayOfWeek.SUNDAY) {
            // 일요일이면 빨간색
            holder.dayText.setTextColor(Color.RED)
        } else {
            // 평일은 검은색
            holder.dayText.setTextColor(Color.BLACK)
        }

        // 선택 날짜와 오늘 표시
        holder.dayText.background = null
        holder.itemView.background = null

        if (currentDate == selectedDate) {
            holder.itemView.setBackgroundResource(R.drawable.selected_day_border)
        }
        if (currentDate == LocalDate.now()) {
            holder.dayText.setBackgroundResource(R.drawable.selected_day_background)
            holder.dayText.setTextColor(Color.WHITE)
        }

        holder.scheduleContainer.removeAllViews()

        if (isHoliday) {
            // 공휴일 바를 맨 위에 추가
            addHolidayBar(holder, holidayMap[currentDate] ?: "공휴일")
        }

        // ✅ 해당 날짜의 일정 가져오기 (Elvis 연산자 수정)
        val dailySchedules = schedules[currentDate]
            ?.sortedWith(
                compareBy { it.startTime ?: LocalTime.MIN } // 🔧 java.time.LocalTime.MIN → LocalTime.MIN
            ) ?: emptyList()

        // ✅ 디버깅 로그
        if (dailySchedules.isNotEmpty()) {
            Log.d("CalendarAdapter", "🎯 $currentDate 일정 렌더링: ${dailySchedules.size}개")
            dailySchedules.forEach { schedule ->
                Log.d("CalendarAdapter", "  📋 ${schedule.title} (색상: ${schedule.color})")
            }
        }

        val scheduleSlots = arrayOfNulls<Schedule>(maxSchedulesPerDay)

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

        // ✅ 렌더링 (로깅 추가)
        var renderedCount = 0
        scheduleSlots.forEach { schedule ->
            if (schedule != null) {
                addScheduleBar(holder, schedule, currentDate)
                renderedCount++
                Log.d("CalendarAdapter", "  ✅ 일정 바 렌더링: ${schedule.title}")
            } else {
                addEmptyBar(holder)
            }
        }

        if (renderedCount > 0) {
            Log.d("CalendarAdapter", "🎨 $currentDate 에 총 ${renderedCount}개 일정 바 렌더링 완료")
        }

        holder.itemView.setOnClickListener { onItemClicked(currentDate) }
    }

    // 🎌 공휴일 맵 업데이트 함수 추가
    fun updateHolidayMap(newHolidayMap: Map<LocalDate, String>) {
        holidayMap = newHolidayMap // 🔧 this. 제거
        Log.d("CalendarAdapter", "🎌 공휴일 맵 업데이트: ${newHolidayMap.size}개")
        newHolidayMap.forEach { (date, name) ->
            Log.d("CalendarAdapter", "  📅 $date: $name")
        }
    }

    // 🎈 추가: 공휴일 바를 생성하고 추가하는 헬퍼 함수
    private fun addHolidayBar(holder: DayViewHolder, holidayName: String) {
        val holidayView = TextView(holder.itemView.context).apply {
            text = holidayName
            textSize = 10f
            isSingleLine = true
            setTextColor(Color.parseColor("#D32F2F")) // 공휴일 텍스트 색상
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(8, 2, 8, 2)

            // 공휴일 바 배경 Drawable 적용
            setBackgroundResource(R.drawable.holiday_bar_background)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (20 * holder.itemView.context.resources.displayMetrics.density).toInt()
            )
            layoutParams.setMargins(4, 0, 4, 2)
            this.layoutParams = layoutParams
        }
        holder.scheduleContainer.addView(holidayView)
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
        // ViewHolder 재사용 시 정리할 내용 없음
    }

    // 일정 바 View를 생성하고 추가하는 헬퍼 함수
    private fun addScheduleBar(holder: DayViewHolder, schedule: Schedule, currentDate: LocalDate) {
        val startDate = schedule.startDate
        val endDate = schedule.endDate

        // 범위 비교 (단일 날짜 기반 처리)
        val prevDayInSchedule = currentDate.minusDays(1) >= startDate
        val nextDayInSchedule = currentDate.plusDays(1) <= endDate

        val isFirstDayOfRow = currentDate.dayOfWeek == DayOfWeek.SUNDAY
        val isLastDayOfRow = currentDate.dayOfWeek == DayOfWeek.SATURDAY

        val startsOnThisCell = !prevDayInSchedule || isFirstDayOfRow
        val endsOnThisCell = !nextDayInSchedule || isLastDayOfRow

        val backgroundResId = when {
            startDate == endDate -> R.drawable.schedule_bar_single
            startsOnThisCell && endsOnThisCell -> R.drawable.schedule_bar_single
            startsOnThisCell -> R.drawable.schedule_bar_start
            endsOnThisCell -> R.drawable.schedule_bar_end
            else -> R.drawable.schedule_bar_middle
        }

        // 1. 먼저 제목이 표시되어야 하는지 확인합니다.
        val titleText = if (currentDate == startDate || (isFirstDayOfRow && !currentDate.isBefore(startDate))) {
            schedule.title
        } else {
            ""
        }

        // 2. 제목이 있을 경우에만 카테고리 접두사를 붙여줍니다.
        val finalTitle = if (titleText.isNotEmpty()) {
            val categoryPrefix = schedule.category?.takeIf { it.isNotBlank() }?.let {
                "${it.first()}) "
            } ?: ""
            "$categoryPrefix$titleText"
        } else {
            "" // 제목이 없으면 카테고리도 표시하지 않습니다.
        }

        val scheduleView = TextView(holder.itemView.context).apply {
            text = finalTitle
            textSize = 10f
            isSingleLine = true
            setTextColor(Color.WHITE)
            setPadding(8, 2, 8, 2)

            // ✅ 안전한 색상 처리 (KTX 확장함수 사용)
            val background = ContextCompat.getDrawable(holder.itemView.context, backgroundResId)
                ?.mutate() as? GradientDrawable

            val safeColor = try {
                schedule.color // 이미 Int 타입이므로 그대로 사용
            } catch (e: Exception) {
                Log.w("CalendarAdapter", "색상 파싱 실패: ${schedule.color}, 기본색 사용")
                "#4285F4".toColorInt() // 🔧 KTX 확장함수 사용
            }
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

            if (schedule.alarmOn) {
                // TextView의 오른쪽(End)에 아이콘을 설정합니다. (Left, Top, Right, Bottom)
                setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_alarm_indicator, 0)
                // 텍스트와 아이콘 사이의 간격을 줍니다.
                compoundDrawablePadding = 8
            }
        }

        holder.scheduleContainer.addView(scheduleView)
    }

    override fun getItemCount(): Int = dayList.size

    // ✅ 일정 데이터 상태 확인 함수 (디버깅용) - refreshData 함수 제거하고 이것만 유지
    fun logCurrentState() {
        Log.d("CalendarAdapter", "📊 === CalendarAdapter 현재 상태 ===")
        Log.d("CalendarAdapter", "📊 총 날짜 수: ${schedules.size}")
        schedules.forEach { (date, scheduleList) ->
            if (scheduleList.isNotEmpty()) {
                Log.d("CalendarAdapter", "📊 $date: ${scheduleList.size}개")
                scheduleList.forEach { schedule ->
                    Log.d("CalendarAdapter", "   - ${schedule.title}")
                }
            }
        }
    }
}
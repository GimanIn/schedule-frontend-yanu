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
import android.widget.LinearLayout

class CalendarAdapter(
    private val dayList: ArrayList<LocalDate>,
    private val schedules: Map<LocalDate, List<Schedule>>,
    private var selectedDate: LocalDate,
    private val onItemClicked: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.dayText)
        val selectionView: View = itemView.findViewById(R.id.selectionView)
        // 👇 scheduleText 대신 scheduleContainer를 찾습니다.
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

            // 선택 효과
            if (date == selectedDate) {
                holder.selectionView.visibility = View.VISIBLE
                holder.dayText.setTextColor(Color.WHITE)
            } else {
                holder.selectionView.visibility = View.GONE
                holder.dayText.setTextColor(Color.BLACK)
            }

            // 👇 일정 바를 동적으로 생성하는 로직
            holder.scheduleContainer.removeAllViews() // 이전에 있던 뷰들을 모두 제거
            val dailySchedules = schedules[date]
            if (!dailySchedules.isNullOrEmpty()) {
                // 최대 2개의 일정만 표시 (UI가 깨지지 않도록)
                dailySchedules.take(2).forEach { schedule ->
                    // 새로운 TextView(일정 바)를 코드로 직접 생성
                    val scheduleView = TextView(holder.itemView.context).apply {
                        text = schedule.title
                        textSize = 10f // sp
                        isSingleLine = true

                        // 배경 설정 및 색상 적용
                        val background = ContextCompat.getDrawable(context, R.drawable.schedule_bar_background)?.mutate() as GradientDrawable
                        background.setColor(schedule.color)
                        this.background = background

                        // 여백 등 기타 속성 설정
                        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        layoutParams.setMargins(2, 2, 2, 2)
                        this.layoutParams = layoutParams
                    }
                    // 컨테이너에 추가
                    holder.scheduleContainer.addView(scheduleView)
                }
            }

            holder.itemView.setOnClickListener { onItemClicked(date) }
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }

    fun updateSchedules(newSchedules: List<Schedule>) {
        // 이 부분은 어댑터의 데이터를 새 것으로 교체하는 로직입니다.
        // 현재는 생성자에서만 데이터를 받으므로, 이 함수는 나중에
        // 더 복잡한 실시간 업데이트에 사용될 수 있습니다.
        // 지금 당장은 이 함수를 추가만 해두셔도 좋습니다.
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = dayList.size
}
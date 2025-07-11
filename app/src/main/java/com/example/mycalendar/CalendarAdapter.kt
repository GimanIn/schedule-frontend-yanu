package com.example.mycalendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val dayList: ArrayList<String>,
    private val onItemClicked: (String, Int) -> Unit // 클릭 이벤트를 처리할 함수
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var selectedPosition = -1

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.dayText)

        fun bind(day: String, position: Int) {
            dayText.text = day

            // 선택 상태에 따른 UI 변경
            if (position == selectedPosition) {
                dayText.setBackgroundResource(R.drawable.selected_day_background)
                dayText.setTextColor(Color.WHITE)
            } else {
                dayText.background = null
                dayText.setTextColor(Color.BLACK) // 기본 색상으로 되돌리기
            }

            // 클릭 리스너 설정
            itemView.setOnClickListener {
                if (day.isNotEmpty()) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition) // 이전 선택 항목 갱신
                    notifyItemChanged(position)   // 현재 선택 항목 갱신
                    onItemClicked(day, position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_cell, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(dayList[position], position)
    }

    override fun getItemCount(): Int {
        return dayList.size
    }
}
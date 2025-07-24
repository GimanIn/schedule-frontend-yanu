package com.example.mycalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.Schedule
import java.time.format.DateTimeFormatter
import java.time.LocalTime

class SearchListAdapter(
    private var scheduleList: List<Schedule>
) : RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    // ✅ NEW: 날짜/시간 포맷 공통 상수로 분리
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateRangeText: TextView = view.findViewById(R.id.dateRangeText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val colorBlock: View = view.findViewById(R.id.colorBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 기존: item_search_result 레이아웃을 inflate하여 뷰 홀더 생성
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = scheduleList[position]

        // 기존: 제목 및 색상 설정
        holder.titleText.text = schedule.title
        holder.colorBlock.setBackgroundColor(schedule.color)

        // ✅ NEW: 날짜 표시 (LocalDate → yyyy년 M월 d일)
        holder.dateRangeText.text = schedule.startDate.format(DATE_FORMATTER)

        // ✅ NEW: 시간 표시 (null-safe 방식으로 start/end 표시)
        if (schedule.startTime != null) {
            val startText = schedule.startTime.format(TIME_FORMATTER)
            val endText = schedule.endTime?.format(TIME_FORMATTER)

            // ✅ NEW: endTime이 null이면 "-" 생략
            holder.timeText.text = if (endText != null) {
                "$startText - $endText"
            } else {
                "$startText"
            }
            holder.timeText.visibility = View.VISIBLE
        } else {
            // 기존: startTime이 없으면 시간 텍스트 숨김
            holder.timeText.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = scheduleList.size

    // 기존: 외부에서 데이터 갱신할 수 있도록 update 함수 제공
    fun updateData(newList: List<Schedule>) {
        scheduleList = newList
        notifyDataSetChanged()
    }
}

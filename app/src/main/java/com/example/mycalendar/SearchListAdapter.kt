package com.example.mycalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

class SearchListAdapter(
    private var scheduleList: List<Schedule>
) : RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateRangeText: TextView = view.findViewById(R.id.dateRangeText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val colorBlock: View = view.findViewById(R.id.colorBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = scheduleList[position]
        holder.titleText.text = schedule.title
        holder.colorBlock.setBackgroundColor(schedule.color)

        // 변경될 수 있는 var 속성을 변경 불가능한 val 지역 변수에 복사합니다.
        val startDateTime = schedule.startDateTime
        val endDateTime = schedule.endDateTime

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // 이제 schedule.startDateTime 대신 지역 변수 startDateTime을 사용합니다.
        if (startDateTime != null) {
            val startDate = startDateTime.toLocalDate()
            val endDate = endDateTime?.toLocalDate() ?: startDate
            holder.dateRangeText.text = if (startDate == endDate) {
                startDate.format(dateFormatter)
            } else {
                "${startDate.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}"
            }
            holder.timeText.text = "${startDateTime.format(timeFormatter)} - ${endDateTime?.format(timeFormatter)}"
            holder.timeText.visibility = View.VISIBLE
        } else {
            holder.dateRangeText.text = "날짜 정보 없음" // createdAt은 Firebase용이므로 대체
            holder.timeText.visibility = View.GONE
        }
    }

    override fun getItemCount() = scheduleList.size

    // 검색 결과가 바뀔 때마다 호출될 함수
    fun updateData(newList: List<Schedule>) {
        scheduleList = newList
        notifyDataSetChanged()
    }
}
package com.example.mycalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

class ScheduleListAdapter(
    private val scheduleList: List<Schedule>,
    private val onScheduleClicked: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleListAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorBar: View = itemView.findViewById(R.id.colorBarView)
        val timeText: TextView = itemView.findViewById(R.id.timeTextView)
        val titleText: TextView = itemView.findViewById(R.id.titleTextView)
        val timeRangeText: TextView = itemView.findViewById(R.id.timeRangeTextView)

        fun bind(schedule: Schedule) {
            titleText.text = schedule.title
            colorBar.setBackgroundColor(schedule.color)

            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            // schedule.startTime과 schedule.endTime을 모두 새 변수 이름으로 변경합니다.
            // 변경될 수 있는 var 변수를 변경 불가능한 val 지역 변수에 담아서 사용합니다.
            val startDateTime = schedule.startDateTime
            val endDateTime = schedule.endDateTime

            if (startDateTime != null) {
                timeText.text = startDateTime.format(formatter)
                timeText.visibility = View.VISIBLE

                if (endDateTime != null) {
                    timeRangeText.text = "${startDateTime.format(formatter)} - ${endDateTime.format(formatter)}"
                    timeRangeText.visibility = View.VISIBLE
                } else {
                    timeRangeText.visibility = View.GONE
                }
            } else {
                timeText.visibility = View.GONE
                timeRangeText.visibility = View.GONE
            }

            itemView.setOnClickListener { onScheduleClicked(schedule) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleList[position])
    }

    override fun getItemCount(): Int = scheduleList.size
}
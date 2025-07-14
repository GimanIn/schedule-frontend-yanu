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

            if (schedule.startTime != null) {
                timeText.text = schedule.startTime.format(formatter)
                timeText.visibility = View.VISIBLE

                if (schedule.endTime != null) {
                    timeRangeText.text = "${schedule.startTime.format(formatter)} - ${schedule.endTime.format(formatter)}"
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
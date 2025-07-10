package com.example.mycalendar

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CalendarAdapter(
    private val context: Context,
    private val dayList: ArrayList<String>
) : BaseAdapter() {

    private var selectedPosition = -1

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    override fun getCount(): Int = dayList.size
    override fun getItem(position: Int): Any = dayList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.day_cell, parent, false)
        val dayText: TextView = view.findViewById(R.id.dayText)
        val day = dayList[position]

        if (day.isEmpty()) {
            dayText.text = ""
            dayText.background = null // 빈 칸은 배경 없음
        } else {
            dayText.text = day

            // 선택된 날짜인 경우 dayText에 배경을 설정
            if (position == selectedPosition) {
                dayText.background = ContextCompat.getDrawable(context, R.drawable.selected_day_background)
                dayText.setTextColor(Color.WHITE)
            } else {
                dayText.background = null
                dayText.setTextColor(Color.BLACK)
            }
        }

        // 주말 색상 변경 (선택되지 않았을 때만 적용)
        if (position != selectedPosition) {
            val dayOfWeek = (position + 1) % 7
            if (dayOfWeek == 1) { // 일요일
                dayText.setTextColor(Color.RED)
            } else if (dayOfWeek == 0) { // 토요일
                dayText.setTextColor(Color.BLUE)
            }
        }

        return view
    }
}
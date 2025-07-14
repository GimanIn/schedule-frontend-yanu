package com.example.mycalendar

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class ScheduleListDialog(
    private val date: LocalDate,
    private val onDataChanged: () -> Unit
) : DialogFragment() {

    private val mainActivity by lazy { activity as MainActivity }
    private val schedules by lazy { mainActivity.schedules }
    private val dailySchedules by lazy { schedules[date]?.toMutableList() ?: mutableListOf() }

    private lateinit var scheduleListAdapter: ScheduleListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_list, container, false)

        val titleLayout = view.findViewById<View>(R.id.titleLayout)
        val titleTextView = titleLayout.findViewById<TextView>(R.id.titleTextView)
        val closeButton = titleLayout.findViewById<ImageButton>(R.id.closeButton)
        val recyclerView = view.findViewById<RecyclerView>(R.id.scheduleListRecyclerView)
        val scheduleEditTextDialog = view.findViewById<EditText>(R.id.scheduleEditTextDialog)
        val addButtonDialog = view.findViewById<ImageButton>(R.id.addButtonDialog)

        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREA)
        titleTextView.text = "${date.dayOfMonth} ${dayOfWeek}"
        scheduleEditTextDialog.hint = "${date.monthValue}월 ${date.dayOfMonth}일에 추가"

        closeButton.setOnClickListener { dismiss() }

        scheduleListAdapter = ScheduleListAdapter(dailySchedules) { schedule ->
            Toast.makeText(context, "'${schedule.title}' 수정 기능은 구현 예정입니다.", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = scheduleListAdapter

        addButtonDialog.setOnClickListener {
            val title = scheduleEditTextDialog.text.toString()
            if (title.isNotEmpty()) {
                val newSchedule = Schedule(title, null, null, Color.GRAY, false, "", true, false)
                mainActivity.addSchedule(date, newSchedule)

                dailySchedules.add(newSchedule)
                scheduleListAdapter.notifyItemInserted(dailySchedules.size - 1)
                scheduleEditTextDialog.text.clear()
            } else {
                dismiss()
                mainActivity.showAddScheduleDialog(date)
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDataChanged()
    }
}
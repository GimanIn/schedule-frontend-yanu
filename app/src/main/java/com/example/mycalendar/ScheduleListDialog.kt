package com.example.mycalendar

import android.content.DialogInterface
import android.content.Intent
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

// 생성자에서 mainActivity 대신 일정 목록을 직접 받도록 수정
class ScheduleListDialog(
    private val date: LocalDate,
    private val dailySchedules: MutableList<Schedule>,
    private val onDataChanged: () -> Unit
) : DialogFragment() {

    private lateinit var scheduleListAdapter: ScheduleListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_list, container, false)

        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val recyclerView = view.findViewById<RecyclerView>(R.id.scheduleListRecyclerView)
        val scheduleEditTextDialog = view.findViewById<EditText>(R.id.scheduleEditTextDialog)
        val addButtonDialog = view.findViewById<ImageButton>(R.id.addButtonDialog)

        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREA)
        titleTextView.text = "${date.dayOfMonth} ${dayOfWeek}"
        scheduleEditTextDialog.hint = "${date.monthValue}월 ${date.dayOfMonth}일에 추가"

        closeButton.setOnClickListener { dismiss() }

        // 어댑터에 일정 목록 전달
        scheduleListAdapter = ScheduleListAdapter(dailySchedules) { schedule ->
            Toast.makeText(context, "'${schedule.title}' 수정 기능은 구현 예정입니다.", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = scheduleListAdapter

        addButtonDialog.setOnClickListener {
            val title = scheduleEditTextDialog.text.toString()
            if (title.isNotEmpty()) {
                val newSchedule = Schedule(title, null, null, Color.GRAY, false, "", true, false)

                // MainActivity의 public 함수를 통해 일정 추가 요청
                (activity as? MainActivity)?.addSchedule(date, newSchedule)

                // 현재 다이얼로그의 목록에도 추가하고 화면 갱신
                dailySchedules.add(newSchedule)
                scheduleListAdapter.notifyItemInserted(dailySchedules.size - 1)
                scheduleEditTextDialog.text.clear()
            } else {
                // 아무것도 입력하지 않고 + 누르면 AddScheduleActivity 열기
                val intent = Intent(context, AddScheduleActivity::class.java)
                intent.putExtra("selectedDate", date)
                context?.startActivity(intent)
                dismiss()
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    // 다이얼로그가 닫힐 때 MainActivity에 데이터가 변경되었음을 알림
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDataChanged()
    }
}
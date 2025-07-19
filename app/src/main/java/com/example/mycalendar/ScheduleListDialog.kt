package com.example.mycalendar

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial // import 추가
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ScheduleListDialog(
    private val date: LocalDate,
    private val dailySchedules: MutableList<Schedule>,
    private val onDataChanged: () -> Unit,
    private val onAddNewSchedule: (LocalDate) -> Unit
) : DialogFragment() {

    private lateinit var listViewContainer: View
    private lateinit var detailViewContainer: View
    private lateinit var titleTextView: TextView
    private lateinit var scheduleListRecyclerView: RecyclerView
    private lateinit var scheduleEditTextDialog: EditText
    private lateinit var addButtonDialog: ImageButton
    private lateinit var backToListButton: ImageButton
    private lateinit var detailDateText: TextView
    private lateinit var detailMemoText: TextView
    private lateinit var deleteButton: Button
    private lateinit var hoursContainer: LinearLayout
    private lateinit var scheduleBlocksContainer: FrameLayout
    private lateinit var editScheduleButton: ImageButton
    private lateinit var colorPaletteLayout: LinearLayout
    private lateinit var detailAlarmSwitch: SwitchMaterial
    private val hourHeightDp = 60

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_list, container, false)
        initViews(view)
        setupListView()
        setupDetailViewListeners()
        return view
    }

    // 모든 UI 요소를 한 곳에서 초기화
    private fun initViews(view: View) {
        listViewContainer = view.findViewById(R.id.listViewContainer)
        detailViewContainer = view.findViewById(R.id.detailViewContainer)
        titleTextView = view.findViewById(R.id.titleTextView)
        scheduleListRecyclerView = view.findViewById(R.id.scheduleListRecyclerView)
        scheduleEditTextDialog = view.findViewById(R.id.scheduleEditTextDialog)
        addButtonDialog = view.findViewById(R.id.addButtonDialog)
        backToListButton = view.findViewById(R.id.backToListButton)
        detailDateText = view.findViewById(R.id.detailDateText)
        editScheduleButton = detailViewContainer.findViewById(R.id.editScheduleButton)
        colorPaletteLayout = detailViewContainer.findViewById(R.id.colorPaletteLayout)
        detailAlarmSwitch = detailViewContainer.findViewById(R.id.detailAlarmSwitch)
        detailMemoText = view.findViewById(R.id.detailMemoText)
        deleteButton = view.findViewById(R.id.deleteButton)
        hoursContainer = view.findViewById(R.id.hoursContainer)
        scheduleBlocksContainer = view.findViewById(R.id.scheduleBlocksContainer)
    }

    private fun setupListView() {
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREA)
        titleTextView.text = "${date.dayOfMonth} ${dayOfWeek}"
        scheduleEditTextDialog.hint = "${date.monthValue}월 ${date.dayOfMonth}일에 추가"

        val scheduleListAdapter = ScheduleListAdapter(dailySchedules) { schedule ->
            showDetailView(schedule)
        }
        scheduleListRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleListRecyclerView.adapter = scheduleListAdapter

        addButtonDialog.setOnClickListener {
            val title = scheduleEditTextDialog.text.toString()
            if (title.isNotEmpty()) {
                val newSchedule = Schedule(title = title)
                (activity as? MainActivity)?.addSchedule(newSchedule)
                dailySchedules.add(newSchedule)
                scheduleListAdapter.notifyItemInserted(dailySchedules.size - 1)
                scheduleEditTextDialog.text.clear()
            } else {
                onAddNewSchedule(date)
                dismiss()
            }
        }
    }

    private fun setupDetailViewListeners() {
        backToListButton.setOnClickListener {
            listViewContainer.visibility = View.VISIBLE
            detailViewContainer.visibility = View.GONE
        }
    }

    private fun showDetailView(schedule: Schedule) {
        listViewContainer.visibility = View.GONE
        detailViewContainer.visibility = View.VISIBLE

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
        detailDateText.text = date.format(dateFormatter)
        detailMemoText.text = schedule.memo
        detailAlarmSwitch.isChecked = schedule.isAlarmOn

        populateTimeline()
        addScheduleBlockToTimeline(schedule)

        colorPaletteLayout.removeAllViews()
        val colors = listOf(
            Color.parseColor("#EF9A9A"), Color.parseColor("#90CAF9"), Color.parseColor("#A5D6A7"),
            Color.parseColor("#FFE082"), Color.parseColor("#B39DDB")
        )
        colors.forEach { color ->
            val colorView = View(requireContext()).apply { // context 대신 requireContext() 사용
                layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginStart = 8 }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.add_button_circle_background)?.mutate()
                background.setTint(color)
                setOnClickListener {
                    schedule.color = color
                    Toast.makeText(context, "색상이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    addScheduleBlockToTimeline(schedule)
                    onDataChanged()
                }
            }
            colorPaletteLayout.addView(colorView)
        }

        detailAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            schedule.isAlarmOn = isChecked
            Toast.makeText(context, "알림 설정이 변경되었습니다.", Toast.LENGTH_SHORT).show()
        }

        editScheduleButton.setOnClickListener {
            (activity as? MainActivity)?.openEditScheduleActivity(schedule)
            dismiss() // 다이얼로그 닫기
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("일정 삭제")
                .setMessage("'${schedule.title}' 일정을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    Toast.makeText(context, "삭제 기능 구현 필요", Toast.LENGTH_SHORT).show()
                    listViewContainer.visibility = View.VISIBLE
                    detailViewContainer.visibility = View.GONE
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun populateTimeline() {
        hoursContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (hour in 0..23) {
            val hourLineView = inflater.inflate(R.layout.item_hour_line, hoursContainer, false)
            val hourTextView = hourLineView.findViewById<TextView>(R.id.hourTextView)
            hourTextView.text = String.format("%02d:00", hour)
            hoursContainer.addView(hourLineView)
        }
    }

    private fun addScheduleBlockToTimeline(schedule: Schedule) {
        scheduleBlocksContainer.removeAllViews()

        // --- 👇 이 부분이 수정되었습니다 ---
        // 변경될 수 있는 var 변수를 변경 불가능한 val 지역 변수에 담아서 사용합니다.
        val startDateTime = schedule.startDateTime
        val endDateTime = schedule.endDateTime

        // 시간 정보가 없으면 하루 종일 일정으로 처리
        val startHour = startDateTime?.hour ?: 0
        val startMinute = startDateTime?.minute ?: 0
        val endHour = endDateTime?.hour ?: 24
        val endMinute = endDateTime?.minute ?: 0
        val startTotalHours = startHour + startMinute / 60.0
        var endTotalHours = endHour + endMinute / 60.0
        if (endTotalHours == 0.0 && schedule.endDateTime != null) {
            endTotalHours = 24.0
        }
        val durationHours = endTotalHours - startTotalHours
        if (durationHours <= 0) return

        val density = resources.displayMetrics.density
        val hourHeightPx = (hourHeightDp * density).toInt()
        val topMargin = (startTotalHours * hourHeightPx).toInt()
        val height = (durationHours * hourHeightPx).toInt()

        val inflater = LayoutInflater.from(context)
        val scheduleBlockView = inflater.inflate(R.layout.item_schedule_block, scheduleBlocksContainer, false) as LinearLayout

        // --- 수정됨: 블록 내부의 View들을 여기서 찾아서 사용합니다. ---
        val blockTitleText = scheduleBlockView.findViewById<TextView>(R.id.blockTitleText)
        val blockTimeText = scheduleBlockView.findViewById<TextView>(R.id.blockTimeText)

        blockTitleText.text = schedule.title
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        // schedule.startDateTime 대신 지역 변수인 startDateTime을 사용합니다.
        if (startDateTime != null && endDateTime != null) {
            blockTimeText.text = "${startDateTime.format(timeFormatter)} - ${endDateTime.format(timeFormatter)}"
        } else {
            blockTimeText.text = "하루 종일"
        }

        (scheduleBlockView.background.mutate() as? GradientDrawable)?.setColor(schedule.color)

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height)
        params.topMargin = topMargin

        scheduleBlocksContainer.addView(scheduleBlockView, params)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val height = (displayMetrics.heightPixels * 0.7).toInt()
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDataChanged()
    }
}
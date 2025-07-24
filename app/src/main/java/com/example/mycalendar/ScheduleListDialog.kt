package com.example.mycalendar

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.Schedule
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

// ✅ 하드코딩된 값들 정리용 상수
private const val DEFAULT_COLOR = Color.BLUE // NEW: 기본 색상 상수
private const val EMPTY_STRING = ""          // NEW: 빈 문자열 상수

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
    private lateinit var scheduleListAdapter: ScheduleListAdapter
    private lateinit var scheduleEditTextDialog: EditText
    private lateinit var addButtonDialog: ImageButton
    private lateinit var backToListButton: ImageButton
    private lateinit var detailDateText: TextView
    private lateinit var detailMemoText: TextView
    private lateinit var deleteButton: Button
    private lateinit var hoursContainer: LinearLayout
    private lateinit var scheduleBlocksContainer: FrameLayout
    private lateinit var editScheduleButton: ImageButton
    private lateinit var currentColorView: View
    private lateinit var detailAlarmSwitch: SwitchMaterial
    private val hourHeightDp = 60

    private var dataChanged = false // NEW: onDismiss 시 변경 여부 체크용

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_list, container, false)
        initViews(view)
        setupListView()
        setupDetailViewListeners()
        return view
    }

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
        currentColorView = detailViewContainer.findViewById(R.id.currentColorView)
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

        scheduleListAdapter = ScheduleListAdapter(
            dailySchedules,
            childFragmentManager,
            { schedule -> showDetailView(schedule) },
            { schedule ->
                (activity as? MainActivity)?.openEditScheduleActivity(schedule, isCopy = false)
                dismiss()
            },
            { schedule ->
                (activity as? MainActivity)?.openEditScheduleActivity(schedule, isCopy = true)
                dismiss()
            },
            { schedule, position ->
                (activity as? MainActivity)?.removeSchedule(schedule)
                dailySchedules.removeAt(position)
                scheduleListAdapter.notifyItemRemoved(position)
                dataChanged = true // NEW: 변경 감지
                Toast.makeText(context, "'${schedule.title}' 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
        )
        scheduleListRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleListRecyclerView.adapter = scheduleListAdapter

        addButtonDialog.setOnClickListener {
            val title = scheduleEditTextDialog.text.toString().trim() // NEW: 공백 제거
            if (title.isNotEmpty()) {
                try {
                    val newSchedule = Schedule(
                        id = 0L,
                        title = title,
                        memo = EMPTY_STRING,        // NEW
                        location = EMPTY_STRING,    // NEW
                        category = EMPTY_STRING,    // NEW
                        color = DEFAULT_COLOR,      // NEW
                        startDate = date,
                        endDate = date,             // NEW: 추가된 endDate
                        startTime = null,
                        endTime = null,
                        isConfirmed = false,
                        alarmOn = false,
                        isDeleted = false,          // NEW: 추가된 isDeleted
                        copiedFromScheduleId = null, // NEW: 추가된 copiedFromScheduleId
                        createdAt = null,           // NEW: 추가된 createdAt
                        updatedAt = null            // NEW: 추가된 updatedAt
                    )
                    (activity as? MainActivity)?.addSchedule(newSchedule)
                    dailySchedules.add(newSchedule)
                    scheduleListAdapter.notifyItemInserted(dailySchedules.size - 1)
                    scheduleEditTextDialog.text.clear()
                    dataChanged = true // NEW
                } catch (e: Exception) {
                    Toast.makeText(context, "일정 추가 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
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
            scheduleListAdapter.notifyDataSetChanged()
        }
    }

    private fun showDetailView(schedule: Schedule) {
        val categoryText = schedule.category?.takeIf { it.isNotBlank() } ?: EMPTY_STRING
        val locationText = schedule.location?.takeIf { it.isNotBlank() } ?: EMPTY_STRING
        val memoText = schedule.memo?.takeIf { it.isNotBlank() } ?: EMPTY_STRING

        detailAlarmSwitch.isChecked = schedule.alarmOn

        detailMemoText.text = listOf(
            if (categoryText.isNotEmpty()) "카테고리: $categoryText" else null,
            if (locationText.isNotEmpty()) "장소: $locationText" else null,
            if (memoText.isNotEmpty()) "메모: $memoText" else null
        ).filterNotNull().joinToString("\n")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (dataChanged) onDataChanged() // NEW: 변경 시에만 콜백 호출
    }
}


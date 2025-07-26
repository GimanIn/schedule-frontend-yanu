// ✅ ScheduleListDialog.kt 전체 수정본
package com.example.mycalendar

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.Schedule
import com.example.mycalendar.model.ScheduleRequest
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

private const val DEFAULT_COLOR = Color.BLUE
private const val EMPTY_STRING = ""

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

    private var dataChanged = false

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
            { schedule -> dismiss() },
            { schedule -> dismiss() },
            { schedule, position ->
                (activity as? MainActivity)?.removeSchedule(schedule)
                dailySchedules.removeAt(position)
                scheduleListAdapter.notifyItemRemoved(position)
                dataChanged = true
                Toast.makeText(context, "'${schedule.title}' 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
        )
        scheduleListRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleListRecyclerView.adapter = scheduleListAdapter

        addButtonDialog.setOnClickListener {
            val title = scheduleEditTextDialog.text.toString().trim()
            if (title.isNotEmpty()) {
                try {
                    // ✅ 1. 서버 전송용 request 객체 생성
                    val request = ScheduleRequest(
                        title = title,
                        memo = EMPTY_STRING,
                        location = EMPTY_STRING,
                        category = EMPTY_STRING,
                        scheduledDate = date.toString(),
                        startDate = date.toString(),
                        endDate = date.toString(),
                        startTime = null,
                        endTime = null,
                        allDay = false,
                        isConfirmed = false,
                        color = "#4285F4",
                        alarmOn = false,
                        copiedFromScheduleId = null
                    )

                    // ✅ 2. 서버로 일정 추가 요청
                    (activity as? MainActivity)?.addSchedule(request)

                    // ✅ 3. UI용 Schedule 생성 후 리스트에 추가
                    val newSchedule = Schedule(
                        id = 0L,
                        title = title,
                        memo = EMPTY_STRING,
                        location = EMPTY_STRING,
                        category = EMPTY_STRING,
                        color = DEFAULT_COLOR,
                        startDate = date,
                        endDate = date,
                        startTime = null,
                        endTime = null,
                        isConfirmed = false,
                        alarmOn = false,
                        isDeleted = false,
                        copiedFromScheduleId = null,
                        createdAt = null,
                        updatedAt = null,
                        scheduledDate = date
                    )

                    dailySchedules.add(newSchedule)
                    scheduleListAdapter.notifyItemInserted(dailySchedules.size - 1)

                    // ✅ 4. 달력 전체 새로고침
                    (activity as? MainActivity)?.fetchAllSchedulesForMonth()

                    // ✅ 5. 입력 초기화 및 콜백
                    scheduleEditTextDialog.text.clear()
                    dataChanged = true
                    onDataChanged()

                } catch (e: Exception) {
                    Toast.makeText(context, "일정 추가 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                onAddNewSchedule(date)
                dismiss()
            }
        }
    }

    // ✅ 따로 분리해서 오류 방지
    private fun setupDetailViewListeners() {
        backToListButton.setOnClickListener {
            listViewContainer.visibility = View.VISIBLE
            detailViewContainer.visibility = View.GONE
            scheduleListAdapter.notifyDataSetChanged()
        }
    }

    // ✅ 이것도 밖으로 빼야 오류 안 남
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
        if (dataChanged) onDataChanged()
    }
}

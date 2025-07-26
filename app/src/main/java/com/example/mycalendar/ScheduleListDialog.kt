// ✅ ScheduleListDialog.kt 전체 수정본
package com.example.mycalendar

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import android.graphics.drawable.ColorDrawable
import java.time.LocalTime
import android.graphics.drawable.GradientDrawable
import java.time.format.DateTimeFormatter
import android.net.Uri






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
        scheduleListAdapter.notifyDataSetChanged()


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
                        startTime = LocalTime.of(9, 0).toString(),
                        endTime = LocalTime.of(9, 0).toString(),
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
                        startTime = LocalTime.of(9, 0),
                        endTime = LocalTime.of(10, 0),
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
            listViewContainer.visibility = View.VISIBLE
            detailViewContainer.visibility = View.GONE

        }
    }
    private fun addScheduleBlockToTimeline(schedule: Schedule) {
        scheduleBlocksContainer.removeAllViews()

        // LocalDateTime으로 변환 (날짜와 시간이 모두 있을 경우)
        val startDateTime = if (schedule.startTime != null) {
            schedule.startDate.atTime(schedule.startTime)
        } else null

        val endDateTime = if (schedule.endTime != null) {
            schedule.endDate.atTime(schedule.endTime)
        } else null

        // --- 하루 종일 일정 처리 ---
        if (startDateTime == null || endDateTime == null) {
            val inflater = LayoutInflater.from(context)
            val scheduleBlockView = inflater.inflate(R.layout.item_schedule_block, scheduleBlocksContainer, false) as LinearLayout

            val blockTitleText = scheduleBlockView.findViewById<TextView>(R.id.blockTitleText)
            val blockTimeText = scheduleBlockView.findViewById<TextView>(R.id.blockTimeText)

            blockTitleText.text = schedule.title
            blockTimeText.text = "하루 종일"

            (scheduleBlockView.background.mutate() as? GradientDrawable)?.setColor(schedule.color)

            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scheduleBlocksContainer.addView(scheduleBlockView, params)
            return
        }

        // 시간 기반 블록 위치 계산
        val startHour = startDateTime.hour
        val startMinute = startDateTime.minute
        val endHour = endDateTime.hour
        val endMinute = endDateTime.minute

        val startTotalHours = startHour + startMinute / 60.0
        val endTotalHours = endHour + endMinute / 60.0
        val durationHours = endTotalHours - startTotalHours
        if (durationHours <= 0) return

        val density = resources.displayMetrics.density
        val hourHeightPx = (hourHeightDp * density).toInt()
        val topMargin = (startTotalHours * hourHeightPx).toInt()
        val height = (durationHours * hourHeightPx).toInt()

        val inflater = LayoutInflater.from(context)
        val scheduleBlockView = inflater.inflate(R.layout.item_schedule_block, scheduleBlocksContainer, false) as LinearLayout

        val blockTitleText = scheduleBlockView.findViewById<TextView>(R.id.blockTitleText)
        val blockTimeText = scheduleBlockView.findViewById<TextView>(R.id.blockTimeText)

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        blockTitleText.text = schedule.title
        blockTimeText.text = "${startDateTime.format(timeFormatter)} - ${endDateTime.format(timeFormatter)}"

        (scheduleBlockView.background.mutate() as? GradientDrawable)?.setColor(schedule.color)

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height)
        params.topMargin = topMargin
        scheduleBlocksContainer.addView(scheduleBlockView, params)
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
        listViewContainer.visibility = View.GONE
        detailViewContainer.visibility = View.VISIBLE
        val shareScheduleButton =
            detailViewContainer.findViewById<ImageButton>(R.id.shareScheduleButton)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
        detailDateText.text = date.format(dateFormatter)
        val detailsText = StringBuilder()
        if (!schedule.category.isNullOrBlank()) {
            detailsText.append("카테고리: ${schedule.category}\n")
        }
        if (!schedule.location.isNullOrBlank()) {
            detailsText.append("장소: ${schedule.location}\n")
        }

        if (schedule.memo.isNotBlank()) {
            detailsText.append("메모: ${schedule.memo}")
        }
        detailMemoText.text = detailsText.toString().trim()
        detailAlarmSwitch.isChecked = schedule.alarmOn


        populateTimeline()
        addScheduleBlockToTimeline(schedule)

        val currentColorView = detailViewContainer.findViewById<View>(R.id.currentColorView)
        (currentColorView.background.mutate() as? GradientDrawable)?.setColor(schedule.color)

        currentColorView.setOnClickListener { colorDotView ->
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.popup_color_palette, null)
            val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.elevation = 20f

            val colors = listOf(
                Pair(popupView.findViewById<View>(R.id.palette_color_1), "#4285F4"),
                Pair(popupView.findViewById<View>(R.id.palette_color_2), "#34A853"),
                Pair(popupView.findViewById<View>(R.id.palette_color_3), "#EA4335"),
                Pair(popupView.findViewById<View>(R.id.palette_color_4), "#FFBE00"),
                Pair(popupView.findViewById<View>(R.id.palette_color_5), "#A142F4"),
                Pair(popupView.findViewById<View>(R.id.palette_color_6), "#EB6E94")
            )

            colors.forEach { (colorView, colorHex) ->
                (colorView.background.mutate() as? GradientDrawable)?.setColor(
                    Color.parseColor(
                        colorHex
                    )
                )
                colorView.setOnClickListener {
                    schedule.color = Color.parseColor(colorHex)
                    (currentColorView.background.mutate() as? GradientDrawable)?.setColor(schedule.color)
                    addScheduleBlockToTimeline(schedule) // 1. 타임라인 블록을 새 색상으로 다시 그립니다.
                    onDataChanged() // 2. MainActivity에 데이터가 변경되었음을 즉시 알립니다.
                    popupWindow.dismiss()
                }
            }
            popupWindow.showAsDropDown(colorDotView)
        }
        detailAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            schedule.alarmOn= isChecked
            Toast.makeText(context, "알림 설정이 변경되었습니다.", Toast.LENGTH_SHORT).show()
        }

        editScheduleButton.setOnClickListener {
            (activity as? MainActivity)?.openEditScheduleActivity(schedule, isCopy = false)
            dismiss()
        }

        shareScheduleButton.setOnClickListener {
            showShareOptionsDialog(schedule, it)
        }

        deleteButton.setOnClickListener {
            val confirmationDialog = DeleteConfirmationDialog {
                val positionToRemove = dailySchedules.indexOf(schedule)
                if (positionToRemove != -1) {
                    (activity as? MainActivity)?.removeSchedule(schedule)
                    dailySchedules.removeAt(positionToRemove)
                    scheduleListAdapter.notifyItemRemoved(positionToRemove)
                }
                listViewContainer.visibility = View.VISIBLE
                detailViewContainer.visibility = View.GONE
                Toast.makeText(context, "'${schedule.title}' 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            confirmationDialog.show(parentFragmentManager, "DeleteConfirmationDialog")
        }
    }


    fun showShareOptionsDialog(schedule: Schedule, anchorView: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.dialog_share_options, null)
        val shareAsTextButton = popupView.findViewById<TextView>(R.id.shareAsTextButton)
        val shareAsLinkButton = popupView.findViewById<TextView>(R.id.shareAsLinkButton)

        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 20f

        shareAsTextButton.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("M월 d일 a hh:mm", Locale.KOREA)
            val scheduleText = """
                [일정 공유]
                📌 제목: ${schedule.title}
                🗓️ 날짜 & 시간: ${schedule.startDateTime?.format(formatter)} ~ ${schedule.endDateTime?.format(formatter)}
                📍 분야: ( ${schedule.category} ) / 장소: ( ${schedule.location} )
                📝 메모: ${schedule.memo}
            """.trimIndent()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("schedule", scheduleText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        shareAsLinkButton.setOnClickListener {
            val startDateTime = schedule.startDateTime
            val endDateTime = schedule.endDateTime
            if(startDateTime == null || endDateTime == null){
                Toast.makeText(context, "시간이 지정된 일정만 링크로 공유할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deepLinkUri = Uri.parse("https://mycalendar.example.com/schedule").buildUpon()
                .appendQueryParameter("title", schedule.title)
                .appendQueryParameter("start", startDateTime.toString())
                .appendQueryParameter("end", endDateTime.toString())
                .appendQueryParameter("color", schedule.color.toString())
                .appendQueryParameter("category", schedule.category)
                .appendQueryParameter("location", schedule.location)
                .appendQueryParameter("memo", schedule.memo)
                .build()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, deepLinkUri.toString())
            }
            startActivity(Intent.createChooser(intent, "일정 공유"))
            popupWindow.dismiss()
        }
        popupWindow.showAsDropDown(anchorView)
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
            if (dataChanged) onDataChanged()
        }
    }

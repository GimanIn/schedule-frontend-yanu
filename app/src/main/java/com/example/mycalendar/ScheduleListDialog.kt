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
// 파일 상단에 추가해야 할 import들
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.mycalendar.network.RetrofitClient
import com.example.mycalendar.model.ApiResponse
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
// 파일 상단에 추가
import android.util.Log
import com.example.mycalendar.mapper.ScheduleMapper
import com.example.mycalendar.model.ScheduleResponse
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.Schedule
import com.example.mycalendar.model.ScheduleRequest
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

// [스와이프] 제스처 감지를 위한 import 문 추가
import android.view.GestureDetector
import android.view.MotionEvent

private const val DEFAULT_COLOR = Color.BLUE
private const val EMPTY_STRING = ""

class ScheduleListDialog(
    private val date: LocalDate,
    private val dailySchedules: MutableList<Schedule>,
    private val onDataChanged: () -> Unit,
    private val onAddNewSchedule: (LocalDate) -> Unit,
    // ⭐️ [수정 1] 상세보기를 바로 보여줄 스케줄을 받는 파라미터 추가
    private val scheduleToShowDetailsFor: Schedule? = null
) : DialogFragment() {

    // [스와이프] 제스처 감지기와 현재 날짜를 관리할 변수 선언
    private lateinit var gestureDetector: GestureDetector
    private var currentDate: LocalDate = date // ✅ [수정] 생성자로 받은 'date'로 현재 날짜를 초기화합니다.

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



        activity?.intent?.data?.let { uri ->
            val scheduleIdParam = uri.getQueryParameter("id")
            Log.d("DeepLink", "ScheduleListDialog 딥링크 scheduleId: $scheduleIdParam")
            val scheduleId = scheduleIdParam?.toLongOrNull()

            // 🔐 인텐트 반복 호출 방지를 위해 초기화 처리
            activity?.intent?.data = null

            if (scheduleId != null) {
                fetchSharedSchedule(scheduleId)
            }
        }


        // [스와이프] 제스처 감지기 설정 및 뷰에 터치 리스너 연결
        setupGestureDetector()

        return view
    }
    // ✅ NEW: 딥링크로 들어온 일정 ID로 서버에서 일정 조회
    private fun fetchSharedSchedule(scheduleId: Long) {
        // 🔧 수정: getPublicSchedule → getSchedule
        RetrofitClient.apiService.getSchedule(scheduleId).enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<ScheduleResponse>>,
                response: Response<ApiResponse<ScheduleResponse>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val scheduleResponse = response.body()!!.data!!
                    val schedule = ScheduleMapper.toSchedule(scheduleResponse)

                    // 다이얼로그에서 일정 미리보기 표시
                    AlertDialog.Builder(requireContext())
                        .setTitle("공유된 일정")
                        .setMessage("${schedule.title}\n${schedule.startDate} ${schedule.startTime}\n\n이 일정을 복사하시겠습니까?")
                        .setPositiveButton("복사하기") { _, _ ->
                            (activity as? MainActivity)?.addScheduleToMap(schedule)
                            dailySchedules.add(schedule)
                            scheduleListAdapter.notifyItemInserted(dailySchedules.size - 1)
                            Toast.makeText(context, "공유 일정을 복사했습니다.", Toast.LENGTH_SHORT).show()
                            onDataChanged()
                        }
                        .setNegativeButton("취소", null)
                        .show()
                } else {
                    Toast.makeText(context, "일정을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                Toast.makeText(context, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // ⭐️ [수정 2] onViewCreated 추가: 뷰가 완전히 생성된 후 특정 상세보기를 바로 띄움
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 만약 상세보기를 보여줘야 할 스케줄이 있다면, 바로 showDetailView 함수를 호출
        scheduleToShowDetailsFor?.let {
            showDetailView(it)
        }
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
            onScheduleClicked = { schedule -> showDetailView(schedule) },
            onEditClicked = { schedule ->
                (activity as? MainActivity)?.openEditScheduleActivity(schedule, isCopy = false)
                dismiss()
            },
            onCopyClicked = { schedule ->
                (activity as? MainActivity)?.openEditScheduleActivity(schedule, isCopy = true)
                dismiss()
            },
            onDeleteClicked = { schedule, position ->
                (activity as? MainActivity)?.deleteScheduleFromServer(schedule) {
                    dailySchedules.removeAt(position)
                    scheduleListAdapter.notifyItemRemoved(position)
                    dataChanged = true
                }
            },
            onLinkShareClicked = { schedule ->
                schedule.id?.let { id ->
                    (activity as? MainActivity)?.shareSchedule(id, receiverId= 15L)
                } ?: run {
                    Toast.makeText(requireContext(), "공유할 수 없는 일정입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )



        scheduleListRecyclerView.layoutManager = LinearLayoutManager(context)
        scheduleListRecyclerView.adapter = scheduleListAdapter
        scheduleListAdapter.notifyDataSetChanged()

        // 1. 버튼의 동그란 배경을 코드로 새로 생성 (회색)
        val addButtonBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#F0F0F0")) // 밝은 회색 배경
        }
        addButtonDialog.background = addButtonBg

        // 2. 버튼의 '+' 아이콘 색상을 강제 설정 (어두운 회색)
        addButtonDialog.setColorFilter(Color.DKGRAY)
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
        // ⭐️ [수정] RecyclerView에 직접 터치 리스너를 추가하는 안정적인 방식으로 변경합니다.
        scheduleListRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // 터치 이벤트를 제스처 감지기에 전달합니다.
                gestureDetector.onTouchEvent(e)
                // false를 반환하여 클릭 등 다른 이벤트가 막히지 않도록 합니다.
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    // --- ⭐️ [스와이프] 핵심 기능 함수들 ⭐️ ---

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) moveToPreviousDay() else moveToNextDay()
                    return true
                }
                return false
            }
        })
    }

    private fun moveToPreviousDay() {
        currentDate = currentDate.minusDays(1)
        refreshSchedules()
    }

    private fun moveToNextDay() {
        currentDate = currentDate.plusDays(1)
        refreshSchedules()
    }

    private fun refreshSchedules() {
        // 스와이프 시 변경된 날짜로 제목과 힌트를 업데이트합니다.
        val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREA)
        titleTextView.text = "${currentDate.dayOfMonth} ${dayOfWeek}"
        scheduleEditTextDialog.hint = "${currentDate.monthValue}월 ${currentDate.dayOfMonth}일에 추가"

        // MainActivity로부터 새 날짜의 일정 데이터를 가져옵니다.
        val newSchedules = (activity as? MainActivity)?.getSchedulesForDate(currentDate)?.toMutableList() ?: mutableListOf()

        // 어댑터의 데이터를 교체하고 UI를 갱신합니다.
        dailySchedules.clear()
        dailySchedules.addAll(newSchedules)
        scheduleListAdapter.notifyDataSetChanged()
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
        // ✅ [수정] 기존 배경을 바꾸는 대신, 항상 새 동그라미 Drawable을 만들어줍니다.
        // 이렇게 하면 다른 버튼의 배경과 완전히 분리됩니다.
        val newColorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(schedule.color)
        }
        currentColorView.background = newColorDrawable

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
                    // ✅ [수정] 여기도 마찬가지로 새 Drawable을 만들어 적용합니다.
                    val selectedColorDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(schedule.color)
                    }
                    currentColorView.background = selectedColorDrawable
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

        // 1. '수정' 버튼의 동그란 배경을 코드로 새로 생성 (회색)
        val editButtonBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#F0F0F0")) // 밝은 회색 배경
        }
        editScheduleButton.background = editButtonBg

        // 2. '수정' 버튼 아이콘 색상을 강제 설정 (어두운 회색)
        editScheduleButton.setColorFilter(Color.DKGRAY)
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
                    // ✅ 서버 삭제 메서드 호출로 변경
                    (activity as? MainActivity)?.deleteScheduleFromServer(schedule) {
                        // 성공 시 UI 업데이트
                        dailySchedules.removeAt(positionToRemove)
                        scheduleListAdapter.notifyItemRemoved(positionToRemove)
                        listViewContainer.visibility = View.VISIBLE
                        detailViewContainer.visibility = View.GONE
                    }
                }
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
            // 서버에 저장되지 않은 로컬 일정은 ID가 없으므로 공유 불가 처리
            if (schedule.id == null || schedule.id == 0L) {
                Toast.makeText(context, "서버에 저장된 일정만 링크로 공유할 수 있습니다.", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
                return@setOnClickListener
            }

            // ✅ [수정] 모든 정보를 보내는 대신, 'id'만 담아서 링크 생성
            val deepLinkUri = Uri.parse("mycalendar://schedule").buildUpon()
                .appendQueryParameter("id", schedule.id.toString())
                .build()

            // 공유 시트(Share Sheet)에 표시될 텍스트와 함께 링크 전달
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "내 캘린더에서 일정을 확인해보세요!\n${deepLinkUri}")
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

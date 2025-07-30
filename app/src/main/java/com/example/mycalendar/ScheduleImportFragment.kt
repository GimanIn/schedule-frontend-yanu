package com.example.mycalendar

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mycalendar.model.Schedule
import java.time.format.DateTimeFormatter
import java.util.Locale

// MainActivity와 통신하기 위한 리스너 인터페이스
interface OnScheduleImportListener {
    fun onScheduleImport(schedule: Schedule)
}

class ScheduleImportFragment : DialogFragment() {

    private lateinit var schedule: Schedule
    private var importListener: OnScheduleImportListener? = null

    // 리스너 설정 메서드
    fun setOnScheduleImportListener(listener: OnScheduleImportListener) {
        this.importListener = listener
    }

    companion object {
        private const val ARG_SCHEDULE = "schedule"
        fun newInstance(schedule: Schedule): ScheduleImportFragment {
            val fragment = ScheduleImportFragment()
            val args = Bundle()
            args.putSerializable(ARG_SCHEDULE, schedule)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        schedule = arguments?.getSerializable(ARG_SCHEDULE) as? Schedule
            ?: run {
                Log.e("ScheduleImportFragment", "Schedule data is null, dismissing.")
                dismiss() // 스케줄 데이터가 없으면 즉시 다이얼로그를 닫습니다.
                return
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (::schedule.isInitialized) { // 스케줄이 초기화되었는지 확인
            setupImportView(view)
        }
    }

    private fun setupImportView(view: View) {
        // 1. 뷰 컴포넌트 찾기
        val listViewContainer: View = view.findViewById(R.id.listViewContainer)
        val detailViewContainer: View = view.findViewById(R.id.detailViewContainer)
        val backToListButton: ImageButton = view.findViewById(R.id.backToListButton)
        val detailTitleText: TextView = view.findViewById(R.id.titleTextView) // 제목 뷰 추가
        val detailDateText: TextView = view.findViewById(R.id.detailDateText)
        val detailMemoText: TextView = view.findViewById(R.id.detailMemoText)
        val importButton: Button = view.findViewById(R.id.deleteButton) // '삭제' 버튼을 '가져오기'로 재활용
        val hoursContainer: LinearLayout = view.findViewById(R.id.hoursContainer)
        val scheduleBlocksContainer: FrameLayout = view.findViewById(R.id.scheduleBlocksContainer)
        val currentColorView: View = detailViewContainer.findViewById(R.id.currentColorView)
        val editScheduleButton: ImageButton = detailViewContainer.findViewById(R.id.editScheduleButton)

        val shareScheduleButton: ImageButton = view.findViewById(R.id.shareScheduleButton)

        // 2. UI 상태 설정
        listViewContainer.visibility = View.GONE
        detailViewContainer.visibility = View.VISIBLE
        editScheduleButton.visibility = View.GONE // 원본 수정 버튼은 숨기기
        backToListButton.setOnClickListener { dismiss() }

        // ✅ [추가] 공유 버튼을 숨깁니다.
        shareScheduleButton.visibility = View.GONE

        // 3. '가져오기' 버튼 설정
        importButton.text = "가져오기"
        importButton.setBackgroundColor(Color.parseColor("#4285F4"))
        importButton.setOnClickListener {
            importListener?.onScheduleImport(schedule)
            dismiss()
        }

        // 4. UI에 일정 데이터 채우기
        val dayOfWeek = schedule.startDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREA)
        detailTitleText.text = "${schedule.startDate.dayOfMonth} ${dayOfWeek}" // 상단 날짜 제목 설정

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
        detailDateText.text = schedule.startDate.format(dateFormatter)

        val detailsText = StringBuilder()
        if (!schedule.category.isNullOrBlank()) detailsText.append("카테고리: ${schedule.category}\n")
        if (!schedule.location.isNullOrBlank()) detailsText.append("장소: ${schedule.location}\n")
        if (schedule.memo.isNotBlank()) detailsText.append("메모: ${schedule.memo}")
        detailMemoText.text = detailsText.toString().trim()

        val newColorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(schedule.color)
        }
        currentColorView.background = newColorDrawable

        // 5. 타임라인 그리기 (수정된 함수 호출)
        populateTimeline(hoursContainer)
        addScheduleBlockToTimeline(schedule, scheduleBlocksContainer)
    }

    // ▼▼▼▼▼▼▼▼▼▼ 여기에 수정된 함수들을 넣어주세요 ▼▼▼▼▼▼▼▼▼▼

    private val hourHeightDp = 60

    private fun populateTimeline(hoursContainer: LinearLayout) {
        hoursContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (hour in 0..23) {
            val hourLineView = inflater.inflate(R.layout.item_hour_line, hoursContainer, false)
            val hourTextView = hourLineView.findViewById<TextView>(R.id.hourTextView)
            hourTextView.text = String.format("%02d:00", hour)
            hoursContainer.addView(hourLineView)
        }
    }

    private fun addScheduleBlockToTimeline(
        schedule: Schedule,
        scheduleBlocksContainer: FrameLayout
    ) {
        scheduleBlocksContainer.removeAllViews()

        val startDateTime = schedule.startDateTime
        val endDateTime = schedule.endDateTime

        if (startDateTime == null || endDateTime == null) {
            // 하루 종일 일정 처리 (필요 시 구현)
            return
        }

        val startTotalHours = startDateTime.hour + startDateTime.minute / 60.0
        val endTotalHours = endDateTime.hour + endDateTime.minute / 60.0
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
    // ▲▲▲▲▲▲▲▲▲▲ 여기까지 ▲▲▲▲▲▲▲▲▲▲

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val height = (displayMetrics.heightPixels * 0.7).toInt()
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}
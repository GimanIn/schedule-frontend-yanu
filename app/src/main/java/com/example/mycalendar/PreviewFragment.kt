package com.example.mycalendar

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.mycalendar.model.*
import com.example.mycalendar.mapper.ScheduleMapper

import com.example.mycalendar.network.RetrofitClient
import com.google.android.material.switchmaterial.SwitchMaterial
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

class PreviewFragment : DialogFragment() {

    // ✅ 딥링크로 전달된 일정 객체
    private var schedule: Schedule? = null

    // ✅ UI 요소들
    private lateinit var copyButton: Button
    private lateinit var detailDateText: TextView
    private lateinit var memoEditText: EditText


    private lateinit var alarmSwitch: SwitchMaterial
    private lateinit var currentColorView: View
    private lateinit var scheduleBlocksContainer: FrameLayout

    // ✅ 색상 선택 상태 저장용
    private var selectedColor: String = "#2196F3"

    // ✅ MainActivity로 복사된 일정 전달할 콜백 리스너
    private var onScheduleCopiedListener: MainActivity.OnScheduleCopiedListener? = null

    // ✅ 외부(MainActivity)에서 리스너 등록용 함수
    fun setOnScheduleCopiedListener(listener: MainActivity.OnScheduleCopiedListener) {
        this.onScheduleCopiedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        schedule = arguments?.getSerializable("schedule") as? Schedule
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_preview, container, false)


        // ✅ UI 연결
        copyButton = view.findViewById(R.id.copyButton)
        detailDateText = view.findViewById(R.id.detailDateText)
        memoEditText = view.findViewById(R.id.detailMemoEdit)
        alarmSwitch = view.findViewById(R.id.detailAlarmSwitch)
        currentColorView = view.findViewById(R.id.currentColorView)
        scheduleBlocksContainer = view.findViewById(R.id.scheduleBlocksContainer)

        // ✅ 전달받은 일정 정보로 화면 채우기
        schedule?.let { it ->
            val dayOfWeek = it.startDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREA)
            detailDateText.text = "${it.startDate} (${dayOfWeek}) ${it.startTime ?: "?"} ~ ${it.endTime ?: "?"}"

            memoEditText.setText(it.memo ?: "")
            alarmSwitch.isChecked = it.alarmOn

            selectedColor = String.format("#%06X", 0xFFFFFF and it.color)
            currentColorView.setBackgroundColor(Color.parseColor(selectedColor))

            renderTimeBlock(it.startTime, it.endTime)
        }

        // ✅ 색상 선택기 클릭 리스너
        currentColorView.setOnClickListener {
            showColorPickerDialog()
        }

        // ✅ 가져오기 버튼 클릭 시 일정 복사 요청
        copyButton.setOnClickListener {
            copyScheduleToMyCalendar()
        }

        return view
    }

    // ✅ 시간 블럭 시각화 (회색 타임라인 위에 칼라로 표시)
    private fun renderTimeBlock(start: LocalTime?, end: LocalTime?) {
        scheduleBlocksContainer.removeAllViews()

        val startHour = start?.hour ?: 9
        val endHour = end?.hour ?: 10
        val durationHours = (endHour - startHour).coerceAtLeast(1)
        val blockHeight = durationHours * 100 // 1시간 = 100dp 기준

        val block = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                blockHeight
            )
            setBackgroundColor(Color.parseColor(selectedColor))
        }

        scheduleBlocksContainer.addView(block)
    }

    // ✅ 색상 선택 다이얼로그
    private fun showColorPickerDialog() {
        val colors = arrayOf("#F44336", "#4CAF50", "#2196F3", "#FF9800", "#9C27B0")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("색상 선택")
        builder.setItems(colors) { _, which ->
            selectedColor = colors[which]
            currentColorView.setBackgroundColor(Color.parseColor(selectedColor))
        }
        builder.show()
    }

    // ✅ 서버에 일정 복사 요청 보내고 MainActivity에 전달
    private fun copyScheduleToMyCalendar() {
        schedule?.let {
            val request = ScheduleRequest(
                title = it.title,
                memo = memoEditText.text.toString(),
                location = "",
                category = "",
                scheduledDate = it.startDate.toString(),
                startDate = it.startDate.toString(),
                endDate = it.endDate.toString(),
                startTime = it.startTime?.toString() ?: "09:00",
                endTime = it.endTime?.toString() ?: "10:00",
                allDay = false,
                isConfirmed = it.isConfirmed,
                color = selectedColor,
                alarmOn = alarmSwitch.isChecked,
                copiedFromScheduleId = it.id
            )

            RetrofitClient.apiService.createSchedule(request)
                .enqueue(object : Callback<ApiResponse<ScheduleResponse>> {
                    override fun onResponse(
                        call: Call<ApiResponse<ScheduleResponse>>,
                        response: Response<ApiResponse<ScheduleResponse>>
                    ) {
                        if (response.isSuccessful && response.body()?.data != null) {
                            val copiedSchedule = ScheduleMapper.toSchedule(response.body()!!.data!!)
                            // ✅ MainActivity로 복사된 일정 전달
                            onScheduleCopiedListener?.onScheduleCopied(copiedSchedule)
                            Toast.makeText(requireContext(), "내 일정에 복사되었습니다.", Toast.LENGTH_SHORT).show()
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), "복사 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<ScheduleResponse>>, t: Throwable) {
                        Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    // ✅ 정적 팩토리 메서드: 딥링크 일정 받아서 프래그먼트 생성
    companion object {
        fun newInstance(schedule: Schedule): PreviewFragment {
            val fragment = PreviewFragment()
            val args = Bundle().apply {
                putSerializable("schedule", schedule)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

}
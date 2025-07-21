package com.example.mycalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog

class ScheduleListAdapter(
    private val scheduleList: List<Schedule>,
    private val fragmentManager: FragmentManager,
    private val onScheduleClicked: (Schedule) -> Unit,
    private val onEditClicked: (Schedule) -> Unit,
    private val onCopyClicked: (Schedule) -> Unit,
    private val onDeleteClicked: (Schedule, Int) -> Unit
) : RecyclerView.Adapter<ScheduleListAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorBar: View = itemView.findViewById(R.id.colorBarView)
        val timeText: TextView = itemView.findViewById(R.id.timeTextView)
        val titleText: TextView = itemView.findViewById(R.id.titleTextView)
        val timeRangeText: TextView = itemView.findViewById(R.id.timeRangeTextView)

        fun bind(schedule: Schedule, position: Int) {
            titleText.text = schedule.title
            colorBar.setBackgroundColor(schedule.color)

            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            // schedule.startTime과 schedule.endTime을 모두 새 변수 이름으로 변경합니다.
            // 변경될 수 있는 var 변수를 변경 불가능한 val 지역 변수에 담아서 사용합니다.
            val startDateTime = schedule.startDateTime
            val endDateTime = schedule.endDateTime

            if (startDateTime != null) {
                timeText.text = startDateTime.format(formatter)
                timeText.visibility = View.VISIBLE

                if (endDateTime != null) {
                    timeRangeText.text = "${startDateTime.format(formatter)} - ${endDateTime.format(formatter)}"
                    timeRangeText.visibility = View.VISIBLE
                } else {
                    timeRangeText.visibility = View.GONE
                }
            } else {
                timeText.visibility = View.GONE
                timeRangeText.visibility = View.GONE
            }

            itemView.setOnClickListener { onScheduleClicked(schedule) }

            // 길게 누르기(Long Press) 리스너
            itemView.setOnLongClickListener { view ->
                val inflater = LayoutInflater.from(view.context)
                val popupView = inflater.inflate(R.layout.dialog_custom_menu, null)

                // 1. PopupWindow를 생성합니다.
                val popupWindow = PopupWindow(
                    popupView,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true // 바깥 영역 터치 시 닫히도록 설정
                )

                // 2. 팝업의 스타일을 설정합니다. (elevation을 주어 입체감 있게)
                popupWindow.elevation = 20f

                // 3. 팝업 안의 각 버튼에 대한 클릭 리스너를 설정합니다.
                val menuEdit = popupView.findViewById<TextView>(R.id.menu_edit)
                val menuCopy = popupView.findViewById<TextView>(R.id.menu_copy)
                val menuShare = popupView.findViewById<TextView>(R.id.menu_share)
                val menuDelete = popupView.findViewById<TextView>(R.id.menu_delete)

                menuEdit.setOnClickListener {
                    onEditClicked(schedule)
                    popupWindow.dismiss()
                }
                menuCopy.setOnClickListener {
                    onCopyClicked(schedule)
                    popupWindow.dismiss()
                }
                menuShare.setOnClickListener {
                    Toast.makeText(view.context, "공유 기능 구현 예정", Toast.LENGTH_SHORT).show()
                    popupWindow.dismiss()
                }
                menuDelete.setOnClickListener {
                    val confirmationDialog = DeleteConfirmationDialog {
                        onDeleteClicked(schedule, position)
                    }
                    confirmationDialog.show(fragmentManager, "DeleteConfirmationDialog")
                    popupWindow.dismiss()
                }

                // 4. 꾹 누른 view를 기준으로 팝업을 보여줍니다.
                popupWindow.showAsDropDown(view)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleList[position], position)
    }

    override fun getItemCount(): Int = scheduleList.size
}
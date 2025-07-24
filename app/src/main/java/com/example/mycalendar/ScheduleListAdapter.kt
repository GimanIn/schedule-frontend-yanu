package com.example.mycalendar

import android.content.*
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycalendar.model.Schedule
import java.time.format.DateTimeFormatter
import java.util.*

class ScheduleListAdapter(
    private val scheduleList: List<Schedule>,
    private val fragmentManager: FragmentManager,
    private val onScheduleClicked: (Schedule) -> Unit,
    private val onEditClicked: (Schedule) -> Unit,
    private val onCopyClicked: (Schedule) -> Unit,
    private val onDeleteClicked: (Schedule, Int) -> Unit
) : RecyclerView.Adapter<ScheduleListAdapter.ScheduleViewHolder>() {

    companion object {
        private const val SCHEME = "mycalendar"                      // ✅ 딥링크 스킴
        private const val HOST = "schedule"                          // ✅ 딥링크 호스트
        private const val TIME_FORMAT = "HH:mm"                      // ✅ 시간 포맷
        private const val SHARE_TIME_FORMAT = "M월 d일 a hh:mm"      // ✅ 공유용 시간 포맷
        private const val DEFAULT_SHARE_TITLE = "[일정 공유]"        // ✅ 공유 텍스트 제목
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorBar: View = itemView.findViewById(R.id.colorBarView)
        val timeText: TextView = itemView.findViewById(R.id.timeTextView)
        val titleText: TextView = itemView.findViewById(R.id.titleTextView)
        val timeRangeText: TextView = itemView.findViewById(R.id.timeRangeTextView)

        fun bind(schedule: Schedule, position: Int) {
            titleText.text = schedule.title
            colorBar.setBackgroundColor(schedule.color)

            val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
            val startDateTime = schedule.startTime?.let { schedule.startDate.atTime(it) }
            val endDateTime = schedule.endTime?.let { schedule.startDate.atTime(it) }

            // 시간 표시
            if (startDateTime != null) {
                timeText.text = startDateTime.format(timeFormatter)
                timeText.visibility = View.VISIBLE

                if (endDateTime != null) {
                    timeRangeText.text = "${startDateTime.format(timeFormatter)} - ${endDateTime.format(timeFormatter)}"
                    timeRangeText.visibility = View.VISIBLE
                } else {
                    timeRangeText.visibility = View.GONE
                }
            } else {
                timeText.visibility = View.GONE
                timeRangeText.visibility = View.GONE
            }

            itemView.setOnClickListener { onScheduleClicked(schedule) }

            itemView.setOnLongClickListener { view ->
                val inflater = LayoutInflater.from(view.context)
                val popupView = inflater.inflate(R.layout.dialog_custom_menu, null)

                val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                    elevation = 20f
                }

                popupView.findViewById<TextView>(R.id.menu_edit).setOnClickListener {
                    onEditClicked(schedule)
                    popupWindow.dismiss()
                }

                popupView.findViewById<TextView>(R.id.menu_copy).setOnClickListener {
                    onCopyClicked(schedule)
                    popupWindow.dismiss()
                }

                popupView.findViewById<TextView>(R.id.menu_share).setOnClickListener {
                    popupWindow.dismiss()

                    val sharePopupView = inflater.inflate(R.layout.dialog_share_options, null)
                    val sharePopupWindow = PopupWindow(sharePopupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                        elevation = 20f
                    }

                    val shareFormatter = DateTimeFormatter.ofPattern(SHARE_TIME_FORMAT, Locale.KOREA)
                    val startForShare = schedule.startTime?.let { schedule.startDate.atTime(it) }
                    val endForShare = schedule.endTime?.let { schedule.startDate.atTime(it) }

                    sharePopupView.findViewById<TextView>(R.id.shareAsTextButton).setOnClickListener {
                        val scheduleText = """
                            $DEFAULT_SHARE_TITLE
                            📌 제목: ${schedule.title}
                            🗓️ 날짜 & 시간: ${startForShare?.format(shareFormatter)} ~ ${endForShare?.format(shareFormatter)}
                            📍 분야: ( ${schedule.category} ) / 장소: ( ${schedule.location} )
                            📝 메모: ${schedule.memo}
                        """.trimIndent()

                        val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("schedule", scheduleText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(view.context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
                        sharePopupWindow.dismiss()
                    }

                    sharePopupView.findViewById<TextView>(R.id.shareAsLinkButton).setOnClickListener {
                        if (startForShare == null || endForShare == null) {
                            Toast.makeText(view.context, "시간이 지정된 일정만 링크로 공유할 수 있습니다.", Toast.LENGTH_SHORT).show()
                            sharePopupWindow.dismiss()
                            return@setOnClickListener
                        }

                        val deepLinkUri = Uri.Builder()
                            .scheme(SCHEME)
                            .authority(HOST)
                            .appendQueryParameter("title", schedule.title)
                            .appendQueryParameter("start", startForShare.toString())
                            .appendQueryParameter("end", endForShare.toString())
                            .appendQueryParameter("color", schedule.color.toString())
                            .appendQueryParameter("memo", schedule.memo ?: "")
                            .build()

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, deepLinkUri.toString())
                        }
                        view.context.startActivity(Intent.createChooser(intent, "일정 공유"))
                        sharePopupWindow.dismiss()
                    }

                    sharePopupWindow.showAsDropDown(view)
                }

                popupView.findViewById<TextView>(R.id.menu_delete).setOnClickListener {
                    DeleteConfirmationDialog {
                        onDeleteClicked(schedule, position)
                    }.show(fragmentManager, "DeleteConfirmationDialog")
                    popupWindow.dismiss()
                }

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

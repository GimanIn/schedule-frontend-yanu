package com.example.mycalendar

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import java.time.LocalDate
import android.view.LayoutInflater
import android.widget.Toast
import com.example.mycalendar.model.Schedule  // ✅ NEW: Schedule import 추가


class SearchActivity : AppCompatActivity() {

    private lateinit var allSchedules: List<Schedule>
    private lateinit var searchAdapter: SearchListAdapter
    private var filterStartDate: LocalDate? = null
    private var filterEndDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // MainActivity로부터 전체 일정 목록을 받아옵니다.
        allSchedules = (intent.getSerializableExtra("allSchedules") as? ArrayList<Schedule> ?: emptyList())
            .sortedWith(compareBy({ it.startDate }, { it.startTime })) // ✅ NEW: 날짜 + 시간 순 정렬

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val searchResultsRecyclerView = findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        val noResultsLayout = findViewById<LinearLayout>(R.id.noResultsLayout)
        val dateFilterButton = findViewById<Button>(R.id.dateFilterButton)
        val categoryFilterSpinner = findViewById<Spinner>(R.id.categoryFilterSpinner)

        // 어댑터 설정
        searchAdapter = SearchListAdapter(allSchedules)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = searchAdapter

        dateFilterButton.setOnClickListener {
            showDateRangePickerDialog()
        }

        // 카테고리는 메모에서 추출 (임시)
        val categories = listOf("모든 분야") + allSchedules.map {
            it.memo?.split("\n")?.getOrNull(0)?.replace("카테고리: ", "")
        }.distinct()

        categoryFilterSpinner.adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        cancelButton.setOnClickListener {
            finish()
        }

        // 검색창 텍스트 변경 리스너
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterSchedules()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 초기 목록 표시
        filterSchedules()
    }

    private fun showDateRangePickerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_range_picker, null)

        // --- 👇 모든 NumberPicker를 찾고 설정하는 코드를 완성합니다 ---
        val startYearPicker = dialogView.findViewById<NumberPicker>(R.id.startYearPicker)
        val startMonthPicker = dialogView.findViewById<NumberPicker>(R.id.startMonthPicker)
        val startDayPicker = dialogView.findViewById<NumberPicker>(R.id.startDayPicker)
        val endYearPicker = dialogView.findViewById<NumberPicker>(R.id.endYearPicker)
        val endMonthPicker = dialogView.findViewById<NumberPicker>(R.id.endMonthPicker)
        val endDayPicker = dialogView.findViewById<NumberPicker>(R.id.endDayPicker)

        val now = LocalDate.now()

        // 시작 날짜 Picker 설정
        startYearPicker.minValue = now.year - 5
        startYearPicker.maxValue = now.year + 5
        startYearPicker.value = filterStartDate?.year ?: now.year

        startMonthPicker.minValue = 1
        startMonthPicker.maxValue = 12
        startMonthPicker.value = filterStartDate?.monthValue ?: now.monthValue

        startDayPicker.minValue = 1
        startDayPicker.maxValue = 31 // TODO: 월마다 다른 최대 일수 적용 필요
        startDayPicker.value = filterStartDate?.dayOfMonth ?: now.dayOfMonth

        // 종료 날짜 Picker 설정
        endYearPicker.minValue = now.year - 5
        endYearPicker.maxValue = now.year + 5
        endYearPicker.value = filterEndDate?.year ?: now.year

        endMonthPicker.minValue = 1
        endMonthPicker.maxValue = 12
        endMonthPicker.value = filterEndDate?.monthValue ?: now.monthValue

        endDayPicker.minValue = 1
        endDayPicker.maxValue = 31 // TODO: 월마다 다른 최대 일수 적용 필요
        endDayPicker.value = filterEndDate?.dayOfMonth ?: now.dayOfMonth

        AlertDialog.Builder(this)
            .setTitle("날짜 범위 선택")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                try {
                    filterStartDate = LocalDate.of(startYearPicker.value, startMonthPicker.value, startDayPicker.value)
                    filterEndDate = LocalDate.of(endYearPicker.value, endMonthPicker.value, endDayPicker.value)
                    // 날짜 필터 버튼의 텍스트도 변경
                    findViewById<Button>(R.id.dateFilterButton).text = "${filterStartDate?.year}년 ${filterStartDate?.monthValue}월 ${filterStartDate?.dayOfMonth}일 ~"
                    filterSchedules() // 필터 적용
                } catch (e: Exception) {
                    Toast.makeText(this, "유효하지 않은 날짜입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("초기화") { _, _ ->
                filterStartDate = null
                filterEndDate = null
                findViewById<Button>(R.id.dateFilterButton).text = "날짜"
                filterSchedules()
            }
            .show()
    }

    private fun filterSchedules() {
        val query = findViewById<EditText>(R.id.searchEditText).text.toString().lowercase()

        val filteredList = allSchedules.filter { schedule ->
            val textMatch = schedule.title.lowercase().contains(query) ||
                    (schedule.memo?.lowercase()?.contains(query) ?: false)

            // 변경 가능한 var 속성을 변경 불가능한 val 지역 변수에 복사
            val startDate = schedule.startDate
            val dateMatch = if (filterStartDate != null && filterEndDate != null) {
                !startDate.isBefore(filterStartDate) && !startDate.isAfter(filterEndDate)
            } else {
                true
            }

            textMatch && dateMatch // 두 조건을 모두 만족하는 것만
        }

        searchAdapter.updateData(filteredList)

        // 결과 유무에 따라 화면 전환
        if (filteredList.isEmpty()) {
            findViewById<RecyclerView>(R.id.searchResultsRecyclerView).visibility = View.GONE
            findViewById<LinearLayout>(R.id.noResultsLayout).visibility = View.VISIBLE
        } else {
            findViewById<RecyclerView>(R.id.searchResultsRecyclerView).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.noResultsLayout).visibility = View.GONE
        }
    }
}
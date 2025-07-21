package com.example.mycalendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DeleteConfirmationDialog(
    private val onConfirm: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_delete_confirmation, container, false)
        val noButton = view.findViewById<Button>(R.id.noButton)
        val yesButton = view.findViewById<Button>(R.id.yesButton)

        noButton.setOnClickListener {
            dismiss() // 팝업 닫기
        }

        yesButton.setOnClickListener {
            onConfirm() // 삭제 확인 동작 실행
            dismiss()   // 팝업 닫기
        }
        return view
    }
}
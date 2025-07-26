package com.example.mycalendar

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var etCurrentPw: EditText
    private lateinit var etNewPw: EditText
    private lateinit var etConfirmPw: EditText
    private lateinit var btnSubmit: MaterialButton

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        etCurrentPw = findViewById(R.id.etCurrentPw)
        etNewPw = findViewById(R.id.etNewPw)
        etConfirmPw = findViewById(R.id.etConfirmPw)
        btnSubmit = findViewById(R.id.btnChangePw)

        // 뒤로 가기 버튼 클릭
        val backButoon = findViewById<ImageButton>(R.id.btn_back)
        backButoon.setOnClickListener {
            finish() // 현재 액티비티 종료 -> 이전 화면으로 돌아감
        }

        btnSubmit.setOnClickListener {
            val current = etCurrentPw.text.toString()
            val newPw = etNewPw.text.toString()
            val confirm = etConfirmPw.text.toString()
            val savedPw = sharedPref.getString("password", "")

            if (current != savedPw) {
                Toast.makeText(this, "현재 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
            } else if (newPw != confirm) {
                Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            } else if (!isValidPassword(newPw)) {
                Toast.makeText(this, "비밀번호는 영문/숫자/특수문자 중 2종 이상 조합, 8~16자입니다.", Toast.LENGTH_LONG).show()
            } else {
                sharedPref.edit().putString("password", newPw).apply()
                Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val hasLower = password.contains(Regex("[a-z]"))
        val hasUpper = password.contains(Regex("[A-Z]"))
        val hasDigit = password.contains(Regex("[0-9]"))
        val hasSpecial = password.contains(Regex("[^a-zA-Z0-9]"))

        val validCount = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }

        return password.length in 8..16 && validCount >= 2
    }
}
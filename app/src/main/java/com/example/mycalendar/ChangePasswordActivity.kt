package com.example.mycalendar

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

// ✅ 서버 연동 관련 import
import com.example.mycalendar.model.ChangePasswordRequest
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

        btnSubmit.setOnClickListener {
            val current = etCurrentPw.text.toString()
            val newPw = etNewPw.text.toString()
            val confirm = etConfirmPw.text.toString()

            // ✅ 입력값 유효성 검사
            if (newPw != confirm) {
                Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            } else if (!isValidPassword(newPw)) {
                Toast.makeText(this, "비밀번호는 영문/숫자/특수문자 중 2종 이상 조합, 8~16자입니다.", Toast.LENGTH_LONG).show()
            } else {
                val token = sharedPref.getString("accessToken", "") ?: ""
                val request = ChangePasswordRequest(current, newPw)

                // ✅ 서버로 비밀번호 변경 요청
                RetrofitClient.apiService.changePassword("Bearer $token", request)
                    .enqueue(object : Callback<ApiResponse<Unit>> {
                        override fun onResponse(
                            call: Call<ApiResponse<Unit>>,
                            response: Response<ApiResponse<Unit>>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "비밀번호가 변경되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            } else {
                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "현재 비밀번호가 틀렸습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                            Toast.makeText(
                                this@ChangePasswordActivity,
                                "서버 오류: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
        }
    }

    // ✅ 비밀번호 유효성 검사 함수
    private fun isValidPassword(password: String): Boolean {
        val hasLower = password.contains(Regex("[a-z]"))
        val hasUpper = password.contains(Regex("[A-Z]"))
        val hasDigit = password.contains(Regex("[0-9]"))
        val hasSpecial = password.contains(Regex("[^a-zA-Z0-9]"))

        val validCount = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }

        return password.length in 8..16 && validCount >= 2
    }
}

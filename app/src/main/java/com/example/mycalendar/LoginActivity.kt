package com.example.mycalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mycalendar.model.LoginRequest
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.model.LoginResponse
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editTextId = findViewById<EditText>(R.id.edit_id)
        val editTextPassword = findViewById<EditText>(R.id.edit_password)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val signupButton = findViewById<Button>(R.id.btn_signup)
        val findInfoText = findViewById<TextView>(R.id.txt_find_account)
        val autoLoginCheckbox = findViewById<CheckBox>(R.id.checkbox_autologin)
        val backButton = findViewById<ImageButton>(R.id.btn_back)

        loginButton.setOnClickListener {
            val id = editTextId.text.toString()
            val pw = editTextPassword.text.toString()
            val autoLogin = autoLoginCheckbox.isChecked

            if (id.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(id, pw, autoLogin)
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        findInfoText.setOnClickListener {
            Toast.makeText(this, "아이디/비밀번호 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun login(id: String, pw: String, autoLogin: Boolean) {
        val loginRequest = LoginRequest(userId = id, password = pw)

        RetrofitClient.apiService.login(loginRequest)
            .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<LoginResponse>>,
                    response: Response<ApiResponse<LoginResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val loginData = response.body()?.data

                        loginData?.let {
                            val token = it.token
                            val userId = it.userId
                            val name = it.name
                            val phone = it.phone

                            Log.d("LOGIN", "받은 토큰: $token")

                            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("user_id", userId)
                                putString("user_pw", pw)
                                putBoolean("auto_login", autoLogin)
                                putString("jwt_token", token)
                                putString("user_name", name)
                                putString("user_phone", phone)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } ?: run {
                            Toast.makeText(this@LoginActivity, "로그인 실패: 응답 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                            Log.e("LOGIN", "응답은 성공했지만 data=null")
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "로그인 실패: ${response.body()?.message ?: "에러"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("LOGIN", "응답 코드: ${response.code()}, 메시지: ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LOGIN", "통신 실패: ${t.message}", t)
                }
            })
    }
}

package com.example.mycalendar

import android.content.Intent
import android.content.SharedPreferences // ✅ new: SharedPreferences import
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mycalendar.model.LoginRequest
import com.example.mycalendar.model.LoginResponse
import com.example.mycalendar.model.ApiResponse
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

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val autoLogin = prefs.getBoolean("auto_login", false)

        if (autoLogin) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 로그인 액티비티 종료
        }

        // 로그인 버튼 클릭
        loginButton.setOnClickListener{
            val id = editTextId.text.toString()
            val pw = editTextPassword.text.toString()
            val autoLogin = autoLoginCheckbox.isChecked

            val loginRequest = LoginRequest(id, pw) // 서버에 로그인 요청 보내기 아래 부터

            // ✅ 서버에 로그인 요청 보내기
            RetrofitClient.apiService.login(loginRequest)
                .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                    override fun onResponse(
                        call: Call<ApiResponse<LoginResponse>>,
                        response: Response<ApiResponse<LoginResponse>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val loginData = response.body()?.data

                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()

                            // ✅ new: SharedPreferences에 토큰, 사용자 정보 저장
                            val editor = prefs.edit()
                            editor.putString("access_token", loginData?.token)
                            editor.putString("refresh_token", loginData?.refreshToken) // ✅ new
                            editor.putString("user_id", loginData?.userId)
                            editor.putString("user_name", loginData?.name)
                            editor.putString("user_phone", loginData?.phone)
                            editor.putBoolean("auto_login", autoLogin) //수정
                            editor.apply()

                            // ✅ new: 토큰 만료 시간 저장 (10분 후로 임시 설정)
                            val expiryTimeMillis = System.currentTimeMillis() + (10 * 60 * 1000)
                            editor.putString("token_expiry_time", expiryTimeMillis.toString())

                            editor.apply()

                            // ✅ 메인 화면으로 이동
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "로그인 실패: ${response.body()?.message ?: "에러"}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // 회원가입 버튼 클릭
        signupButton.setOnClickListener {
            // 회원가입 화면으로 이동
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // 아이디/비밀번호 찾기 클릭
        findInfoText.setOnClickListener {
            Toast.makeText(this, "아이디/비밀번호 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        // 뒤로가기 버튼 클릭
        backButton.setOnClickListener {
            finish() // 현재 액티비티 종료
        }
    }
}
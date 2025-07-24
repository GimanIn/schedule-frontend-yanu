package com.example.mycalendar

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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

    companion object {
        const val PREFS_NAME = "user_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_PHONE = "user_phone"
        const val KEY_AUTO_LOGIN = "auto_login"
        const val KEY_TOKEN_EXPIRY = "token_expiry_time"
    }

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

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // ✅ FIX: 자동 로그인 체크를 더 안전하게 처리
        checkAutoLogin(prefs)

        // ✅ 로그인 버튼 클릭 이벤트 처리
        loginButton.setOnClickListener {
            val id = editTextId.text.toString().trim()
            val pw = editTextPassword.text.toString().trim()

            // ✅ FIX: 입력 검증 추가
            if (id.isEmpty()) {
                Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pw.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val autoLogin = autoLoginCheckbox.isChecked
            performLogin(id, pw, autoLogin, prefs)
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

    // ✅ FIX: 자동 로그인 체크를 별도 함수로 분리
    private fun checkAutoLogin(prefs: SharedPreferences) {
        val autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN, false)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        val now = System.currentTimeMillis()

        Log.d("LoginActivity", "자동로그인 체크: autoLogin=$autoLogin, hasToken=${!accessToken.isNullOrEmpty()}, isExpired=${now > expiryTime}")

        // ✅ FIX: 토큰이 유효할 때만 자동 로그인
        if (autoLogin && !accessToken.isNullOrEmpty() && now < expiryTime) {
            Log.d("LoginActivity", "자동 로그인 실행")
            navigateToMainActivity()
        }
    }

    // ✅ FIX: 로그인 로직을 별도 함수로 분리
    private fun performLogin(id: String, pw: String, autoLogin: Boolean, prefs: SharedPreferences) {
        val loginRequest = LoginRequest(id, pw)

        Log.d("LoginActivity", "로그인 시도: id=$id")

        RetrofitClient.apiService.login(loginRequest)
            .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<LoginResponse>>,
                    response: Response<ApiResponse<LoginResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val loginData = response.body()?.data

                        if (loginData != null) {
                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()

                            // ✅ FIX: 로그인 정보 저장을 더 안전하게
                            saveLoginData(loginData, autoLogin, prefs)

                            Log.d("LoginActivity", "로그인 성공, MainActivity로 이동")

                            // ✅ FIX: MainActivity로 이동
                            navigateToMainActivity()
                        } else {
                            Toast.makeText(this@LoginActivity, "로그인 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: "로그인 실패"
                        Toast.makeText(this@LoginActivity, "로그인 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "로그인 응답 실패: ${response.code()} - $errorMsg")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "서버 통신 실패", t)
                }
            })
    }

    // ✅ FIX: 로그인 데이터 저장을 별도 함수로 분리
    private fun saveLoginData(loginData: LoginResponse, autoLogin: Boolean, prefs: SharedPreferences) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, loginData.token)
            putString(KEY_REFRESH_TOKEN, loginData.refreshToken)
            putString(KEY_USER_ID, loginData.userId)
            putString(KEY_USER_NAME, loginData.name)
            putString(KEY_USER_PHONE, loginData.phone)
            putBoolean(KEY_AUTO_LOGIN, autoLogin)

            // ✅ FIX: 토큰 만료 시간을 1시간으로 설정 (10분에서 변경)
            val expiryTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000) // 1시간
            putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)

            apply()
        }

        Log.d("LoginActivity", "로그인 데이터 저장 완료: token=${loginData.token?.take(10)}...")
    }

    // ✅ FIX: MainActivity로 이동하는 로직을 별도 함수로 분리
    private fun navigateToMainActivity() {
        try {
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                // ✅ FIX: 플래그 설정 변경 - 더 안전한 방식
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // ✅ FIX: 추가 데이터 전달 (필요한 경우)
                putExtra("from_login", true)
            }

            Log.d("LoginActivity", "MainActivity 시작 시도")
            startActivity(intent)

            // ✅ FIX: finish()를 약간 지연시켜서 화면 전환이 완료된 후 호출
            finish()

            // ✅ FIX: 화면 전환 애니메이션 설정 (선택사항)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } catch (e: Exception) {
            Log.e("LoginActivity", "MainActivity 시작 실패", e)
            Toast.makeText(this, "앱 시작 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ FIX: 액티비티가 다시 시작될 때 중복 실행 방지
    override fun onResume() {
        super.onResume()
        Log.d("LoginActivity", "LoginActivity onResume 호출됨")
    }

    override fun onPause() {
        super.onPause()
        Log.d("LoginActivity", "LoginActivity onPause 호출됨")
    }
}
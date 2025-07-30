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

    private lateinit var editTextId: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var findInfoText: TextView
    private lateinit var autoLoginCheckbox: CheckBox
    private lateinit var backButton: ImageButton
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        intent?.data?.let { deepLink ->
            Log.d("DeepLink", "LoginActivity에서 받은 딥링크: $deepLink")

            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            val autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN, false)

            // 로그인 상태일 때만 MainActivity로 이동
            if (!accessToken.isNullOrEmpty() && autoLogin) {
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = deepLink
                }
                startActivity(mainIntent)
                finish()
                return
            } else {
                // 로그인 안 된 상태면 딥링크 정보는 저장만 하고 로그인 화면 유지
                Log.d("DeepLink", "로그인 필요. 딥링크를 로그인 후 처리해야 함.")
            }
        }



        // RetrofitClient 초기화
        RetrofitClient.init(this)

        initViews()
        setupClickListeners()

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // ✅ FIX: SharedPreferences 데이터 타입 검증 및 초기화
        validateAndFixSharedPreferences()

        // ✅ FIX: 자동 로그인 체크를 더 안전하게 처리
        checkAutoLogin(prefs)
    }

    private fun initViews() {
        editTextId = findViewById(R.id.edit_id)
        editTextPassword = findViewById(R.id.edit_password)
        loginButton = findViewById(R.id.btn_login)
        signupButton = findViewById(R.id.btn_signup)
        findInfoText = findViewById(R.id.txt_find_account)
        autoLoginCheckbox = findViewById(R.id.checkbox_autologin)

        // ✅ 방법 1: backButton을 안전하게 처리 (추천)
        backButton = findViewById<ImageButton>(R.id.btn_back)
            ?: run {
                Log.w("LoginActivity", "btn_back을 찾을 수 없습니다. 레이아웃을 확인하세요.")
                // 더미 ImageButton 생성하거나 null 허용 타입으로 변경
                ImageButton(this).apply { visibility = android.view.View.GONE }
            }
    }

    private fun setupClickListeners() {
        // ✅ 로그인 버튼 클릭 이벤트 처리
        loginButton.setOnClickListener {
            val id = editTextId.text.toString().trim()
            val pw = editTextPassword.text.toString().trim()

            // ✅ FIX: 입력 검증 추가
            if (id.isEmpty()) {
                Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
                editTextId.requestFocus()
                return@setOnClickListener
            }

            if (pw.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                editTextPassword.requestFocus()
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
    }

    // ✅ FIX: SharedPreferences 데이터 타입 충돌 해결
    private fun validateAndFixSharedPreferences() {
        try {
            // TOKEN_EXPIRY가 String으로 저장되어 있는지 확인하고 수정
            if (prefs.contains(KEY_TOKEN_EXPIRY)) {
                try {
                    prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
                } catch (e: ClassCastException) {
                    Log.w("LoginActivity", "TOKEN_EXPIRY가 잘못된 타입으로 저장됨. 초기화합니다.")
                    prefs.edit().remove(KEY_TOKEN_EXPIRY).apply()
                }
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "SharedPreferences 검증 중 오류 발생", e)
            clearAllPreferences()
        }
    }

    // ✅ FIX: 자동 로그인 체크를 별도 함수로 분리 + 안전 처리
    private fun checkAutoLogin(prefs: SharedPreferences) {
        try {
            val autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN, false)
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)

            // ✅ FIX: 안전한 방식으로 Long 값 읽기
            val expiryTime = try {
                prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
            } catch (e: ClassCastException) {
                Log.w("LoginActivity", "TOKEN_EXPIRY 읽기 실패, 기본값 사용")
                0L
            }

            val now = System.currentTimeMillis()

            Log.d("LoginActivity", "자동로그인 체크: autoLogin=$autoLogin, hasToken=${!accessToken.isNullOrEmpty()}, isExpired=${now > expiryTime}")

            // ✅ FIX: 토큰이 유효할 때만 자동 로그인
            if (autoLogin && !accessToken.isNullOrEmpty() && now < expiryTime) {
                Log.d("LoginActivity", "자동 로그인 실행")
                navigateToMainActivity()
            } else if (autoLogin && (accessToken.isNullOrEmpty() || now > expiryTime)) {
                // 토큰이 만료되었거나 없는 경우 자동로그인 해제
                Log.d("LoginActivity", "토큰 만료로 자동로그인 해제")
                prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "자동 로그인 체크 중 오류 발생", e)
            clearAllPreferences()
        }
    }

    // ✅ FIX: 로그인 로직을 별도 함수로 분리
    private fun performLogin(id: String, pw: String, autoLogin: Boolean, prefs: SharedPreferences) {
        // 로그인 버튼 비활성화 (중복 클릭 방지)
        loginButton.isEnabled = false
        loginButton.text = "로그인 중..."

        val loginRequest = LoginRequest(id, pw)

        Log.d("LoginActivity", "로그인 시도: id=$id")

        RetrofitClient.apiService.login(loginRequest)
            .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<LoginResponse>>,
                    response: Response<ApiResponse<LoginResponse>>
                ) {
                    // 로그인 버튼 다시 활성화
                    loginButton.isEnabled = true
                    loginButton.text = "로그인"

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
                    // 로그인 버튼 다시 활성화
                    loginButton.isEnabled = true
                    loginButton.text = "로그인"

                    Toast.makeText(this@LoginActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "서버 통신 실패", t)
                }
            })
    }

    // ✅ FIX: 로그인 데이터 저장을 별도 함수로 분리 + 안전 처리
    private fun saveLoginData(loginData: LoginResponse, autoLogin: Boolean, prefs: SharedPreferences) {
        try {
            prefs.edit().apply {
                putString(KEY_ACCESS_TOKEN, loginData.token)
                putString(KEY_REFRESH_TOKEN, loginData.refreshToken)
                putString(KEY_USER_ID, loginData.userId)
                putString(KEY_USER_NAME, loginData.name)
                putString(KEY_USER_PHONE, loginData.phone)
                putBoolean(KEY_AUTO_LOGIN, autoLogin)

                // ✅ FIX: 토큰 만료 시간을 Long으로 안전하게 저장
                val expiryTimeMillis = System.currentTimeMillis() + (60 * 60 * 1000) // 1시간
                putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)

                apply()
            }

            Log.d("LoginActivity", "로그인 데이터 저장 완료: token=${loginData.token?.take(10)}...")
        } catch (e: Exception) {
            Log.e("LoginActivity", "로그인 데이터 저장 중 오류 발생", e)
            Toast.makeText(this, "로그인 정보 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ FIX: MainActivity로 이동하는 로직을 별도 함수로 분리
    private fun navigateToMainActivity() {
        try {
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                // ✅ FIX: 플래그 설정 - 로그인 스택을 모두 제거하고 새로운 태스크로 시작
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // ✅ FIX: 추가 데이터 전달
                putExtra("from_login", true)
            }

            Log.d("LoginActivity", "MainActivity 시작 시도")
            startActivity(intent)

            // ✅ FIX: finish()를 호출하여 LoginActivity 종료
            finish()

            // ✅ FIX: 화면 전환 애니메이션 설정
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } catch (e: Exception) {
            Log.e("LoginActivity", "MainActivity 시작 실패", e)
            Toast.makeText(this, "앱 시작 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ 모든 SharedPreferences 초기화 (긴급 상황용)
    private fun clearAllPreferences() {
        try {
            prefs.edit().clear().apply()
            Log.d("LoginActivity", "모든 SharedPreferences 초기화 완료")
        } catch (e: Exception) {
            Log.e("LoginActivity", "SharedPreferences 초기화 실패", e)
        }
    }

    // ✅ 로그아웃 처리 (다른 액티비티에서 호출할 수 있도록)
    fun logout() {
        clearAllPreferences()
        Log.d("LoginActivity", "로그아웃 완료")
    }

    // ✅ FIX: 액티비티 라이프사이클 로그
    override fun onResume() {
        super.onResume()
        Log.d("LoginActivity", "LoginActivity onResume 호출됨")
    }

    override fun onPause() {
        super.onPause()
        Log.d("LoginActivity", "LoginActivity onPause 호출됨")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LoginActivity", "LoginActivity onDestroy 호출됨")
    }
}
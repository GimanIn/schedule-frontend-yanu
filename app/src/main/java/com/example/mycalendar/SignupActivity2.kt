package com.example.mycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import android.content.res.ColorStateList
import com.example.mycalendar.model.SignupRequest
import com.example.mycalendar.model.SignupResponse
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity2 : AppCompatActivity() {
    // 아이디 입력
    private lateinit var editTextId: EditText
    private lateinit var btnCheckId: Button
    private lateinit var messageId: LinearLayout
    private lateinit var iconName: ImageView
    private lateinit var textName: TextView

    // 비밀번호 입력
    private lateinit var password: EditText
    private lateinit var togglePw: ImageView
    private lateinit var messagePassword: LinearLayout
    private lateinit var iconPassword: ImageView
    private lateinit var textPassword: TextView

    // 비밀번호 재입력
    private lateinit var rePassword: EditText
    private lateinit var toggleRePw: ImageView
    private lateinit var messageRePassword: LinearLayout
    private lateinit var iconRePassword: ImageView
    private lateinit var textRePassword: TextView

    // 회원가입 버튼
    private lateinit var btnFinish: Button

    // 비밀번호 보기 상태
    private var isPwVisible = false
    private var isRePwVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup2)

        initViews()

        val name = intent.getStringExtra("name") ?: ""
        val phone = intent.getStringExtra("phone") ?: ""

        // 아이디 입력 감지
        editTextId.doAfterTextChanged { text ->
            val id = text.toString()
            val isValid = Regex("^[a-zA-Z0-9]{6,10}$").matches(id)

            btnCheckId.isEnabled = id.length >= 6
            btnCheckId.setBackgroundTintList(
                ColorStateList.valueOf(if (btnCheckId.isEnabled) Color.parseColor("#1C2444") else Color.parseColor("#999CAA"))
            )

            messageId.visibility = View.GONE
            updateFinishButtonState()
        }

        // 아이디 중복 체크
        btnCheckId.setOnClickListener {
            val id = editTextId.text.toString()
            val duplicatedIds = listOf("aaa123", "user01", "test123")
            val isDuplicated = id in duplicatedIds

            messageId.visibility = View.VISIBLE
            if (isDuplicated) {
                iconName.setImageResource(R.drawable.ic_warning)
                textName.setTextColor(Color.RED)
                textName.text = "이미 존재하는 아이디입니다."
            } else {
                iconName.setImageResource(R.drawable.ic_check)
                textName.setTextColor(Color.parseColor("#2BA600"))
                textName.text = "사용 가능한 아이디입니다."
            }
            updateFinishButtonState()
        }

        // 비밀번호 입력 감지
        password.doAfterTextChanged {
            val pw = it.toString()
            val hasLower = pw.any { it.isLowerCase() }
            val hasUpper = pw.any { it.isUpperCase() }
            val hasDigit = pw.any { it.isDigit() }
            val hasSpecial = pw.any { "!@#\$%^&*()-_=+[{]}|;:'\",<.>/?`~".contains(it) }

            val typesUsed = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }
            val isValid = pw.length in 8..16 && typesUsed >= 2

            if (isValid) {
                messagePassword.visibility = View.GONE
            } else {
                messagePassword.visibility = View.VISIBLE
                iconPassword.setImageResource(R.drawable.ic_warning)
                textPassword.setTextColor(Color.RED)
                textPassword.text = "올바른 양식이 아닙니다. 다시 입력해주세요."
            }

            checkPasswordMatch()
            updateFinishButtonState()
        }

        // 비밀번호 재입력 감지
        rePassword.doAfterTextChanged {
            checkPasswordMatch()
            updateFinishButtonState()
        }

        // 비밀번호 토글
        togglePw.setOnClickListener {
            isPwVisible = !isPwVisible
            password.inputType = if (isPwVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            password.setSelection(password.text.length)
            togglePw.setImageResource(if (isPwVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
        }

        toggleRePw.setOnClickListener {
            isRePwVisible = !isRePwVisible
            rePassword.inputType = if (isRePwVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            rePassword.setSelection(rePassword.text.length)
            toggleRePw.setImageResource(if (isRePwVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
        }

        // 회원가입 버튼
        btnFinish.setOnClickListener {
            if (btnFinish.isEnabled) {
                val userId = editTextId.text.toString()
                val passwordValue = password.text.toString()
                val signupRequest = SignupRequest(userId, name, phone, passwordValue)

                RetrofitClient.apiService.signup(signupRequest)
                    .enqueue(object : Callback<SignupResponse> {
                        override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                startActivity(Intent(this@SignupActivity2, SignupCompleteActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@SignupActivity2, "회원가입 실패: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                            Toast.makeText(this@SignupActivity2, "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

    private fun initViews() {
        editTextId = findViewById(R.id.editTextId)
        btnCheckId = findViewById(R.id.btn_overlap)
        messageId = findViewById(R.id.message_id)
        iconName = findViewById(R.id.icon_name)
        textName = findViewById(R.id.text_name)

        password = findViewById(R.id.password)
        togglePw = findViewById(R.id.togglePw)
        messagePassword = findViewById(R.id.message_password)
        iconPassword = findViewById(R.id.icon_password)
        textPassword = findViewById(R.id.text_password)

        rePassword = findViewById(R.id.re_password)
        toggleRePw = findViewById(R.id.toggle_rePw)
        messageRePassword = findViewById(R.id.message_re_password)
        iconRePassword = findViewById(R.id.icon_re_password)
        textRePassword = findViewById(R.id.text_re_password)

        btnFinish = findViewById(R.id.btn_finish)
    }

    private fun checkPasswordMatch() {
        val pw = password.text.toString()
        val rePw = rePassword.text.toString()

        if (rePw.isEmpty()) {
            messageRePassword.visibility = View.GONE
            return
        }

        messageRePassword.visibility = View.VISIBLE
        if (pw == rePw) {
            iconRePassword.setImageResource(R.drawable.ic_check)
            textRePassword.setTextColor(Color.parseColor("#2BA600"))
            textRePassword.text = "비밀번호가 일치합니다."
        } else {
            iconRePassword.setImageResource(R.drawable.ic_warning)
            textRePassword.setTextColor(Color.RED)
            textRePassword.text = "비밀번호가 일치하지 않습니다. 다시 입력해주세요."
        }
    }

    private fun updateFinishButtonState() {
        val isIdAvailable = messageId.visibility == View.VISIBLE && textName.text.contains("사용 가능")
        val isPwValid = messagePassword.visibility == View.GONE
        val isPwMatch = messageRePassword.visibility == View.VISIBLE && textRePassword.text.contains("일치합니다")

        val canRegister = isIdAvailable && isPwValid && isPwMatch
        btnFinish.isEnabled = canRegister

        btnFinish.setBackgroundTintList(
            ColorStateList.valueOf(if (canRegister) Color.parseColor("#1C2444") else Color.parseColor("#999CAA"))
        )
    }
}

package com.example.mycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

class SignupActivity2 : AppCompatActivity() {

    // 입력 필드 및 버튼
    private lateinit var editTextId: EditText
    private lateinit var btnCheckId: Button
    private lateinit var idWarning: TextView

    private lateinit var editTextPw: EditText
    private lateinit var togglePw: ImageView
    private lateinit var pwWarning: TextView

    private lateinit var editTextPwConfirm: EditText
    private lateinit var togglePwConfirm: ImageView
    private lateinit var pwConfirmWarning: TextView

    private lateinit var btnRegister: Button

    // 상태 변수
    private var isIdValid = false
    private var isIdAvailable = false
    private var isPwValid = false
    private var isPwMatched = false
    private var isPwVisible = false
    private var isPwConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup2)  // XML 파일 연결

        initViews()          // 뷰 바인딩
        setupListeners()     // 입력 리스너 등록

        // 이전 액티비티에서 이름, 번호 받기
        val name = intent.getStringExtra("name") ?: ""
        val phone = intent.getStringExtra("phone") ?: ""
    }

    // 각 뷰 컴포넌트를 연결
    private fun initViews() {
        editTextId = findViewById(R.id.editTextId)
        btnCheckId = findViewById(R.id.btn_overlap)
        idWarning = findViewById(R.id.id_warning)

        editTextPw = findViewById(R.id.btn_newpassword)
        togglePw = findViewById(R.id.toggle_password)
        pwWarning = findViewById(R.id.pw_warning_text)

        editTextPwConfirm = findViewById(R.id.btn_re_password)
        togglePwConfirm = findViewById(R.id.toggle_password2)
        pwConfirmWarning = findViewById(R.id.pw_confirm_warning_text)

        btnRegister = findViewById(R.id.btn_register)
    }

    // 입력 리스너 설정
    private fun setupListeners() {

        // 아이디 입력 감지
        editTextId.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val id = s.toString()

                // 정규식 조건 검사 (소문자, 숫자, _, -, . 포함 6~10자)
                isIdValid = Regex("^[a-z0-9_.-]{6,10}$").matches(id)

                // 6자 이상일 경우 버튼 활성화
                btnCheckId.isEnabled = id.length >= 6
                btnCheckId.setBackgroundResource(
                    if (btnCheckId.isEnabled) R.drawable.btn_login
                    else R.drawable.ic_roundedbox
                )

                // 양식 오류 표시
                if (!isIdValid) {
                    showWarning(idWarning, "올바른 양식이 아닙니다. 다시 입력해주세요.", false)
                    isIdAvailable = false
                    updateRegisterButton()
                } else {
                    hideWarning(idWarning)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 중복 확인 버튼 클릭 시
        btnCheckId.setOnClickListener {
            val id = editTextId.text.toString()

            // 예시: 실제 구현에서는 서버 중복 체크 필요
            if (id == "existingid") {
                showWarning(idWarning, "이미 존재하는 아이디입니다.", false)
                isIdAvailable = false
            } else {
                showWarning(idWarning, "사용 가능한 아이디입니다.", true)
                isIdAvailable = true
            }

            // 버튼 색상
            btnCheckId.setTextColor(
                if (btnCheckId.isEnabled) Color.WHITE else Color.BLACK
            )
            updateRegisterButton()
        }

        // 비밀번호 입력 감지
        editTextPw.doAfterTextChanged {
            val pw = it.toString()
            isPwValid = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+=-])[A-Za-z\\d!@#\$%^&*()_+=-]{8,16}$")
                .containsMatchIn(pw)

            if (!isPwValid) {
                showWarning(pwWarning, "영문자, 숫자, 특수문자 포함 8~16자로 입력해주세요.", false)
            } else {
                hideWarning(pwWarning)
            }

            validatePasswordMatch()
            updateRegisterButton()
        }

        // 비밀번호 확인 입력 감지
        editTextPwConfirm.doAfterTextChanged {
            validatePasswordMatch()
            updateRegisterButton()
        }

        // 비밀번호 눈 아이콘 클릭 시 가시성 토글
        togglePw.setOnClickListener {
            isPwVisible = !isPwVisible
            toggleVisibility(editTextPw, togglePw, isPwVisible)
        }

        // 비밀번호 확인 눈 아이콘 클릭 시 가시성 토글
        togglePwConfirm.setOnClickListener {
            isPwConfirmVisible = !isPwConfirmVisible
            toggleVisibility(editTextPwConfirm, togglePwConfirm, isPwConfirmVisible)
        }

        // 회원가입 버튼 클릭
        btnRegister.setOnClickListener {
            if (isIdAvailable && isPwValid && isPwMatched) {
                // 성공 시 다음 화면으로 이동
                val intent = Intent(this, SignupActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    // 비밀번호 일치 여부 확인
    private fun validatePasswordMatch() {
        val pw = editTextPw.text.toString()
        val confirm = editTextPwConfirm.text.toString()
        isPwMatched = pw == confirm && pw.isNotEmpty()

        if (isPwMatched) {
            showWarning(pwConfirmWarning, "비밀번호가 일치합니다.", true)
        } else {
            showWarning(pwConfirmWarning, "비밀번호가 일치하지 않습니다. 다시 입력해주세요", false)
        }
    }

    // 비밀번호 표시 토글 처리
    private fun toggleVisibility(editText: EditText, icon: ImageView, isVisible: Boolean) {
        editText.inputType = if (isVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        editText.setSelection(editText.text.length)

        icon.setImageResource(
            if (isVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )
    }

    // 경고/성공 메시지 표시
    private fun showWarning(textView: TextView, message: String, isSuccess: Boolean) {
        textView.visibility = View.VISIBLE
        textView.text = message
        textView.setTextColor(
            if (isSuccess) Color.parseColor("#2BA600") else Color.parseColor("#FF0000")
        )
        textView.setCompoundDrawablesWithIntrinsicBounds(
            if (isSuccess) R.drawable.ic_check else R.drawable.ic_warning, 0, 0, 0
        )
    }

    // 메시지 숨김 처리
    private fun hideWarning(textView: TextView) {
        textView.visibility = View.GONE
        textView.text = ""
        textView.setCompoundDrawables(null, null, null, null)
    }

    // 모든 조건 만족 시 회원가입 버튼 활성화
    private fun updateRegisterButton() {
        val enabled = isIdAvailable && isPwValid && isPwMatched
        btnRegister.isEnabled = enabled
        btnRegister.setBackgroundResource(
            if (enabled) R.drawable.btn_login else R.drawable.ic_roundedbox
        )
        btnRegister.setTextColor(
            if (enabled) Color.WHITE else Color.BLACK
        )
    }
}
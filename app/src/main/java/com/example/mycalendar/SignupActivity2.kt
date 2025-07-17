package com.example.mycalendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

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
        setContentView(R.layout.activity_signup2) // XML 화면 연결

        // 📌 모든 뷰와 연결
        initViews()

        // 아이디 입력 감지
        editTextId.doAfterTextChanged { text ->
            val id = text.toString()
            // 정규식: 영문 대/소문자 또는 숫자, 6~10자
            val isValid = Regex("^[a-zA-Z0-9]{6,10}$").matches(id)

            // 입력 길이가 6자 이상이면 버튼 활성화
            btnCheckId.isEnabled = id.length >= 6
            // 버튼 색상 변경 (예: 회색 → 남색)
            btnCheckId.setBackgroundResource(
                if (btnCheckId.isEnabled) R.drawable.btn_login else R.drawable.ic_roundedbox_dark
            )

            // 아이디 입력이 바뀔 때마다 메시지 숨기기
            messageId.visibility = View.GONE
            // 회원가입 버튼 상태 갱신
            updateFinishButtonState()
        }
        btnCheckId.setOnClickListener {
            val id = editTextId.text.toString()
            // 임시 중복 리스트: 나중에 서버 연동하면 이 부분 변경
            val duplicatedIds = listOf("aaa123", "user01", "test123")
            val isDuplicated = id in duplicatedIds

            messageId.visibility = View.VISIBLE

            if (isDuplicated) {
                // 중복된 아이디
                iconName.setImageResource(R.drawable.ic_warning)
                textName.setTextColor(Color.RED)
                textName.text = "이미 존재하는 아이디입니다."
            } else {
                // 사용 가능한 아이디
                iconName.setImageResource(R.drawable.ic_check)
                textName.setTextColor(Color.parseColor("#2BA600"))
                textName.text = "사용 가능한 아이디입니다."
            }
            updateFinishButtonState()
        }

        // 비밀번호 입력 감지
        password.doAfterTextChanged {
            val pw = it.toString()
            // 조건별 체크
            val hasLower = pw.any { it.isLowerCase() }
            val hasUpper = pw.any { it.isUpperCase() }
            val hasDigit = pw.any { it.isDigit() }
            val hasSpecial = pw.any { "!@#\$%^&*()-_=+[{]}|;:'\",<.>/?`~".contains(it) }

            val typesUsed = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }
            val isValid = pw.length in 8..16 && typesUsed >= 2

            if (isValid) {
                // 조건 만족 → 경고 숨기기
                messagePassword.visibility = View.GONE
            } else {
                // 조건 불충분 → 경고 메시지 표시
                messagePassword.visibility = View.VISIBLE
                iconPassword.setImageResource(R.drawable.ic_warning)
                textPassword.setTextColor(Color.RED)
                textPassword.text = "올바른 양식이 아닙니다. 다시 입력해주세요."
            }

            // 아래 단계에서 추가할 비밀번호 재확인 검사 함수
            checkPasswordMatch()
            // 회원가입 버튼 상태 갱신
            updateFinishButtonState()
        }
        // 비밀번호 재입력 감지
        rePassword.doAfterTextChanged {
            checkPasswordMatch()
            updateFinishButtonState()
        }
        // 비밀번호 초기상태
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        // 비밀번호 보기/숨기기 토글 기능 추가
        togglePw.setOnClickListener {
            isPwVisible = !isPwVisible

            // inputType 설정 변경
            password.inputType = if (isPwVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            // 커서 위치 유지
            password.setSelection(password.text.length)

            // 눈 아이콘 바꾸기
            togglePw.setImageResource(
                if (isPwVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
        }
        // 비밀번호 초기상태
        rePassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        // 토글 클릭 시 보여주기/숨기
        toggleRePw.setOnClickListener {
            isRePwVisible = !isRePwVisible

            rePassword.inputType = if (isRePwVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            rePassword.setSelection(rePassword.text.length)

            toggleRePw.setImageResource(
                if (isRePwVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
        }
        // 회원가입 버튼
        btnFinish.setOnClickListener {
            if (btnFinish.isEnabled) {
                val intent = Intent(this, SignupCompleteActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    // 💡 뷰들을 한 번에 연결하는 함수
    private fun initViews() {
        // 아이디
        editTextId = findViewById(R.id.editTextId)
        btnCheckId = findViewById(R.id.btn_overlap)
        messageId = findViewById(R.id.message_id)
        iconName = findViewById(R.id.icon_name)
        textName = findViewById(R.id.text_name)

        // 비밀번호
        password = findViewById(R.id.password)
        togglePw = findViewById(R.id.togglePw)
        messagePassword = findViewById(R.id.message_password)
        iconPassword = findViewById(R.id.icon_password)
        textPassword = findViewById(R.id.text_password)

        // 비밀번호 확인
        rePassword = findViewById(R.id.re_password)
        toggleRePw = findViewById(R.id.toggle_rePw)
        messageRePassword = findViewById(R.id.message_re_password)
        iconRePassword = findViewById(R.id.icon_re_password)
        textRePassword = findViewById(R.id.text_re_password)

        // 완료 버튼
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
            btnFinish.setBackgroundResource(
                if (canRegister) R.drawable.btn_login else R.drawable.ic_roundedbox_dark
            )
        }
    }
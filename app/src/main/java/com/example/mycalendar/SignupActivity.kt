package com.example.mycalendar

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color

class SignupActivity : AppCompatActivity() {

    private var isVerified = false // 인증 성공 여부
    private var sentCode = "123456"  // 임시 하드코딩된 인증번호
    private var sendCount = 1 // 인증번호 발송 횟수
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // UI 연결
        // 입력 필드
        val etName = findViewById<EditText>(R.id.et_name)
        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etAuthCode = findViewById<EditText>(R.id.et_auth_code)

        // 버튼
        val btnSendCode = findViewById<Button>(R.id.btn_send_code)
        val btnCheckCode = findViewById<Button>(R.id.btn_check_code)
        val btnNext = findViewById<Button>(R.id.btn_next)

        // 이름 경고 메시지
        val messageName = findViewById<LinearLayout>(R.id.message_name)
        val iconName = findViewById<ImageView>(R.id.icon_name)
        val textName = findViewById<TextView>(R.id.text_name)

        // 인증 메시지 영역
        val messagePhone = findViewById<LinearLayout>(R.id.message_phone)
        val iconPhone = findViewById<ImageView>(R.id.icon_phone)
        val textPhone = findViewById<TextView>(R.id.text_phone)
        val textSendCount = findViewById<TextView>(R.id.text_send_count)

        val messageCert = findViewById<LinearLayout>(R.id.message_cert)
        val iconCert = findViewById<ImageView>(R.id.icon_cert)
        val textCert = findViewById<TextView>(R.id.text_cert)

        // 약관 동의 체크박스
        val cbAll = findViewById<CheckBox>(R.id.cb_all)
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)
        val cbPrivacy = findViewById<CheckBox>(R.id.cb_privacy)

        // 뒤로 가기 버튼 클릭
        val backButoon = findViewById<ImageButton>(R.id.btn_back)
        backButoon.setOnClickListener {
            finish() // 현재 액티비티 종료 -> 이전 화면으로 돌아감
        }

        // 이름 + 번호 입력 시 인증버튼 활성화
        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString()
                val isNameValid = isValidName(name)
                val isPhoneValid = phone.length == 11

                // 이름 경고 처리
                if (!isNameValid && name.isNotEmpty()) {
                    messageName.visibility = LinearLayout.VISIBLE
                    iconName.setImageResource(R.drawable.ic_warning)
                    textName.text="올바른 양식이 아닙니다. 다시 입력해주세요."
                    textName.setTextColor(Color.RED)
                } else {
                    messageName.visibility = LinearLayout.GONE
                }

                // 휴대폰 번호 경고 처리
                if (!isPhoneValid && phone.isNotEmpty()) {
                    messagePhone.visibility = LinearLayout.VISIBLE
                    iconPhone.setImageResource(R.drawable.ic_warning)
                    textPhone.text = "올바른 양식이 아닙니다. 다시 입력해주세요."
                    textPhone.setTextColor(Color.RED)
                    textSendCount.visibility = TextView.GONE
                } else {
                    messagePhone.visibility = LinearLayout.GONE
                }
                // 휴대폰 번호 포함한 전체 버튼 조건
                val valid = isNameValid && isPhoneValid
                btnSendCode.isEnabled = valid
                btnSendCode.setBackgroundResource(
                    if (valid) R.drawable.btn_login else R.drawable.ic_roundedbox_dark
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etName.addTextChangedListener(inputWatcher)
        etPhone.addTextChangedListener(inputWatcher)

        // 인증번호 6자리 입력 감지
        etAuthCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val valid = s?.length == 6
                btnCheckCode.isEnabled = valid
                btnCheckCode.setBackgroundResource(if (valid) R.drawable.btn_login else R.drawable.ic_roundedbox_dark)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 인증번호 발송 버튼 클릭
        btnSendCode.setOnClickListener {
            if (sendCount > 5) {
                messagePhone.visibility = LinearLayout.VISIBLE
                iconPhone.setImageResource(R.drawable.ic_warning)
                textPhone.text = "인증번호 발송 횟수를 초과했습니다."
                textPhone.setTextColor(Color.RED)
                textSendCount.visibility = TextView.GONE
                return@setOnClickListener
            }
            messagePhone.visibility = LinearLayout.VISIBLE
            iconPhone.setImageResource(R.drawable.ic_check)
            textPhone.text = "인증번호가 발송되었습니다."
            textPhone.setTextColor(Color.parseColor("#2BA600"))
            textSendCount.text = "($sendCount/5)"
            textSendCount.visibility = TextView.VISIBLE

            startTimer()
            sendCount++
        }

        // 인증번호 확인 버튼 클릭
        btnCheckCode.setOnClickListener {
            messageCert.visibility = LinearLayout.VISIBLE
            val inputCode = etAuthCode.text.toString()

            if (inputCode == sentCode) {
                isVerified = true
                iconCert.setImageResource(R.drawable.ic_check)
                textCert.text = "인증번호가 일치합니다."
                textCert.setTextColor(Color.parseColor("#2BA600"))
            } else {
                isVerified = false
                iconCert.setImageResource(R.drawable.ic_warning)
                textCert.text = "인증번호가 일치하지 않습니다."
                textCert.setTextColor(Color.RED)
            }
            updateNextButton(cbTerms, cbPrivacy, btnNext)
        }

        // 전체 동의 => 개별 2개 체크
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            cbTerms.isChecked = isChecked
            cbPrivacy.isChecked = isChecked
        }

        // 개별 체크 -> 전체 동의
        val updateAllCheckbox = {
            cbAll.setOnCheckedChangeListener(null) // 무한 루프 방지
            cbAll.isChecked = cbTerms.isChecked && cbPrivacy.isChecked
            cbAll.setOnCheckedChangeListener { _, isChecked ->
                cbTerms.isChecked = isChecked
                cbPrivacy.isChecked = isChecked
            }
        }

        cbTerms.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
            updateNextButton(cbTerms, cbPrivacy, btnNext)
        }
        cbPrivacy.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
            updateNextButton(cbTerms, cbPrivacy, btnNext)
        }
    }
    // 다음 버튼 활성화 조건 확인
    private fun updateNextButton(cbTerms: CheckBox, cbPrivacy: CheckBox, btnNext: Button) {
        val enabled = isVerified && cbTerms.isChecked && cbPrivacy.isChecked
        btnNext.isEnabled = enabled
        btnNext.setBackgroundResource(if (enabled) R.drawable.btn_login else R.drawable.ic_roundedbox_dark)
    }

    // 인증 타이머 시작 (180초)
    private fun startTimer() {
        val textTimer = findViewById<TextView>(R.id.text_timer)
        timer?.cancel()
        timer = object : CountDownTimer(180000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val min = millisUntilFinished / 60000
                val sec = (millisUntilFinished / 1000) % 60
                textTimer.text = String.format("%01d:%02d", min, sec)
            }

            override fun onFinish() {
                // 타이머 텍스트
                val textTimer = findViewById<TextView>(R.id.text_timer)
                textTimer.text="시간초과"
                // 인증 메시지 영역
                val textCert = findViewById<TextView>(R.id.text_cert)
                val iconCert = findViewById<ImageView>(R.id.icon_cert)
                val layout = findViewById<LinearLayout>(R.id.message_cert)
                // 경고 아이콘
                layout.visibility = LinearLayout.VISIBLE
                iconCert.setImageResource(R.drawable.ic_warning)
                textCert.text="인증 시간이 만료되었습니다. 다시 시도해주세요."
                textCert.setTextColor(Color.RED)
            }
        }.start()
    }

    private fun isValidName(name: String): Boolean {
        val regex = "^[가-힣a-zA-Z]{2,20}$".toRegex()
        return name.matches(regex)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
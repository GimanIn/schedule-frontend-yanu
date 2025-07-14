package com.example.mycalendar

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color

class SignupActivity : AppCompatActivity() {

    private var isVerified = false
    private var sentCode = "123456"  // 임시 하드코딩된 인증번호ㅌ
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.et_name)
        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etAuthCode = findViewById<EditText>(R.id.et_auth_code)

        val btnSendCode = findViewById<Button>(R.id.btn_send_code)
        val btnCheckCode = findViewById<Button>(R.id.btn_check_code)
        val btnNext = findViewById<Button>(R.id.btn_next)

        val tvAuthMessage = findViewById<TextView>(R.id.tv_auth_message)

        // 초기 상태
        btnSendCode.isEnabled = false
        btnCheckCode.isEnabled = false
        btnNext.isEnabled = false
        btnSendCode.setBackgroundResource(R.drawable.ic_roundedbox_dark)
        btnCheckCode.setBackgroundResource(R.drawable.ic_roundedbox_dark)
        btnNext.setBackgroundResource(R.drawable.ic_roundedbox_dark)

        // 이름 & 번호 입력 감지
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val valid = etName.text.isNotBlank() && etPhone.text.length == 11
                btnSendCode.isEnabled = valid
                btnSendCode.setBackgroundResource(
                    if (valid) R.drawable.btn_login else R.drawable.ic_roundedbox_dark
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etName.addTextChangedListener(watcher)
        etPhone.addTextChangedListener(watcher)

        // 인증번호 입력 감지
        etAuthCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val valid = s?.length == 6
                btnCheckCode.isEnabled = valid
                btnCheckCode.setBackgroundResource(
                    if (valid) R.drawable.btn_login else R.drawable.ic_roundedbox_dark
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 인증번호 발송 버튼 클릭
        btnSendCode.setOnClickListener {
            // 실제로는 서버 요청 필요 → 지금은 하드코딩된 sentCode 사용
            Toast.makeText(this, "인증번호가 발송되었습니다.", Toast.LENGTH_SHORT).show()
            tvAuthMessage.text = "📩 인증번호가 발송되었습니다. (3:00)"
            tvAuthMessage.setTextColor(Color.parseColor("#2BA600"))
            startTimer(tvAuthMessage)
        }

        // 인증번호 확인 버튼 클릭
        btnCheckCode.setOnClickListener {
            val inputCode = etAuthCode.text.toString()
            if (inputCode == sentCode) {
                isVerified = true
                tvAuthMessage.text = "✅ 인증이 완료되었습니다."
                tvAuthMessage.setTextColor(Color.parseColor("#2BA600"))
                btnNext.isEnabled = true
                btnNext.setBackgroundResource(R.drawable.btn_login)
            } else {
                tvAuthMessage.text = "❌ 인증번호가 일치하지 않습니다."
                tvAuthMessage.setTextColor(Color.parseColor("#E53935"))
            }
        }
    }

    // 타이머 3분 (180초)
    private fun startTimer(tv: TextView) {
        timer?.cancel()
        timer = object : CountDownTimer(180000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val min = millisUntilFinished / 60000
                val sec = (millisUntilFinished / 1000) % 60
                tv.text = String.format("📩 인증번호가 발송되었습니다. (%01d:%02d)", min, sec)
            }

            override fun onFinish() {
                tv.text = "⏰ 인증 시간이 만료되었습니다. 다시 시도해주세요."
                tv.setTextColor(Color.parseColor("#E53935"))
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
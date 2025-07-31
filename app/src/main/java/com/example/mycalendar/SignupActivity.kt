package com.example.mycalendar

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.content.res.ColorStateList
import com.google.android.material.button.MaterialButton
import com.example.mycalendar.model.*
import com.example.mycalendar.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    private var isVerified = false
    private var sendCount = 1
    private var timer: CountDownTimer? = null
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.et_name)
        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etAuthCode = findViewById<EditText>(R.id.et_auth_code)
        etName.setTextColor(Color.BLACK)
        etPhone.setTextColor(Color.BLACK)
        etAuthCode.setTextColor(Color.BLACK)

        val btnSendCode = findViewById<Button>(R.id.btn_send_code)
        val btnCheckCode = findViewById<Button>(R.id.btn_check_code)
        val btnNext = findViewById<MaterialButton>(R.id.btn_next)

        val messagePhone = findViewById<LinearLayout>(R.id.message_phone)
        val iconPhone = findViewById<ImageView>(R.id.icon_phone)
        val textPhone = findViewById<TextView>(R.id.text_phone)
        val textSendCount = findViewById<TextView>(R.id.text_send_count)

        val messageCert = findViewById<LinearLayout>(R.id.message_cert)
        val iconCert = findViewById<ImageView>(R.id.icon_cert)
        val textCert = findViewById<TextView>(R.id.text_cert)

        val textTimer = findViewById<TextView>(R.id.text_timer)

        val cbAll = findViewById<CheckBox>(R.id.cb_all)
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)
        val cbPrivacy = findViewById<CheckBox>(R.id.cb_privacy)

        val tvTermsDetail = findViewById<TextView>(R.id.tv_view_terms)
        val tvPrivacyDetail = findViewById<TextView>(R.id.tv_view_privacy)

        tvTermsDetail.setOnClickListener {
            val intent = Intent(this, TermsActivity::class.java)
            startActivity(intent)
        }

        tvPrivacyDetail.setOnClickListener {
            val intent = Intent(this, PrivacyActivity::class.java)
            startActivity(intent)
        }

        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener { finish() }

        val inputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString()
                val isNameValid = name.matches("^[가-힣a-zA-Z]{2,20}$".toRegex())
                val isPhoneValid = phone.length == 11

                // 휴대폰 번호 버튼 활성화
                btnSendCode.isEnabled = isNameValid && isPhoneValid
                btnSendCode.setTextColor(if (btnSendCode.isEnabled) Color.WHITE else Color.parseColor("#BDBDBD"))
                btnSendCode.setBackgroundTintList(
                    ColorStateList.valueOf(
                        if (btnSendCode.isEnabled) Color.parseColor("#1C2444") else Color.parseColor("#999CAA")
                    )
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etName.addTextChangedListener(inputWatcher)
        etPhone.addTextChangedListener(inputWatcher)

        etAuthCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val valid = s?.length == 6
                btnCheckCode.isEnabled = valid
                btnCheckCode.setTextColor(if (valid) Color.WHITE else Color.parseColor("#BDBDBD"))
                btnCheckCode.setBackgroundTintList(
                    ColorStateList.valueOf(
                        if (valid) Color.parseColor("#1C2444") else Color.parseColor("#999CAA")
                    )
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 인증번호 발송
        btnSendCode.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (sendCount > 5) {
                showError(messagePhone, iconPhone, textPhone, "인증번호 발송 횟수를 초과했습니다.")
                return@setOnClickListener
            }

            apiService.sendSms(SendSMSRequest(phone)).enqueue(object : Callback<ApiResponse<Unit>> {
                override fun onResponse(call: Call<ApiResponse<Unit>>, response: Response<ApiResponse<Unit>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        showSuccess(messagePhone, iconPhone, textPhone, "인증번호가 발송되었습니다.")
                        textSendCount.text = "($sendCount/5)"
                        textSendCount.visibility = TextView.VISIBLE
                        startTimer(textTimer, messageCert, iconCert, textCert)
                        sendCount++
                    } else {
                        showError(messagePhone, iconPhone, textPhone, "발송 실패")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                    showError(messagePhone, iconPhone, textPhone, "서버 오류: ${t.message}")
                }
            })
        }

        // 인증번호 확인
        btnCheckCode.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val code = etAuthCode.text.toString().trim()

            apiService.verifySms(VerifySMSRequest(phone, code)).enqueue(object : Callback<ApiResponse<Unit>> {
                override fun onResponse(call: Call<ApiResponse<Unit>>, response: Response<ApiResponse<Unit>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        isVerified = true
                        showSuccess(messageCert, iconCert, textCert, "인증번호가 일치합니다.")
                    } else {
                        isVerified = false
                        showError(messageCert, iconCert, textCert, "인증번호가 일치하지 않습니다.")
                    }
                    updateNextButton(cbTerms, cbPrivacy, btnNext)
                }

                override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                    showError(messageCert, iconCert, textCert, "서버 오류: ${t.message}")
                }
            })
        }

        // 다음 버튼 클릭
        btnNext.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (!isVerified) {
                Toast.makeText(this, "휴대폰 인증을 완료해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, SignupActivity2::class.java)
            intent.putExtra("name", name)
            intent.putExtra("phone", phone)
            startActivity(intent)
        }

        val updateAllCheckbox = {
            cbAll.setOnCheckedChangeListener(null)
            cbAll.isChecked = cbTerms.isChecked && cbPrivacy.isChecked
            cbAll.setOnCheckedChangeListener { _, isChecked ->
                cbTerms.isChecked = isChecked
                cbPrivacy.isChecked = isChecked
            }
        }

        cbAll.setOnCheckedChangeListener { _, isChecked ->
            cbTerms.isChecked = isChecked
            cbPrivacy.isChecked = isChecked
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

    // 다음 버튼 활성화
    private fun updateNextButton(cbTerms: CheckBox, cbPrivacy: CheckBox, btnNext: MaterialButton) {
        val enabled = isVerified && cbTerms.isChecked && cbPrivacy.isChecked
        btnNext.isEnabled = enabled
        btnNext.setTextColor(if (enabled) Color.WHITE else Color.parseColor("#BDBDBD"))
        btnNext.setBackgroundTintList(
            ColorStateList.valueOf(
                if (enabled) Color.parseColor("#1C2444") else Color.parseColor("#999CAA")
            )
        )
    }

    private fun startTimer(
        textTimer: TextView,
        layout: LinearLayout,
        icon: ImageView,
        textCert: TextView
    ) {
        timer?.cancel()
        timer = object : CountDownTimer(180000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val min = millisUntilFinished / 60000
                val sec = (millisUntilFinished / 1000) % 60
                textTimer.text = String.format("%01d:%02d", min, sec)
            }

            override fun onFinish() {
                textTimer.text = "시간초과"
                layout.visibility = LinearLayout.VISIBLE
                icon.setImageResource(R.drawable.ic_warning)
                textCert.text = "인증 시간이 만료되었습니다. 다시 시도해주세요."
                textCert.setTextColor(Color.RED)
            }
        }.start()
    }

    private fun showSuccess(layout: LinearLayout, icon: ImageView, text: TextView, message: String) {
        layout.visibility = LinearLayout.VISIBLE
        icon.setImageResource(R.drawable.ic_check)
        text.text = message
        text.setTextColor(Color.parseColor("#2BA600"))
    }

    private fun showError(layout: LinearLayout, icon: ImageView, text: TextView, message: String) {
        layout.visibility = LinearLayout.VISIBLE
        icon.setImageResource(R.drawable.ic_warning)
        text.text = message
        text.setTextColor(Color.RED)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
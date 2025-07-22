package com.example.mycalendar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MypageActivity : AppCompatActivity() {

    private lateinit var nameText: TextView
    private lateinit var idText: TextView
    private lateinit var btnChangePw: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton


    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        nameText = findViewById(R.id.nameText)
        idText = findViewById(R.id.idText)
        btnChangePw = findViewById(R.id.btnChangePw)
        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // 저장된 이름, 아이디 불러오기
        nameText.text = sharedPref.getString("name", "이름 없음")
        idText.text = sharedPref.getString("id", "ID 없음")

        // 비밀번호 변경 화면 이동
        btnChangePw.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // 로그아웃
        btnLogout.setOnClickListener {
            sharedPref.edit().putBoolean("autoLogin", false).apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // 계정 삭제 (실제로는 서버 연동 필요)
        btnDeleteAccount.setOnClickListener {
            Toast.makeText(this, "계정 삭제 요청이 접수되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
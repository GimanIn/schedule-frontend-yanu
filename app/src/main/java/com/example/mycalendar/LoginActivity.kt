package com.example.mycalendar

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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

        // 로그인 버튼 클릭
        loginButton.setOnClickListener{
            val id = editTextId.text.toString()
            val pw = editTextPassword.text.toString()
            val autoLogin = autoLoginCheckbox.isChecked

            if (id == "test" && pw == "1234") {
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()

                if (autoLogin) {
                    // 자동로그인 정보 저장
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString("user_id", id)
                    editor.putString("user_pw", pw)
                    editor.putBoolean("auto_login", true)
                    editor.apply()
                }

                // 메인 화면으로 이동하는 Intent 추가
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "아이디나 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
            }
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
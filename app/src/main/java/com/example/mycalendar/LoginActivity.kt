package com.example.mycalendar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editTextId = findViewById<EditText>(R.id.editTextId)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signupButton)

        // 로그인 버튼 클릭
        loginButton.setOnClickListener{
            val id = editTextId.text.toString()
            val pw = editTextPassword.text.toString()

            if (id == "test" && pw == "1234") {
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                // 메인 화면으로 이동
            } else {
                Toast.makeText(this, "아이디나 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 회원가입 버튼 클릭
        signupButton.setOnClickListener {
            // 회원가입 화면으로 이동
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
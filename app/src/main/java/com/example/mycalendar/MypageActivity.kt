package com.example.mycalendar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.os.Handler
import android.os.Looper
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog

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

        // 상단 아이콘 클릭 시 MainActivity로 이동
        val userIcon = findViewById<ImageView>(R.id.userIcon)

        userIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

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
            // 자동로그인 해제
            sharedPref.edit().putBoolean("autoLogin", false).commit()

            // 로그아웃 안내 문구
            val rootView = findViewById<View>(android.R.id.content)
            Snackbar.make(rootView, "로그아웃 되었습니다.", Snackbar.LENGTH_SHORT).show()

            // 1~1.5초 후에 LoginActivity로 이동
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("fromLogout", true)
                startActivity(intent)
                finish()
            }, 1200)  // 1.2초 정도 지연
        }

        // 계정 삭제 (실제로는 서버 연동 필요)
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("계정을 삭제하시겠습니까?")
                .setMessage("계정 삭제 시 계정 정보 및 일정에 관한 내용은 복구되지 않습니다.")
                .setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()  // 아무것도 하지 않고 창 닫기
                }
                .setPositiveButton("계정 삭제") { _, _ ->
                    // 모든 사용자 정보 삭제
                    sharedPref.edit().clear().commit()

                    // 안내 메시지 표시
                    val rootView = findViewById<View>(android.R.id.content)
                    Snackbar.make(rootView, "계정이 삭제되었습니다.", Snackbar.LENGTH_SHORT).show()

                    // 약간의 지연 후 로그인 화면으로 이동
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("fromLogout", true)
                        startActivity(intent)
                        finish()
                    }, 1200)
                }
                .show()
        }
    }
}
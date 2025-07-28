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
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import android.view.Gravity

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

        // 뒤로가기 클릭 시 MainActivity로 이동
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            // 사용자 정보 삭제
            sharedPref.edit().clear().commit()

            // 스낵바 표시
            val rootView = findViewById<View>(android.R.id.content)
            val toast = Toast.makeText(this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

            dialog.dismiss()

            // 로그인 화면으로 이동
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("fromLogout", true)
                startActivity(intent)
                finish()
            }, 1200)
        }

        dialog.show()
    }
}

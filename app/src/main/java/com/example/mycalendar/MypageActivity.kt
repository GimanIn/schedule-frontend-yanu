package com.example.mycalendar

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.model.UserInfoResponse
import com.example.mycalendar.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

        intent?.data?.let { deepLink ->
            Log.d("DeepLink", "LoginActivity에서 받은 딥링크: $deepLink")

            val mainIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = deepLink
            }
            startActivity(mainIntent)
            finish()
            return
        }

        // ✅ LoginActivity와 동일한 SharedPreferences 이름 사용
        sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        nameText = findViewById(R.id.nameText)
        idText = findViewById(R.id.idText)
        btnChangePw = findViewById(R.id.btnChangePw)
        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // ✅ 뒤로가기 클릭 시 MainActivity로 이동
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // ✅ LoginActivity와 동일한 토큰 키 사용
        val token = sharedPref.getString("access_token", "") ?: ""
        RetrofitClient.apiService.getUserInfo()
            .enqueue(object : Callback<ApiResponse<UserInfoResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserInfoResponse>>,
                    response: Response<ApiResponse<UserInfoResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val user = response.body()!!.data
                        nameText.text = user?.name
                        idText.text = user?.userId
                    } else {
                        Toast.makeText(this@MypageActivity, "유저 정보 조회 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<UserInfoResponse>>, t: Throwable) {
                    Toast.makeText(this@MypageActivity, "서버 오류", Toast.LENGTH_SHORT).show()
                }
            })

        // ✅ 비밀번호 변경 화면 이동
        btnChangePw.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // ✅ 로그아웃
        btnLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // ✅ 계정 삭제 - 커스텀 다이얼로그 + 서버 API 연동
        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog(token)
        }
    }

    private fun showDeleteAccountDialog(token: String) {
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
            // ✅ 서버에 계정 삭제 요청
            RetrofitClient.apiService.deleteAccount()
                .enqueue(object : Callback<ApiResponse<Unit>> {
                    override fun onResponse(
                        call: Call<ApiResponse<Unit>>,
                        response: Response<ApiResponse<Unit>>
                    ) {
                        if (response.isSuccessful) {
                            // 사용자 정보 삭제
                            sharedPref.edit().clear().commit()

                            // 성공 메시지 표시
                            val toast = Toast.makeText(this@MypageActivity, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.CENTER, 0, 0)
                            toast.show()

                            dialog.dismiss()

                            // 로그인 화면으로 이동
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this@MypageActivity, LoginActivity::class.java)
                                intent.putExtra("fromLogout", true)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            }, 1200)
                        } else {
                            dialog.dismiss()
                            Toast.makeText(this@MypageActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                        dialog.dismiss()
                        Toast.makeText(this@MypageActivity, "서버 오류", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        dialog.show()
    }
}
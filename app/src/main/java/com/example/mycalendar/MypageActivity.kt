package com.example.mycalendar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mycalendar.model.ApiResponse
import com.example.mycalendar.model.UserInfoResponse
import com.example.mycalendar.network.RetrofitClient
import com.google.android.material.button.MaterialButton
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

        sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        nameText = findViewById(R.id.nameText)
        idText = findViewById(R.id.idText)
        btnChangePw = findViewById(R.id.btnChangePw)
        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // ✅ 서버에서 이름, 아이디 불러오기
        val token = sharedPref.getString("accessToken", "") ?: ""
        RetrofitClient.apiService.getUserInfo("Bearer $token")
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

        // ✅ 계정 삭제
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("계정 삭제")
                .setMessage("정말로 탈퇴하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    RetrofitClient.apiService.deleteAccount("Bearer $token")
                        .enqueue(object : Callback<ApiResponse<Unit>> {
                            override fun onResponse(
                                call: Call<ApiResponse<Unit>>,
                                response: Response<ApiResponse<Unit>>
                            ) {
                                if (response.isSuccessful) {
                                    sharedPref.edit().clear().apply()
                                    Toast.makeText(this@MypageActivity, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@MypageActivity, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@MypageActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                                Toast.makeText(this@MypageActivity, "서버 오류", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}

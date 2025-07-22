package com.example.mycalendar.network

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TokenInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // ✅ SharedPreferences에서 기존 토큰들을 꺼냄
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        var token = prefs.getString("access_token", null)
        val refreshToken = prefs.getString("refresh_token", null)

        val originalRequest = chain.request()

        // ✅ access token이 있다면 Authorization 헤더에 추가
        val requestWithToken = originalRequest.newBuilder().apply {
            token?.let {
                Log.d("TokenInterceptor", "✅ 기존 access_token 사용: $it")
                addHeader("Authorization", "Bearer $it")
            }
        }.build()

        val response = chain.proceed(requestWithToken)

        // ✅ access token이 만료되었고 refresh token이 있다면 갱신 시도
        if (response.code == 401 && refreshToken != null) {
            Log.d("TokenInterceptor", "⚠️ access_token 만료. refresh_token으로 갱신 시도")
            response.close() // 기존 응답 닫기

            // ✅ refresh token으로 새 access token & refresh token 요청
            val mediaType = "application/json".toMediaType()
            val refreshJson = "{\"refreshToken\":\"$refreshToken\"}"
            val refreshRequestBody = refreshJson.toRequestBody(mediaType)

            val refreshRequest = Request.Builder()
                .url("http://10.0.2.2:8080/api/auth/refresh") // 로컬 서버 주소
                .post(refreshRequestBody)
                .build()

            val client = OkHttpClient()
            val refreshResponse = client.newCall(refreshRequest).execute()

            if (refreshResponse.isSuccessful) {
                // ✅ 응답 본문에서 새 토큰 꺼냄
                val body = refreshResponse.body?.string()
                val json = JSONObject(body ?: "")

                val newToken = json.getJSONObject("data").getString("token")
                val newRefreshToken = json.getJSONObject("data").getString("refreshToken")

                Log.d("TokenInterceptor", "✅ 새 access_token 갱신 성공: $newToken")
                Log.d("TokenInterceptor", "✅ 새 refresh_token 갱신 성공: $newRefreshToken")

                // ✅ 새 토큰들 SharedPreferences에 저장
                prefs.edit()
                    .putString("access_token", newToken)
                    .putString("refresh_token", newRefreshToken)
                    .apply()

                // ✅ 새 access token으로 원래 요청 다시 보내기
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()

                return chain.proceed(newRequest)
            } else {
                Log.e("TokenInterceptor", "❌ Refresh token 요청 실패: ${refreshResponse.code}")
            }
        }

        // ✅ (정상 응답 or refresh 실패 시) 기존 응답 그대로 반환
        return response
    }
}

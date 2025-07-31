package com.example.mycalendar.network

import com.example.mycalendar.model.HolidayResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface HolidayApiService {
    @GET("getRestDeInfo") // 특일 정보 조회 오퍼레이션 URL
    fun getHolidays(
        @Query("serviceKey") serviceKey: String, // 발급받은 API 키
        @Query("solYear") year: String,          // 년도
        @Query("solMonth") month: String,         // 월
        @Query("_type") type: String = "json"    // 응답 타입 (JSON)
    ): Call<HolidayResponse>
}
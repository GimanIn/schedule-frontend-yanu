package com.example.mycalendar.model

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

// 전체 응답을 감싸는 클래스
data class HolidayResponse(
    val response: ResponseData
)

// response 필드 내부의 데이터를 담는 클래스
data class ResponseData(
    val header: Header,
    val body: Body
)

data class Header(
    val resultCode: String,
    val resultMsg: String
)

data class Body(
    val items: Items,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int
)

// 🔧 Items 클래스 수정 - 커스텀 Deserializer 사용
data class Items(
    @JsonAdapter(HolidayItemDeserializer::class)
    @SerializedName("item")
    val holidayItems: List<HolidayItem>
)

// 실제 공휴일 정보를 담는 클래스
data class HolidayItem(
    val dateName: String, // 공휴일 이름 (예: 추석)
    val locdate: Int      // 날짜 (예: 20240917)
)

// 🎯 핵심: 커스텀 Deserializer 클래스
class HolidayItemDeserializer : JsonDeserializer<List<HolidayItem>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<HolidayItem> {
        return try {
            when {
                json == null || json.isJsonNull -> {
                    emptyList() // null인 경우 빈 리스트 반환
                }
                json.isJsonArray -> {
                    // 배열인 경우 (공휴일이 여러 개)
                    val gson = Gson()
                    val array = json.asJsonArray
                    array.map {
                        gson.fromJson(it, HolidayItem::class.java)
                    }
                }
                json.isJsonObject -> {
                    // 객체인 경우 (공휴일이 1개)
                    val gson = Gson()
                    listOf(gson.fromJson(json, HolidayItem::class.java))
                }
                else -> {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // 파싱 실패 시 빈 리스트 반환
        }
    }
}
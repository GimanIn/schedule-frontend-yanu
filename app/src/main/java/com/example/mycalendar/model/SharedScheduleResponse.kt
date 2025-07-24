// 🔧 NEW: SharedScheduleResponse.kt 예시 (임시 틀)
package com.example.mycalendar.model

data class SharedScheduleResponse(
    val id: Long,
    val title: String,
    val senderId: Long,
    val receiverId: Long,
    val status: String
)

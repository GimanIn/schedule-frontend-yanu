package com.example.mycalendar

import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID
import android.graphics.Color


data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var startDateTime: LocalDateTime? = null,
    var endDateTime: LocalDateTime? = null,
    var color: Int = Color.parseColor("#4285F4"), // 기본 파란색
    var isAlarmOn: Boolean = false,
    var memo: String = "",
    var category: String = "",
    var location: String = "",
    var isConfirmed: Boolean = true,
    var isPostponed: Boolean = false,
    var documentId: String? = null
) : Serializable
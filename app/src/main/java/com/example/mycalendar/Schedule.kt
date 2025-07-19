package com.example.mycalendar

import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID

data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var startDateTime: LocalDateTime? = null,
    var endDateTime: LocalDateTime? = null,
    var color: Int = -7829368, // 기본 회색
    var isAlarmOn: Boolean = false,
    var memo: String = "",
    var isConfirmed: Boolean = true,
    var isPostponed: Boolean = false
) : Serializable
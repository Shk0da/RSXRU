package com.github.shk0da.rsx.persistence.entity

import java.util.*

data class Ticker(
    val name: String,
    val market: String,
    val description: String,
    val totalDebt: Long,
    val debtToEquity: Double,
    val type: String,
    val subtype: String,
    val recommend1M: Double,
    val dateTime: Date
)
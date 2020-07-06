package com.github.shk0da.rsx.persistence.entity

import java.util.*

data class Fundamental(
    val symbol: String,
    val name: String,
    val dateTime: Date,
    val periodType: String?,
    val currencyCode: String?,
    val valueRaw: Double,
    val valueFmt: String?
)
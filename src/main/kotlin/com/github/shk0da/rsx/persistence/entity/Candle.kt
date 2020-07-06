package com.github.shk0da.rsx.persistence.entity

import java.util.*

data class Candle(val symbol: String, val dateTime: Date, val close: Double)
package com.github.shk0da.rsx.service

import com.github.shk0da.rsx.persistence.dao.CandleDao
import com.github.shk0da.rsx.persistence.entity.Candle
import com.github.shk0da.yahoofinance.model.TickerCandle
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.SimpleDateFormat
import java.time.Duration.of
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class CandleService {

    private val log = LoggerFactory.getLogger(CandleService::class.java)

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

    private val candleDao: CandleDao = CandleDao()
    private val httpClient = HttpClient.newHttpClient()

    fun getLastCandles(symbol: String, size: Int): List<Candle> {
        val now = Date()
        val candles = ArrayList<Candle>(candleDao.findAllBySymbol(symbol, size))
        if (candles.isEmpty()) {
            val startDate = Date(now.time - of(daysFromSize(size), DAYS).toMillis())
            addCandles(symbol, startDate, now, candles)
        } else if (candles.first().dateTime.before(Date())) {
            val lastCandle = candles.first().dateTime
            val yesterday = Date(now.time - of(1, DAYS).toMillis())
            val yesterdayDate = yesterday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val lastDate = lastCandle.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            if (lastDate.atStartOfDay().isBefore(yesterdayDate.atStartOfDay())) {
                addCandles(symbol, lastCandle, Date(), candles)
            }
        } else if (candles.size < size) {
            val firstCandle = candles.last().dateTime
            val startDate = Date(firstCandle.time - of(daysFromSize(size - candles.size), DAYS).toMillis())
            addCandles(symbol, startDate, firstCandle, candles)
        }
        return candles.asSequence().sortedByDescending { it.dateTime }.take(size).sortedBy { it.dateTime }.toMutableList()
    }

    private fun daysFromSize(size: Int): Long {
        var days = 0
        var workDays = 5
        for (day in 1..size) {
            days++
            if (--workDays == 0) {
                workDays = 5
                days += 2
            }
        }
        return (days + 10).toLong()
    }

    private fun addCandles(symbol: String, lastCandle: Date, now: Date, candles: ArrayList<Candle>) {
        try {
            sleep((1..5).random().toLong() * 1000)
            val historyData = historyData(symbol, lastCandle, now)
            log.info("Downloaded {} new candles for '{}'", historyData.size, symbol)
            historyData.forEach { tickerCandle ->
                try {
                    val candle = Candle(symbol, dateFormatter.parse(tickerCandle.date), tickerCandle.close)
                    candles.add(candleDao.save(candle))
                } catch (ex: Exception) {
                    log.error("Failed add new candle: [{}], {}", tickerCandle, ex.message)
                }
            }
        } catch (ex: Exception) {
            log.info("Failed downloaded new candles for '{}', {}", symbol, ex.message)
        }
    }

    private fun historyData(symbol: String, lastCandle: Date, now: Date): List<TickerCandle> {
        val from = dateFormatter.format(lastCandle)
        val till = dateFormatter.format(now)
        var url = "http://iss.moex.com/iss/history/engines/stock/markets/shares/boards/tqbr/securities/$symbol.csv?from=$from&till=$till"
        var historyRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(of(10, ChronoUnit.SECONDS))
            .GET()
            .build()
        var response = httpClient.send(historyRequest, HttpResponse.BodyHandlers.ofString())

        // try ETFs
        if (response.body().lines().stream().skip(3).map { it.trim() }.filter { !it.trim().isBlank() }.findFirst().isEmpty) {
            url = "http://iss.moex.com/iss/history/engines/stock/markets/shares/boards/tqtf/securities/$symbol.csv?from=$from&till=$till"
            historyRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(of(10, ChronoUnit.SECONDS))
                .GET()
                .build()
            response = httpClient.send(historyRequest, HttpResponse.BodyHandlers.ofString())
        }

        return response.body().lines().stream()
            .skip(3)
            .filter { !it.trim().isBlank() }
            .map { it.split(";") }
            .map {
                TickerCandle(
                    symbol = symbol,
                    date = it[1],
                    open = it[6].toDouble(),
                    high = it[8].toDouble(),
                    low = it[7].toDouble(),
                    close = it[11].toDouble(),
                    adjClose = it[9].toDouble(),
                    volume = it[5].toDouble().toInt()
                )
            }.collect(Collectors.toList())
    }
}
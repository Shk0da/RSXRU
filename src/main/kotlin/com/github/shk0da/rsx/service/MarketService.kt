package com.github.shk0da.rsx.service

import com.github.shk0da.rsx.persistence.dao.TickerDao
import com.github.shk0da.rsx.persistence.entity.Ticker
import com.github.shk0da.yahoofinance.model.ScanRequest
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.SimpleDateFormat
import java.time.Duration.of
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList

class MarketService {

    private val log = LoggerFactory.getLogger(MarketService::class.java)

    private val updatePeriod = of(7, ChronoUnit.DAYS).toMillis()

    private val tickerDao: TickerDao = TickerDao()
    private val httpClient = HttpClient.newHttpClient()
    private val tradingViewClient: TradingViewClient = TradingViewClient()

    init {
        val lastUpdate = tickerDao.findLatestDate()
        val updatePeriodAgoTime = Date().time.minus(updatePeriod)
        if (null == lastUpdate || lastUpdate.time < updatePeriodAgoTime) {
            updateScanMarket()
        } else {
            log.info("Last update: {}, next update: {}", lastUpdate, Date(lastUpdate.time.plus(updatePeriod)))
        }
    }

    fun getLastTickers(size: Int): List<Ticker> {
        val tickers = tickerDao.findAll(size)
        if (tickers.isEmpty() || tickers.first().dateTime.time < Date().time.minus(updatePeriod)) {
            return updateScanMarket()
        }
        return tickers
    }

    fun getLastClosePrice(symbol: String): Double {
        sleep((1..5).random().toLong() * 1000)
        var url = "http://iss.moex.com/iss/engines/stock/markets/shares/boards/tqbr/securities/$symbol.csv?iss.meta=off&iss.only=securities&securities.columns=PREVPRICE"
        var historyRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(of(10, ChronoUnit.SECONDS))
            .GET()
            .build()
        var response = httpClient.send(historyRequest, HttpResponse.BodyHandlers.ofString())

        // try ETFs
        if (response.body().lines().stream().skip(2).map { it.trim() }.filter { !it.trim().isBlank() }.findFirst().isEmpty) {
            url = "http://iss.moex.com/iss/engines/stock/markets/shares/boards/tqtf/securities/$symbol.csv?iss.meta=off&iss.only=securities&securities.columns=PREVPRICE"
            historyRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(of(10, ChronoUnit.SECONDS))
                .GET()
                .build()
            response = httpClient.send(historyRequest, HttpResponse.BodyHandlers.ofString())
        }

        val close = response.body().lines().stream()
            .skip(2)
            .map { it.trim() }
            .filter { !it.trim().isBlank() }
            .findFirst()

        return if (close.isPresent) close.get().toDouble() else 0.0
    }

    private fun updateScanMarket(size: Int = 200): List<Ticker> {
        val scanRequest = ScanRequest(
            filter = listOf(
                ScanRequest.Filter("debt_to_equity", "nempty"),
                ScanRequest.Filter("Recommend.All|1M", "nempty"),
                ScanRequest.Filter("total_debt", "nequal", 0.0),
                ScanRequest.Filter("type", "in_range", listOf("stock")),
                ScanRequest.Filter("subtype", "in_range", listOf("common")),
                ScanRequest.Filter("exchange", "in_range", listOf("MOEX")),
                ScanRequest.Filter("market_cap_basic", "egreater", 50_000_000),
                ScanRequest.Filter("Recommend.All|1M", "egreater", 0.4),
                ScanRequest.Filter("debt_to_equity", "in_range", listOf(-50, 3)),
                ScanRequest.Filter("total_revenue", "egreater", 0),
                ScanRequest.Filter("number_of_employees", "in_range", listOf(1000, 10000000))
            ),
            options = ScanRequest.Options(lang = "en"),
            columns = arrayListOf(
                "name",
                "description",
                "total_debt",
                "debt_to_equity",
                "type",
                "subtype",
                "Recommend.All|1M"
            ),
            sort = ScanRequest.Sort("debt_to_equity", "asc"),
            range = intArrayOf(0, size)
        )
        sleep(1000)
        val marketScan = tradingViewClient.scan(scanRequest)
        log.info("The scan found {} tickers", marketScan.totalCount)

        val tickers = ArrayList<Ticker>()
        for (data in marketScan.data) {
            val values = data.values
            val nameWithMarket = data.name.split(":")
            tickers.add(
                Ticker(
                    nameWithMarket[1],
                    nameWithMarket[0],
                    values[1] as String,
                    values[2].toString().toDouble().toLong(),
                    values[3].toString().toDouble(),
                    values[4] as String,
                    values[5] as String,
                    if (null == values[6]) 0.0 else values[6].toString().toDouble(),
                    Date()
                )
            )
        }
        tickerDao.saveAll(tickers)
        return tickers
    }
}
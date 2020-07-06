package com.github.shk0da.rsx

import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Logger
import com.github.shk0da.rsx.service.CandleService
import com.github.shk0da.rsx.service.MarketService
import org.slf4j.LoggerFactory
import java.lang.Double.max
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.round

class Application {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = INFO
            Application().run()
        }
    }

    private val log = LoggerFactory.getLogger(Application::class.java)

    private val trendStock = "SBMX"
    private val stockPortfolioMaxSize = 5
    private val bonds = arrayOf("SBGB")

    private val marketService = MarketService()
    private val candleService = CandleService()

    fun run() {
        trade()
    }

    private fun trade() {
        val trendUp = tendUp()
        val topSymbols = topSymbols()

        val toHold = ArrayList<String>()
        if (trendUp) {
            toHold.addAll(topSymbols)
        }

        val stockWeight = 100.0 / stockPortfolioMaxSize
        val bondWeight = max(100.0 - stockWeight * toHold.size, 0.0) / bonds.size

        val portfolioWeights: HashMap<String, Double> = HashMap()
        toHold.forEach { portfolioWeights[it] = stockWeight }
        bonds.forEach { portfolioWeights[it] = bondWeight }
        val portfolio = portfolioWeights.filter { it.value > 0.0 }.toMap()
        log.info("target weights %: {}", portfolio)

        val corrections: HashMap<String, Double> = HashMap()
        portfolio.forEach { (symbol, weight) ->
            corrections[symbol] = round((weight) * 100) / 100
        }
        val orderedCorrections = corrections.toList().filter { it.second != 0.0 }.sortedBy { it.second }.toMap()
        log.info("corrections %: {}", orderedCorrections)

        for (it in orderedCorrections) {
            try {
                val price = marketService.getLastClosePrice(it.key)
                val action = if (it.value > 0) "BUY" else "SELL"
                log.info("{} '{}' by ~{}р", action, it.key, price)
            } catch (ex: Exception) {
                log.error("Failed place order [{}]: {}", it, ex.message)
            }
        }
    }

    private fun tendUp(): Boolean {
        val trendCandles50 = candleService.getLastCandles(trendStock, 50)
        val trendCandles5 = trendCandles50.takeLast(5)
        val longSMA = trendCandles50.stream().mapToDouble { it.close }.sum() / trendCandles50.size
        val shortSMA = trendCandles5.stream().mapToDouble { it.close }.sum() / trendCandles5.size
        val trendUp = shortSMA > longSMA
        log.info("trendUp: {}", trendUp)
        return trendUp
    }

    private fun topSymbols(): List<String> {
        val tickers = marketService.getLastTickers(200)
        val dataForFiler = ArrayList<HashMap<String, Any>>()
        tickers.filter { it.debtToEquity != 0.0 }.forEach { ticker ->
            val map = HashMap<String, Any>()
            map["symbol"] = ticker.name
            map["recommend1M"] = ticker.recommend1M
            map["debtToEquity"] = ticker.debtToEquity
            dataForFiler.add(map)
        }

        val stocks = dataForFiler
            .sortedByDescending { it["recommend1M"] as Double }
            .sortedBy { it["debtToEquity"] as Double }
            .toList()

        stocks.forEach { stock ->
            val candles = candleService.getLastCandles(stock["symbol"] as String, 128)
            if (candles.isNotEmpty()) {
                val topCandles = candles.takeLast(2)
                val returnsOverall = ((candles.last().close - candles.first().close) / candles.first().close) * 100
                val returnsRecent = ((topCandles.last().close - topCandles.first().close) / topCandles.first().close) * 100
                val momentum = returnsOverall - returnsRecent
                stock["momentum"] = momentum
            } else {
                stock["momentum"] = 0.0
            }
        }

        val topQualityMomentum = stocks.asSequence()
            .sortedByDescending { it["momentum"] as Double }
            .filter { it["momentum"] as Double > 0 }
            .take(stockPortfolioMaxSize)
            .toList()

        topQualityMomentum.forEach { log.info("$it") }

        return topQualityMomentum.map { it["symbol"] as String }.toList()
    }
}
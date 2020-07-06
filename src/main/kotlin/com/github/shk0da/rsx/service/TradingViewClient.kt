package com.github.shk0da.rsx.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.shk0da.yahoofinance.model.ScanRequest
import com.github.shk0da.yahoofinance.model.ScanResult
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit

class TradingViewClient {

    companion object {
        const val SCAN_URI = "https://scanner.tradingview.com/russia/scan"
    }

    private val httpClient = HttpClient.newHttpClient()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    fun scan(request: ScanRequest): ScanResult {
        val scanRequest = HttpRequest.newBuilder()
            .uri(URI.create(SCAN_URI))
            .timeout(Duration.of(10, ChronoUnit.SECONDS))
            .POST(ofString(objectMapper.writeValueAsString(request)))
            .build()
        val response = httpClient.send(scanRequest, HttpResponse.BodyHandlers.ofString())
        return objectMapper.readValue(response.body())
    }
}
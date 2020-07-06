package com.github.shk0da.rsx.persistence.dao

import com.github.shk0da.rsx.persistence.dao.Dao.Companion.connection
import com.github.shk0da.rsx.persistence.entity.Candle
import java.util.*

class CandleDao : Dao<Candle> {

    companion object {
        private const val createTableSQL = """CREATE TABLE IF NOT EXISTS candle (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                    symbol TEXT NOT NULL,
                                                    date_time LONG DEFAULT 0,
                                                    "close" DOUBLE DEFAULT 0,
                                                    UNIQUE(symbol, date_time)
                                                );"""

        init {
            // create table
            connection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(createTableSQL)
                }
            }
        }
    }

    fun findAllBySymbol(symbol: String, limit: Int): List<Candle> {
        val select = "select * from candle where symbol = '$symbol' order by date_time desc limit $limit"
        val candles = ArrayList<Candle>()
        connection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(select)
                while (resultSet.next()) {
                    candles.add(
                        Candle(
                            resultSet.getString("symbol"),
                            Date(resultSet.getLong("date_time")),
                            resultSet.getDouble("close")
                        )
                    )
                }
            }
        }
        return candles
    }

    fun save(entity: Candle): Candle {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                val sql = """INSERT INTO candle (symbol, date_time, "close") 
                             VALUES('${entity.symbol}', ${entity.dateTime.time}, ${entity.close}) 
                             ON CONFLICT(symbol, date_time) DO UPDATE SET "close"=excluded."close";"""
                statement.executeUpdate(sql)
            }
        }
        return entity
    }
}
package com.github.shk0da.rsx.persistence.dao

import com.github.shk0da.rsx.persistence.dao.Dao.Companion.connection
import com.github.shk0da.rsx.persistence.entity.Fundamental
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class FundamentalDao : Dao<Fundamental> {

    private val log = LoggerFactory.getLogger(FundamentalDao::class.java)

    companion object {
        private const val createTableSQL = """CREATE TABLE IF NOT EXISTS fundamental (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                    symbol TEXT NOT NULL,
                                                    "name" TEXT NOT NULL,
                                                    date_time LONG DEFAULT 0,
                                                    period_type TEXT,
                                                    currency_code TEXT,
                                                    value_raw DOUBLE DEFAULT 0,
                                                    value_fmt TEXT,
                                                    UNIQUE(symbol, "name", date_time, period_type)
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

    fun findBySymbolAndOptions(symbol: String, options: List<String>): List<Fundamental> {
        val names = options.stream().map { "'$it'" }.collect(Collectors.toList()).joinToString(",")
        val select = "select * from fundamental where symbol = '$symbol' and \"name\" in ($names) order by date_time desc"
        val fundamentals = ArrayList<Fundamental>()
        connection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(select)
                while (resultSet.next()) {
                    fundamentals.add(
                        Fundamental(
                            resultSet.getString("symbol"),
                            resultSet.getString("name"),
                            Date(resultSet.getLong("date_time")),
                            resultSet.getString("period_type"),
                            resultSet.getString("currency_code"),
                            resultSet.getDouble("value_raw"),
                            resultSet.getString("value_fmt")
                        )
                    )
                }
            }
        }
        return fundamentals
    }

    fun save(entity: Fundamental): Fundamental {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                val sql =
                    """INSERT INTO fundamental (symbol, "name", date_time, period_type, currency_code, value_raw, value_fmt) 
                               VALUES('${entity.symbol}', '${entity.name}', ${entity.dateTime.time}, '${entity.periodType}', '${entity.currencyCode}', ${entity.valueRaw}, '${entity.valueFmt}') 
                               ON CONFLICT(symbol, "name", date_time, period_type) 
                               DO UPDATE SET currency_code=excluded.currency_code, value_raw=excluded.value_raw, value_fmt=excluded.value_fmt;"""
                try {
                    statement.executeUpdate(sql)
                } catch (ex: Exception) {
                    log.error("Failed insert or update fundamental: [{}]. {}", entity, ex.message)
                }
            }
        }
        return entity
    }
}
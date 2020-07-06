package com.github.shk0da.rsx.persistence.dao

import com.github.shk0da.rsx.persistence.dao.Dao.Companion.connection
import com.github.shk0da.rsx.persistence.entity.Ticker
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList

class TickerDao : Dao<Ticker> {

    private val log = LoggerFactory.getLogger(TickerDao::class.java)

    companion object {
        private const val createTableSQL = """CREATE TABLE IF NOT EXISTS ticker (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                    "name" TEXT NOT NULL,
                                                    market TEXT NOT NULL,
                                                    description TEXT,
                                                    total_debt LONG DEFAULT 0,
                                                    debt_to_equity DOUBLE DEFAULT 0,
                                                    "type" TEXT NOT NULL,
                                                    sub_type TEXT NOT NULL,
                                                    recommend_1m DOUBLE DEFAULT 0,
                                                    date_time LONG DEFAULT 0,
                                                    UNIQUE("name", market)
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

    fun findLatestDate(): Date? {
        val select = "select date_time from ticker order by date_time desc limit 1"
        connection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(select)
                while (resultSet.next()) {
                    return Date(resultSet.getLong("date_time"))
                }
            }
        }
        return null
    }

    fun saveAll(entities: List<Ticker>) {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                entities.forEach { entity ->
                    val description = entity.description.replace("'", "")
                    val sql =
                        """INSERT INTO ticker ("name", market, description, total_debt, debt_to_equity, "type", sub_type, recommend_1m, date_time) 
                               VALUES('${entity.name}', '${entity.market}', '${description}', ${entity.totalDebt}, ${entity.debtToEquity}, '${entity.type}', '${entity.subtype}', ${entity.recommend1M}, ${entity.dateTime.time}) 
                               ON CONFLICT("name", market) 
                               DO UPDATE SET total_debt=excluded.total_debt, debt_to_equity=excluded.debt_to_equity, recommend_1m=excluded.recommend_1m, date_time=excluded.date_time;"""
                    try {
                        statement.executeUpdate(sql)
                    } catch (ex: Exception) {
                        log.error("Failed insert or update ticker: [{}]. {}", entity, ex.message)
                    }
                }
            }
        }
    }

    fun findAll(limit: Int): List<Ticker> {
        val select = "select * from ticker order by date_time desc limit $limit"
        val tickers = ArrayList<Ticker>()
        connection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(select)
                while (resultSet.next()) {
                    tickers.add(
                        Ticker(
                            resultSet.getString("name"),
                            resultSet.getString("market"),
                            resultSet.getString("description"),
                            resultSet.getLong("total_debt"),
                            resultSet.getDouble("debt_to_equity"),
                            resultSet.getString("type"),
                            resultSet.getString("sub_type"),
                            resultSet.getDouble("recommend_1m"),
                            Date(resultSet.getLong("date_time"))
                        )
                    )
                }
            }
        }
        return tickers
    }
}
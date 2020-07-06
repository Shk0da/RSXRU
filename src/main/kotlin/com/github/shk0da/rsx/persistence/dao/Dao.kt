package com.github.shk0da.rsx.persistence.dao

import java.sql.Connection
import java.sql.DriverManager

interface Dao<T> {

    companion object {
        fun connection(): Connection {
            // used: Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:market.db")
        }
    }
}
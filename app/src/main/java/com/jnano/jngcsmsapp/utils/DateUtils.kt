package com.jnano.jngcsmsapp.utils

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.util.Date

object DateUtils {

    fun getStartOfDayAndTommorowForDate(date: Date): Pair<Long, Long> {
        return Pair(
            ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate().atStartOfDay().toInstant(
                ZoneOffset.UTC).toEpochMilli(),
            ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate().atStartOfDay().plusDays(1).toInstant(
                ZoneOffset.UTC).toEpochMilli()
        )
    }

}
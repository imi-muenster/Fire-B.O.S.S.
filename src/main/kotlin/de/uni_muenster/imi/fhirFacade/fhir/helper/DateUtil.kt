package de.uni_muenster.imi.fhirFacade.fhir.helper

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import ca.uhn.fhir.rest.param.DateParam
import java.util.*

data class DateRange(val min: String, val max: String)

class DateUtil(date: DateParam) {

    lateinit var dateRange: DateRange

    private val MAX_MONTH = 12
    private val MIN_MONTH = 1

    private val MIN_DAYS = 1

    private val MAX_HOURS = 23
    private val MIN_HOURS = 0

    private val MAX_MINUTES = 59
    private val MIN_MINUTES = 0

    private val MAX_SECONDS = 59
    private val MIN_SECONDS = 0

    private val MAX_MILLIS = 999
    private val MIN_MILLIS = 0

    init {
        this.computeMaxAndMinDateTime(date)
    }

    private fun computeMaxAndMinDateTime(theDate: DateParam) {
        when (theDate.precision) {
            TemporalPrecisionEnum.YEAR -> {
                dateRange = DateRange(
                    getMinDate(
                        year = theDate.getYear()
                    ),
                    getMaxDate(
                        year = theDate.getYear()
                    )
                )
            }
            TemporalPrecisionEnum.MONTH -> {
                dateRange = DateRange(
                    getMinDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth()
                    ),
                    getMaxDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth()
                    )
                )
            }
            TemporalPrecisionEnum.DAY -> {
                dateRange = DateRange(
                    getMinDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth()
                    ),
                    getMaxDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth()
                    )
                )
            }
            TemporalPrecisionEnum.MINUTE -> {
                dateRange = DateRange(
                    getMinDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth(),
                        hours = theDate.getHours(),
                        minutes = theDate.getMinutes()
                    ),
                    getMaxDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth(),
                        hours = theDate.getHours(),
                        minutes = theDate.getMinutes()
                    )
                )
            }
            TemporalPrecisionEnum.SECOND -> {
                dateRange = DateRange(
                    getMinDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth(),
                        hours = theDate.getHours(),
                        minutes = theDate.getMinutes(),
                        seconds = theDate.getSeconds()
                    ),
                    getMaxDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth(),
                        hours = theDate.getHours(),
                        minutes = theDate.getMinutes(),
                        seconds = theDate.getSeconds()
                    )
                )
            }
            TemporalPrecisionEnum.MILLI -> {
                dateRange = DateRange(
                    getMinDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth(),
                        hours = theDate.getHours(),
                        minutes = theDate.getMinutes(),
                        seconds = theDate.getSeconds(),
                        millis = theDate.getMillis()
                    ),
                    getMaxDate(
                        year = theDate.getYear(),
                        month = theDate.getMonth(),
                        day = theDate.getDayOfMonth(),
                        hours = theDate.getHours(),
                        minutes = theDate.getMinutes(),
                        seconds = theDate.getSeconds(),
                        millis = theDate.getMillis()
                    )
                )
            }
        }
    }

    private fun getMaxDate(year: Int,
                           month: Int? = null,
                           day: Int? = null,
                           hours: Int? = null,
                           minutes: Int? = null,
                           seconds: Int? = null,
                           millis: Int? = null
    ): String {
        return "$year-" +
                "${"%02d".format(month ?: MAX_MONTH)}-" +
                "%02d".format(day ?: getMaxDayForMonth(month ?: MAX_MONTH)) +
                "T" +
                "${"%02d".format(hours ?: MAX_HOURS)}:" +
                "${"%02d".format(minutes ?: MAX_MINUTES)}:" +
                "${"%02d".format(seconds ?: MAX_SECONDS)}." +
                "%03d".format(millis ?: MAX_MILLIS)
    }

    private fun getMinDate(year: Int,
                           month: Int? = null,
                           day: Int? = null,
                           hours: Int? = null,
                           minutes: Int? = null,
                           seconds: Int? = null,
                           millis: Int? = null
    ): String {
        return "$year-" +
                "${"%02d".format(month ?: MIN_MONTH)}-" +
                "%02d".format(day ?: MIN_DAYS) +
                "T" +
                "${"%02d".format(hours ?: MIN_HOURS)}:" +
                "${"%02d".format(minutes ?: MIN_MINUTES)}:" +
                "${"%02d".format(seconds ?: MIN_SECONDS)}." +
                "%03d".format(millis ?: MIN_MILLIS)
    }

    private fun getMaxDayForMonth(month: Int): Int {
        return when (month) {
            1,3,5,7,8,10,12 -> 31
            2 -> 29
            4,6,9,11 -> 30
            else -> Int.MAX_VALUE
        }
    }

    private fun DateParam.getYear(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.YEAR)
    }

    private fun DateParam.getMonth(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.MONTH) + 1 //JANUARY = 0
    }

    private fun DateParam.getDayOfMonth(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.DAY_OF_MONTH)
    }

    private fun DateParam.getHours(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.HOUR_OF_DAY)
    }

    private fun DateParam.getMinutes(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.MINUTE)
    }

    private fun DateParam.getSeconds(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.SECOND)
    }

    private fun DateParam.getMillis(): Int {
        val date = this.value
        return Calendar.getInstance().apply {
            time = date
        }.get(Calendar.MILLISECOND)
    }


}
package com.gabecast.weather.domain.util

import kotlin.math.roundToInt

fun celsiusToFahrenheit(value: Double): Double = value * 9.0 / 5.0 + 32.0

fun fahrenheitToCelsius(value: Double): Double = (value - 32.0) * 5.0 / 9.0

fun metersPerSecondToMph(value: Double): Double = value * 2.2369362921

fun metersToMiles(value: Double): Double = value / 1609.344

fun pascalsToInHg(value: Double): Double = value / 3386.389

fun mphToKmh(value: Double): Double = value * 1.609344

fun Double.roundToSingleDecimal(): String = if (this.isNaN()) {
    "--"
} else {
    val rounded = (this * 10.0).roundToInt() / 10.0
    if (rounded % 1.0 == 0.0) rounded.roundToInt().toString() else rounded.toString()
}

fun Double.roundToWhole(): String = if (this.isNaN()) "--" else roundToInt().toString()

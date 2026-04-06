package com.a5starcompany.horizonpay.utils

import android.util.Log
import kotlin.collections.get

object CardUtil {
    private const val TAG = "CardUtil"

    private val cardType = mapOf(
        "A000000004" to "MASTER",
        "A000000003" to "VISA",
        "A000000025" to "AMEX",
        "A000000065" to "JCB",
        "A000000152" to "DISCOVER",
        "A000000324" to "DISCOVER",
        "A000000333" to "PBOC",
        "A000000524" to "RUPAY"
    )

    private val cardCurrency = mapOf(
        "156" to "RMB",
        "344" to "HKD",
        "446" to "MOP",
        "458" to "MYR",
        "702" to "SGD",
        "978" to "EUR",
        "036" to "AUD",
        "764" to "THB",
        "784" to "AED",
        "392" to "JPY",
        "360" to "IDR",
        "840" to "USD",
        "566" to "NGN",
        "356" to "INR",
        "364" to "IRR",
        "400" to "JOD",
        "116" to "KHR",
        "480" to "MUR",
        "938" to "SDG"
    )

    fun getCardTypeFromAid(aid: String?): String {
        if (aid.isNullOrEmpty() || aid.length < 10) {
            return ""
        }
        Log.d(TAG, "getCardTypeFromAid: ${aid.length}")
        return cardType[aid.substring(0, 10)] ?: ""
    }

    fun getCurrencyName(code: String?): String {
        return try {
            cardCurrency[code?.take(3)] ?: "UnKnown"
        } catch (e: Exception) {
            e.printStackTrace()
            "UnKnown"
        }
    }
}
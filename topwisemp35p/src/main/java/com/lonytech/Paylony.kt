package com.lonytech

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.topwisemp35p.emvreader.TopWiseDevice
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadState
import com.a5starcompany.topwisemp35p.emvreader.emv.TransactionMonitor
import com.a5starcompany.topwisemp35p.emvreader.printer.PrintTemplate
import com.a5starcompany.topwisemp35p_horizonpay.Topwisemp35pHorizonpay
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.random.Random

/**
 * Main SDK entry point for Paylony integration.
 * This class manages the EMV device and orchestrates transaction flows.
 */
class Paylony(
    val context: Context,
    val terminal: Terminal,
    private val callback: (TransactionMonitor?) -> Unit
) {
    var transactionType: String = ""
    var accDetails: String = ""
    var accountType: String = ""

    private val topWiseDevice: Topwisemp35pHorizonpay by lazy {
        Topwisemp35pHorizonpay(context) { monitor ->
            handleTransactionMonitor(monitor)
        }
    }

    val ivString: String = generateRandomString(16)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Processes updates from the EMV device.
     */
    private fun handleTransactionMonitor(monitor: TransactionMonitor) {
        Log.d("Paylony", "State: ${monitor.state}, Message: ${monitor.message}")
        
        val cardResult = monitor.transactionData
        if (monitor.state == CardReadState.CardData && cardResult != null) {
            // Card read successful, initiate debit request
            val data = mutableMapOf<String?, String?>().apply {
                put("amount", cardResult.amount)
                put("pan", cardResult.applicationPrimaryAccountNumber)
                put("expiry", cardResult.expirationDate)
                put("track2", cardResult.track2Data)
                put("iccData", cardResult.iccDataString)
                put("pinBlock", cardResult.encryptedPinBlock)
                put("sequenceNumber", cardResult.applicationPANSequenceNumber)
                put("terminalId", terminal.terminalId)
                put("transactionType", transactionType)
                put("accountType", accountType)
            }
            
            val headers = mutableMapOf<String?, String?>().apply {
                put("Authorization", "Bearer ${terminal.token}")
                put("serialNumber", serialNumber)
                put("deviceName", "Bearer ${terminal.token}")
                put("Terminal-Auth", hashedString(terminal.rrn , terminal.terminalId))
                put("timestamp", ivString)
            }
            
            performDebitRequest(data, headers, monitor)
        } else {
            // Pass through other states (Loading, Error, etc.) to the caller
            callback(monitor)
        }
    }

    /**
     * Executes the debit API request in the background.
     */
    private fun performDebitRequest(
        data: MutableMap<String?, String?>, 
        headers: MutableMap<String?, String?>, 
        monitor: TransactionMonitor
    ) {
        coroutineScope.launch {
            try {
                // Notify UI that processing has started
                callback(TransactionMonitor(CardReadState.Loading, "Processing payment...", true, monitor.transactionData))
                
                val response = withContext(Dispatchers.IO) {
                    HttpJsonParser(context).makeHttpRequest(
                        "payment/card/debit",
                        "POST",
                        data,
                        headers,
                        ivString
                    )
                }

                print("response from server")
                print(response)
                if (response != null) {
                    val message = response.optString("message", "Transaction finished")
                    val success = response.optBoolean("success", false)
                    callback(TransactionMonitor(CardReadState.CallBackTransResult, message, success, monitor.transactionData))
                } else {
                    callback(TransactionMonitor(CardReadState.CallBackError, "Network request failed", false, monitor.transactionData))
                }
            } catch (e: Exception) {
                Log.e("Paylony", "Error during debit request", e)
                callback(TransactionMonitor(CardReadState.CallBackError, e.message ?: "Unknown error", false, monitor.transactionData))
            }
        }
    }

    // --- Public API methods ---

    fun readCard(amount: String, type: String, details: String) {
        this.transactionType = type
        this.accDetails = details
        topWiseDevice.makePayment(amount)
    }

    @Throws(RemoteException::class)
    fun printDoc(template: PrintTemplate) {
        topWiseDevice.printDoc(template)
    }

    fun closeCardReader() {
        topWiseDevice.cancelcardprocess()
    }

    fun getCardScheme(amount: String) {
        topWiseDevice.getcardsheme(amount)
    }

    fun enterPin(directPin: String, type: String) {
        this.accountType = type
        topWiseDevice.enterpin(directPin)
    }

    val serialNumber: String
        get() = topWiseDevice.serialnumber

    fun getFees(): String {
        return if (terminal.feeBearer == "customer") {
            // Logic for fee calculation can be added here
            "0"
        } else "0"
    }

    @Throws(NoSuchAlgorithmException::class)
    fun hashedString(reference: String?, terminalId: String?): String {
        val join = "$terminalId|$serialNumber|MP35P|$reference"
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(join.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateRandomString(length: Int): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { Random.nextInt(0, charPool.length).let { charPool[it] } }
            .joinToString("")
    }
}

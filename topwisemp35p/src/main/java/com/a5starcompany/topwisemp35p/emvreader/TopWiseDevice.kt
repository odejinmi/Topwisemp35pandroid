package com.a5starcompany.topwisemp35p.emvreader

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadResult
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadState
import com.a5starcompany.topwisemp35p.emvreader.emv.Processor
import com.a5starcompany.topwisemp35p.emvreader.emv.TransactionMonitor
import com.topwise.cloudpos.aidl.printer.AidlPrinter
import com.topwise.cloudpos.aidl.printer.AidlPrinterListener
import com.a5starcompany.topwisemp35p.emvreader.printer.PrintTemplate
import com.a5starcompany.topwisemp35p.emvreader.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class TopWiseDevice(val context: Context, callback: (TransactionMonitor) -> Unit) {
    private val callback: (TransactionMonitor) -> Unit
    private var isCardDetected = false
    init {
        this.callback = callback
    }

    private val printManager: AidlPrinter? = DeviceTopUsdkServiceManager.instance?.printManager

    fun printDoc(template: PrintTemplate) {
        printManager?.addRuiImage(template.printBitmap, 0);
        printManager?.printRuiQueue(object : AidlPrinterListener.Stub() {
            override fun onError(p0: Int) {
               val message = when(p0){
                    0x01-> "printer is out of paper"
                    0x02-> "printer is experiencing high temperature"
                    0x03-> "unknown mistake during printing"
                    0x04-> "printer device is not open"
                    0x05-> "printer device is currently busy"
                    0x06-> "width of the bitmap to be printed exceeds the allowed limit"
                    0x07-> "issue with printing a bitmap"
                    0x08-> "issue with printing a barcode"
                    0x09-> "parameter error related to the print job"
                    0x0A-> "issue with printing text"
                    0x0B-> "the system is asked to print data with anti-string change check and it fails"
                   else -> "unknown error"
               }
                callback.invoke(TransactionMonitor(
                    CardReadState.Printer,
                    message,
                    false,
                    null as CardReadResult?
                ))
//                printListener.onError(p0)
            }

            override fun onPrintFinish() {
                callback.invoke(TransactionMonitor(
                    CardReadState.Printer,
                    "printed successfully",
                    true,
                    null as CardReadResult?
                ))
//                printListener.onPrintFinish()
            }

        })
    }

    val serialnumber: String
        get() = DeviceTopUsdkServiceManager.instance?.systemManager?.serialNo!!

    fun enterpin(directpin: String) {

// Convert the string to a ByteArray
        val pin: ByteArray = BCDASCII.hexStringToBytes(directpin)
//        val pin = call.argument<String>("pin")!!
        Log.i("TAG", "onConfirmInput(), pin = " + BCDASCII.bytesToHexString(pin))
        val mCardNo = PosApplication.getApp().mConsumeData?.cardno
//        var finalPan = ""
//        mCardNo?.let {
//            val numbersOfStars =
//                mCardNo.length - (mCardNo.take(5).length + mCardNo.takeLast(4).length)
//            var stars = ""
//            for (i in 1..numbersOfStars)
//                stars += "*"
//            finalPan = mCardNo.take(5) + stars + mCardNo!!.takeLast(4)
//        }
        PosApplication.getApp().mConsumeData?.pin = pin
        PosApplication.getApp().mConsumeData?.pinBlock = BCDASCII.bytesToHexString(
            Format.pinblock(
                mCardNo,
                directpin
            )
        )

//            if (isOnline) {
//                //socket通信
//                val bundle = Bundle()
//                bundle.putInt(
//                    PacketProcessUtils.PACKET_PROCESS_TYPE,
//                    PacketProcessUtils.PACKET_PROCESS_CONSUME
//                )
////                CardManager.getInstance()
////                    .startActivity(this@PinpadActivity, bundle, PacketProcessActivity::class.java)
//                /*byte[] sendData = PosApplication.Companion.getApp().mConsumeData.getICData();
//                Log.d(TAG, BCDASCII.bytesToHexString(sendData));
//                JsonAndHttpsUtils.sendJsonData(mContext, BCDASCII.bytesToHexString(sendData));*/
//            } else {
////                if (ConsumeData.CARD_TYPE_MAG === mCardType) {
////                    //val intent = Intent(this@PinpadActivity, PacketProcessActivity::class.java)
////                    intent.putExtra(
////                        PacketProcessUtils.PACKET_PROCESS_TYPE,
////                        PacketProcessUtils.PACKET_PROCESS_CONSUME
////                    )
////                    startActivity(intent)
////                } else {
////
////                }
//
//                if (pin == null) {
//                    CardManager.instance.setImportPin("000000")
//                } else {
//
//                }
//            }

        CardManager.instance.setImportPin(directpin)
    }

    private val mCheckCard by lazy { DeviceTopUsdkServiceManager.instance?.getCheckCard() }
    val searchCardTime: Int
        get() = 30000
    val job = Job()
    private val coroutineContext = job + Dispatchers.Main
    private val mainScope = MainScope()
    fun closeCardReader() {
        CardManager.instance.stopCardDealService(context)
        cancelCheckCard()
    }
     fun readCard(amount: String) {
        isCardDetected = false
        PosApplication.getApp().mConsumeData?.amount = amount
        PosApplication.getApp().transactionType = PosApplication.CONSUME
        PosApplication.getApp().processor = Processor.INTERSWITCH
        read()
//        return cardConsumeEmitter
    }

    fun getCardScheme(amount: String) {
        isCardDetected = false
        PosApplication.getApp().transactionType = PosApplication.CARD_SCHEME
        PosApplication.getApp().mConsumeData?.amount = amount
        PosApplication.getApp().processor = Processor.INTERSWITCH
        getScheme()
    }

    private fun read() {
//        this.callback.invoke(TransactionMonitor(
//                CardReadState.Loading,
//                "device error ",
//                true,
//                null as CardReadResult?
//        ))
//        cardConsumeEmitter.emit(CardReadState.Loading)
        CardManager.instance.initCardExceptionCallBack(object : CardManager.CardExceptionCallBack {
            override fun callBackTimeOut() {
                callback.invoke(TransactionMonitor(
                        CardReadState.CardReadTimeOut,
                        "card time out ",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun callBackError(errorCode: Int) {
                callback.invoke(TransactionMonitor(
                        CardReadState.CallBackError,
                        "card time out $errorCode",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun callBackCanceled() {
                callback.invoke(TransactionMonitor(
                        CardReadState.CallBackCanceled,
                        "card canceled ",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun callBackTransResult(result: Int) {

            }

            override fun finishPreActivity() {

            }
        })

        CardManager.instance.setCardFoundListener(object : CardManager.Card {
            override fun searching() {
                callback.invoke(TransactionMonitor(
                        CardReadState.Loading,
                        "card loading",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun onCardDetected(pan: String) {
                if(isCardDetected) return
                isCardDetected = true
                callback.invoke(TransactionMonitor(
                        CardReadState.CardDetected,
                        pan,
                        true,
                        null as CardReadResult?
                ))
            }

            override fun onCardReadResult(cardReadResult: CardReadResult) {
                val shola =  CardReadResult(
                        issuerApplicationData = cardReadResult.issuerApplicationData,
                        applicationVersionNumber = cardReadResult.applicationVersionNumber,
                        transactionType = cardReadResult.transactionType,
                        amount = cardReadResult.amount,
                        amountAuthorized = cardReadResult.amountAuthorized,
                        cardSeqenceNumber = cardReadResult.cardSeqenceNumber,
                        unifiedPaymentIccData = cardReadResult.unifiedPaymentIccData,
                        deviceSerialNumber = cardReadResult.deviceSerialNumber,
                        iccDataString = cardReadResult.iccDataString,
                        authorizationResponseCode = cardReadResult.authorizationResponseCode,
                        nibssIccSubset = cardReadResult.nibssIccSubset,
                        applicationTransactionCounter = cardReadResult.applicationTransactionCounter,
                        terminalVerificationResults = cardReadResult.terminalVerificationResults,
                        expirationDate = cardReadResult.expirationDate,
                        applicationPrimaryAccountNumber = cardReadResult.applicationPrimaryAccountNumber,
                        applicationPANSequenceNumber = cardReadResult.applicationPANSequenceNumber,
                        transactionDate = cardReadResult.transactionDate,
                        cryptogramInformationData = cardReadResult.cryptogramInformationData,
                        dedicatedFileName = cardReadResult.dedicatedFileName,
                        transactionSequenceNumber = cardReadResult.transactionSequenceNumber,
                        transactionSequenceCounter = cardReadResult.transactionSequenceCounter,
                        cryptogram = cardReadResult.cryptogram,
                        track2Data = cardReadResult.track2Data,
                        cardholderVerificationMethod = cardReadResult.cardholderVerificationMethod,
                        applicationInterchangeProfile = cardReadResult.applicationInterchangeProfile,
                        pinBlockDUKPT = cardReadResult.pinBlockDUKPT,
                        pinBlockTrippleDES = cardReadResult.pinBlockTrippleDES,
                        cardScheme = cardReadResult.cardScheme,
                        applicationDiscretionaryData = cardReadResult.applicationDiscretionaryData,
                        unpredictableNumber = cardReadResult.unpredictableNumber,
                        interfaceDeviceSerialNumber = cardReadResult.interfaceDeviceSerialNumber,
                        encryptedPinBlock = cardReadResult.encryptedPinBlock,
                        terminalType = cardReadResult.terminalType,
                        cardHolderName = cardReadResult.cardHolderName,
                        originalDeviceSerial = cardReadResult.originalDeviceSerial,
                        transactionCurrencyCode = cardReadResult.transactionCurrencyCode,
                        terminalCountryCode = cardReadResult.terminalCountryCode,
                        cashBackAmount = cardReadResult.cashBackAmount,
                        terminalCapabilities = cardReadResult.terminalCapabilities,
                        plainPinKey = cardReadResult.plainPinKey,
                        originalPan = cardReadResult.originalPan,
                )
                callback.invoke(TransactionMonitor(
                        CardReadState.CardData,
                        "card data",
                        true,
                        shola
                ))
            }
        })
        CardManager.instance.startCardDealService(context)
    }

    private fun getScheme() {
        this.callback.invoke(TransactionMonitor(
                CardReadState.Loading,
                "device error ",
                true,
                null as CardReadResult?
        ))
        CardManager.instance.initCardExceptionCallBack(object : CardManager.CardExceptionCallBack {
            override fun callBackTimeOut() {
                mainScope.launch {
                    callback.invoke(TransactionMonitor(
                            CardReadState.CardReadTimeOut,
                            "card time out ",
                            true,
                            null as CardReadResult?
                    ))
                }
            }

            override fun callBackError(errorCode: Int) {
                callback.invoke(TransactionMonitor(
                        CardReadState.CallBackError,
                        "card time out $errorCode",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun callBackCanceled() {
                callback.invoke(TransactionMonitor(
                        CardReadState.CallBackCanceled,
                        "card canceled",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun callBackTransResult(result: Int) {

            }

            override fun finishPreActivity() {

            }
        })
        CardManager.instance.setCardFoundListener(object : CardManager.Card {
            override fun searching() {
                callback.invoke(TransactionMonitor(
                        CardReadState.Loading,
                        "Loading ",
                        true,
                        null as CardReadResult?
                ))
            }

            override fun onCardDetected(pan: String) {
                if (isCardDetected) return
                isCardDetected = true
                callback.invoke(TransactionMonitor(
                        CardReadState.CardDetected,
                        pan,
                        true,
                        null as CardReadResult?
                ))
            }

            override fun onCardReadResult(cardReadResult: CardReadResult) {
                val shola =  CardReadResult(
                        issuerApplicationData = cardReadResult.issuerApplicationData,
                        applicationVersionNumber = cardReadResult.applicationVersionNumber,
                        transactionType = cardReadResult.transactionType,
                        amount = cardReadResult.amount,
                        amountAuthorized = cardReadResult.amountAuthorized,
                        cardSeqenceNumber = cardReadResult.cardSeqenceNumber,
                        unifiedPaymentIccData = cardReadResult.unifiedPaymentIccData,
                        deviceSerialNumber = cardReadResult.deviceSerialNumber,
                        iccDataString = cardReadResult.iccDataString,
                        authorizationResponseCode = cardReadResult.authorizationResponseCode,
                        nibssIccSubset = cardReadResult.nibssIccSubset,
                        applicationTransactionCounter = cardReadResult.applicationTransactionCounter,
                        terminalVerificationResults = cardReadResult.terminalVerificationResults,
                        expirationDate = cardReadResult.expirationDate,
                        applicationPrimaryAccountNumber = cardReadResult.applicationPrimaryAccountNumber,
                        applicationPANSequenceNumber = cardReadResult.applicationPANSequenceNumber,
                        transactionDate = cardReadResult.transactionDate,
                        cryptogramInformationData = cardReadResult.cryptogramInformationData,
                        dedicatedFileName = cardReadResult.dedicatedFileName,
                        transactionSequenceNumber = cardReadResult.transactionSequenceNumber,
                        transactionSequenceCounter = cardReadResult.transactionSequenceCounter,
                        cryptogram = cardReadResult.cryptogram,
                        track2Data = cardReadResult.track2Data,
                        cardholderVerificationMethod = cardReadResult.cardholderVerificationMethod,
                        applicationInterchangeProfile = cardReadResult.applicationInterchangeProfile,
                        pinBlockDUKPT = cardReadResult.pinBlockDUKPT,
                        pinBlockTrippleDES = cardReadResult.pinBlockTrippleDES,
                        cardScheme = cardReadResult.cardScheme,
                        applicationDiscretionaryData = cardReadResult.applicationDiscretionaryData,
                        unpredictableNumber = cardReadResult.unpredictableNumber,
                        interfaceDeviceSerialNumber = cardReadResult.interfaceDeviceSerialNumber,
                        encryptedPinBlock = cardReadResult.encryptedPinBlock,
                        terminalType = cardReadResult.terminalType,
                        cardHolderName = cardReadResult.cardHolderName,
                        originalDeviceSerial = cardReadResult.originalDeviceSerial,
                        transactionCurrencyCode = cardReadResult.transactionCurrencyCode,
                        terminalCountryCode = cardReadResult.terminalCountryCode,
                        cashBackAmount = cardReadResult.cashBackAmount,
                        terminalCapabilities = cardReadResult.terminalCapabilities,
                        plainPinKey = cardReadResult.plainPinKey,
                        originalPan = cardReadResult.originalPan,
                )
                callback.invoke(TransactionMonitor(
                        CardReadState.CardData,
                        "card data",
                        true,
                        shola
                ))
            }
        })
        CardManager.instance.setCardSchemeListener(object : CardManager.CardSchemeListener {
            override fun onCardScheme(cardScheme: String, maskedPan: String) {
                callback.invoke(TransactionMonitor(
                        CardReadState.CardDetected,
                        "card detected $cardScheme, $maskedPan",
                        true,
                        null as CardReadResult?
                ))
            }

        })
        CardManager.instance.startCardDealService(context)
    }

    private fun cancelCheckCard() {
        synchronized(this) {
            try {
                if (mCheckCard != null) {
                    mCheckCard!!.cancelCheckCard()
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun onCardRemoved(): CardReadResult {
        TODO("Not yet implemented")
    }
}
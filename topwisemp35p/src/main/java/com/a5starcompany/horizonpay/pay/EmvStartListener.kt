package com.a5starcompany.horizonpay.pay

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.a5starcompany.horizonpay.DeviceHelper
import com.a5starcompany.horizonpay.utils.EmvUtil
import com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadResult
import com.a5starcompany.topwisemp35p.emvreader.emv.Processor
import com.a5starcompany.topwisemp35p.emvreader.util.DukptHelper
import com.a5starcompany.topwisemp35p.emvreader.util.Format
import com.a5starcompany.topwisemp35p.emvreader.util.IPEK_LIVE
import com.a5starcompany.topwisemp35p.emvreader.util.KSN_LIVE
import com.horizonpay.smartpossdk.aidl.cardreader.IAidlCardReader
import com.horizonpay.smartpossdk.aidl.emv.AidlCheckCardListener
import com.horizonpay.smartpossdk.aidl.emv.AidlEmvStartListener
import com.horizonpay.smartpossdk.aidl.emv.CandidateAID
import com.horizonpay.smartpossdk.aidl.emv.EmvFinalSelectData
import com.horizonpay.smartpossdk.aidl.emv.EmvTags
import com.horizonpay.smartpossdk.aidl.emv.EmvTransOutputData
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2
import com.horizonpay.smartpossdk.aidl.magcard.TrackData
import com.horizonpay.smartpossdk.data.EmvConstant
import com.horizonpay.utils.ConvertUtils
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class EmvStartListener (emvL2: IAidlEmvL2, context: Context?, cardReader: IAidlCardReader) : AidlEmvStartListener.Stub() {
    // Cache for EMV tag values to avoid repeated calls
    private val tagCache: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()

    // Constants for EMV tags (better than magic strings)
    private object EmvTagConstants {
//        val STANDARD_TAGS: Array<String?> = arrayOf<String?>("9f26", "9f27", "9f10", "9f37", "9f36", "95", "9a", "9c", "9f02", "5f2a",
//            "9f1a", "82", "9f33", "9f34", "9f03", "84", "9F08", "9f09", "9f35", "9f1e",
//            "9F53", "9f41", "9f63", "9F6E", "9F4C", "9F5D", "9B", "5F34", "50", "9F12",
//            "91", "DF31", "8F"
//        )

        val CONSUME_55_TAGS: Array<String?> = arrayOf<String?>("4F", "82", "95", "9A", "9B", "9C", "5F24", "5F2A", "9F02", "9F03", "9F06",
            "9F10", "9F12", "9F1A", "9F1C", "9F26", "9F27", "9F33", "9F34", "9F36",
            "9F37", "C2", "CD", "CE", "C0", "C4", "C7", "C8"
        )

        val UNIFIED_PAYMENT_TAGS: Array<String?> = arrayOf<String?>("82", "95", "9A", "9C", "5F2A", "9F02", "9F03", "9F10", "9F1A", "9F26",
            "9F33", "9F34", "9F35", "9F36", "9F27", "9F37"
        )

//        val VFD_55_TAGS: Array<String?> = arrayOf<String?>("9F02", "9F03", "9F09", "9F10", "9F15", "9F26", "9F27", "9F33", "9F34",
//            "9F35", "9F36", "9F37", "9F41", "9F1A", "9F1E", "95", "9A", "9C",
//            "5F24", "5F2A", "5F34", "82", "84"
//        )
    }

    // Pre-built StringBuilder for string operations
    private val stringBuilder: StringBuilder = StringBuilder(512)

    private val mEmvL2: IAidlEmvL2 = emvL2
    private val mContext: Context? = context
    private val mCardReader: IAidlCardReader = cardReader
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val creditCard: CardReadResult = CardReadResult()

    private val startTick: Long = System.currentTimeMillis()
    private var cardNum: String? = ""
    private var processor: Processor? = Processor.INTERSWITCH
//    private val emvFlow: Int = EmvConstant.EmvTransFlow.FULL
//    private val mOnlineRespEntitiy: OnlineRespEntitiy?

//    init {
//        this.mOnlineRespEntitiy = null
//    }

    @Throws(RemoteException::class) override fun onRequestAmount() {
        Log.d(LOG_TAG, "emvStartListener onRequestAmount")
        mEmvL2.requestAmountResp(PosApplication.getApp().mConsumeData?.amount)
    }

    @Throws(RemoteException::class) override fun onRequestAidSelect(times: Int, aids: MutableList<CandidateAID?>) {
        Log.d(LOG_TAG, "onRequestAidSelect")
        handler.post(Runnable {selApp(aids)})
    }

    @Throws(RemoteException::class) override fun onFinalSelectAid(emvFinalSelectData: EmvFinalSelectData) {
        Log.d(LOG_TAG, "onFinalSelectAid: " + emvFinalSelectData.aid)
        mEmvL2.requestFinalSelectAidResp(emvFinalSelectData.aid)
    }

    @Throws(RemoteException::class) override fun onConfirmCardNo(cardNo: String) {
        Log.d(LOG_TAG, "onConfirmCardNo: $cardNo")
        logElapsedTime()

        // Cache country code for later use
        cacheTagValue("9F1A")
        cacheTagValue("5F20")

        setupCardData(cardNo)
        mEmvL2.confirmCardNoResp(true)

        handleCardSchemeOrDetection(cardNo)
    }

    @Throws(RemoteException::class) override fun onRequestPin(isOnlinePIN: Boolean, leftTimes: Int) {
        Log.d(LOG_TAG,
            "onCardHolderInputPin isOnlinePin: $isOnlinePIN offlinePIN leftTimes: $leftTimes"
        )

        var pan: String? = getTagValueCached("5A")
        if (pan != null) {
            pan = pan.replace("F", "")
        }

        if (TextUtils.isEmpty(pan)) {
            pan = cardNum
        }

        Log.d(LOG_TAG, "onRequestPin: PAN $pan")
        CardManager.Companion.instance.cardDetected(PosApplication.getApp().mConsumeData?.cardno.toString())
    }

    @Throws(RemoteException::class) override fun onResquestOfflinePinDisp(i: Int) {
        if (i == 0) {
            Log.d(LOG_TAG, "onResquestOfflinePinDisp: PIN OK!")
        } else {
            Log.d(LOG_TAG, "WRONG PIN --> $i Chance Left")
        }
    }

    @Throws(RemoteException::class) override fun onRequestOnline(emvTransOutputData: EmvTransOutputData?) {
        Log.d(TAG, "onRequestOnline()")
        processor = PosApplication.getApp().processor

        // Batch process all EMV data operations
        processEmvDataBatch()

        // Build card result efficiently
        val cardResult: CardReadResult = buildCardReadResult()

        // Set final data and cache
        PosApplication.getApp().mConsumeData?.cardReadResult = cardResult
        CardManager.Companion.instance.setCardReadResult(cardResult)

        logElapsedTime()
    }

    @Throws(RemoteException::class) override fun onFinish(emvResult: Int, emvTransOutputData: EmvTransOutputData) {
        Log.d(LOG_TAG, "CallBack onFinish")
        logElapsedTime()
        emvFinish(emvResult, emvTransOutputData)
    }

    @Throws(RemoteException::class) override fun onError(errCode: Int) {
        Log.e(LOG_TAG, "onError: errcode: $errCode")
        emvFinish(EmvConstant.EmvTransResultCode.ERROR_UNKNOWN, EmvTransOutputData())
        CardManager.Companion.instance.callBackError(errCode)
    }

    // Optimized helper methods
    private fun setupCardData(cardNo: String?) {
        PosApplication.getApp().mConsumeData?.cardType = ConsumeData.CARD_TYPE_IC
        PosApplication.getApp().mConsumeData?.cardno = cardNo
        cardNum = cardNo
    }

    private fun handleCardSchemeOrDetection(cardNo: String) {
        if (PosApplication.getApp().transactionType == PosApplication.CARD_SCHEME) {
            val cardScheme: String = determineCardScheme(cardNo)
            Log.d(TAG, "Card type $cardScheme")
            CardManager.Companion.instance.sendCardScheme(cardScheme, cardNo)
        } else {
            CardManager.Companion.instance.setConfirmCardInfo(true)
        }
    }

    private fun determineCardScheme(cardNo: String): String {
        return if (cardNo.length == 19) {
            "verve"
        } else if (cardNo.length != 19 && cardNo[0] == '4') {
            "visa"
        } else {
            "master"
        }
    }

    // Optimized EMV data processing
    private fun processEmvDataBatch() {
        try {
            // Process all EMV data in batch operations
            this.emvCardInfo
            setExpired()
            setSeqNum()
            setTrack2()
            setConsume55()
            setConsumePositive55()
            this.unifiedPaymentConsume55
        }catch (e: Exception) {
            Log.e(TAG, "Error processing EMV data batch", e)
        }
    }

    private fun buildCardReadResult(): CardReadResult {
        val cardResult = CardReadResult()

        // Use cached values where possible
        cardResult.applicationTransactionCounter = getEmvFieldValue("9F36")
        cardResult.cryptogram = getEmvFieldValue("9F26")
        cardResult.cryptogramInformationData = getEmvFieldValue("9F27")
        cardResult.cardholderVerificationMethod = getEmvFieldValue("9F34")
        cardResult.issuerApplicationData = getEmvFieldValue("9F10")
        cardResult.terminalVerificationResults = getEmvFieldValue("95", 4)
        cardResult.terminalType = getEmvFieldValue("9F35")
        cardResult.amount = PosApplication.getApp().mConsumeData?.amount.toString()
        cardResult.amountAuthorized = getEmvFieldValue("9F02")
        cardResult.applicationVersionNumber = getEmvFieldValue("9F09")
        cardResult.transactionSequenceCounter = getEmvFieldValue("9F41", 4)
        cardResult.transactionDate = getEmvFieldValue("9A", 4)
        cardResult.transactionType = getEmvFieldValue("9C", 4)
        cardResult.unpredictableNumber = getEmvFieldValue("9F37")
        cardResult.interfaceDeviceSerialNumber = getEmvFieldValue("9F1E", 4)
        cardResult.cardHolderName = BCDASCII.hexToAscii(getEmvFieldValue("5F20"))
        cardResult.applicationInterchangeProfile = getEmvFieldValue("82", 4)
        cardResult.dedicatedFileName = getEmvFieldValue("84", 4)
        cardResult.terminalCapabilities = getEmvFieldValue("9F33")
        cardResult.terminalCountryCode = getEmvFieldValue("9F1A")
        cardResult.cashBackAmount = getEmvFieldValue("9F03")
        cardResult.transactionCurrencyCode = getEmvFieldValue("5F2A")
        cardResult.applicationIssuerData = getEmvFieldValue("9F06", 4)

        // Set data from consume data
        val consumeData: ConsumeData? = PosApplication.getApp().mConsumeData
        cardResult.applicationPrimaryAccountNumber = consumeData?.cardno.toString()
        cardResult.expirationDate = consumeData?.expiryData.toString()
        cardResult.track2Data = consumeData?.secondTrackData.toString()
        cardResult.cardSeqenceNumber = consumeData?.serialNum.toString()
        cardResult.iccDataString = BCDASCII.bytesToHexString(consumeData?.icData)
        cardResult.unifiedPaymentIccData = BCDASCII.bytesToHexString(consumeData?.unifiedPaymentIccData!!)

        // Set PIN data efficiently
        setPinData(cardResult, consumeData)

        return cardResult
    }

    private fun setPinData(cardResult: CardReadResult, consumeData: ConsumeData) {
        try {
            val pinHex: String = BCDASCII.bytesToHexString(consumeData.pin!!)
            val pan: String = cardResult.applicationPrimaryAccountNumber

            cardResult.pinBlockDUKPT = DukptHelper.DesEncryptDukpt(
                DukptHelper.getSessionKey(IPEK_LIVE, KSN_LIVE),
                pan,
                pinHex
            )


            cardResult.plainPinKey = BCDASCII.bytesToHexString(Format.pinblock(pan, pinHex))


            cardResult.pinBlock = consumeData.pin.toString()
        }catch (e: Exception) {
            Log.e(TAG, "Error setting PIN data", e)
        }
    }

    // Optimized tag value retrieval with caching
    private fun getTagValueCached(tag: String?): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return tagCache.computeIfAbsent(tag) {tag: String? -> this.getTagValueDirect(tag)}
        }
        return ""
    }

    private fun cacheTagValue(tag: String?) {
        tagCache.put(tag, getTagValueDirect(tag))
    }

    private fun getTagValueDirect(tag: String?): String? {
        try {
            return mEmvL2.getTagValue(tag)
        }catch (e: RemoteException) {
            Log.e(TAG, "Error getting tag value for $tag", e)
            return null
        }
    }

    // Optimized EMV field value extraction
    private fun getEmvFieldValue(tag: String?): String {
        return getEmvFieldValue(tag, 6)
    }

    private fun getEmvFieldValue(tag: String?, substringStart: Int): String {
        val tlvData: ByteArray? = getTlvOptimized(arrayOf<String?>(tag))
        if (tlvData == null) return ""

        val hexString: String = BCDASCII.bytesToHexString(tlvData)
        if (processor == Processor.INTERSWITCH && hexString.length > substringStart) {
            return hexString.substring(substringStart)
        }
        return hexString
    }

    // Optimized TLV operations
    private fun getTlvOptimized(tags: Array<String?>?): ByteArray? {
        try {
            val strResult: String? = DeviceHelper.getEmvHandler()?.getTlvByTags(tags)
            return if (strResult != null) BCDASCII.hexStringToBytes(strResult) else null
        }catch (e: RemoteException) {
            Log.e(TAG, "Failed to get TLV data for tags: " + tags.contentToString(), e)
            return null
        }
    }

    private fun setSeqNum() {
        Log.i(LOG_TAG, "getSeqNum()")
        var cardSeqNum: String? = getEmvFieldValue("5F34", 2)
        Log.d(LOG_TAG, "setSeqNum: $cardSeqNum")
            if (processor === Processor.INTERSWITCH) cardSeqNum =
                cardSeqNum!!.substring(cardSeqNum.length - 2, cardSeqNum.length)
        Log.d(LOG_TAG, "setSeqNum : $cardSeqNum")
        PosApplication.getApp().mConsumeData.serialNum = cardSeqNum
    }
    private fun setExpired() {
        var expired: String? = getEmvFieldValue("5F24")
        if (expired != null && expired.length >= 6) {
            expired = expired.substring(expired.length - 6, expired.length - 2)
        }
        Log.d(TAG, "setExpired: $expired")
        PosApplication.getApp().mConsumeData?.expiryData = expired
    }

    private fun setTrack2() {
        val track2TlvList: ByteArray? = getTlvOptimized(arrayOf("57"))
        if (track2TlvList != null) {
            var track2: String = processTrack2(BCDASCII.bytesToHexString(track2TlvList))
            track2 = if (track2.length > 4) track2.substring(4) else track2
            PosApplication.getApp().mConsumeData?.secondTrackData = track2
            Log.i(TAG, "getTrack2() $track2")
        }
    }

    private fun setConsume55() {
        val consume55TlvList: ByteArray? = getTlvOptimized(EmvTagConstants.CONSUME_55_TAGS)
        if (consume55TlvList != null) {
            Log.d(TAG, "setConsume55 consume55TlvList: " + BCDASCII.bytesToHexString(consume55TlvList))
            PosApplication.getApp().mConsumeData?.icData =consume55TlvList
        }
    }

    private fun setConsumePositive55() {
        val positive55Tag: Array<String?> = arrayOf("95", "9F1E", "9F10", "9F36")
        val positive55TagTlvList: ByteArray? = getTlvOptimized(positive55Tag)
        if (positive55TagTlvList != null) {
            Log.d(TAG, "setConsume55 positive55TagTlvList: " + BCDASCII.bytesToHexString(positive55TagTlvList))
        }
    }

    private val unifiedPaymentConsume55: ByteArray?
        get() {
            val unifiedPaymentConsume55TlvList: ByteArray? = getTlvOptimized(EmvTagConstants.UNIFIED_PAYMENT_TAGS)
            if (unifiedPaymentConsume55TlvList != null) {
                PosApplication.getApp().mConsumeData?.unifiedPaymentIccData = unifiedPaymentConsume55TlvList
                Log.d(TAG, "getUnifiedPaymentConsume55() " + BCDASCII.bytesToHexString(unifiedPaymentConsume55TlvList))
            }
            return unifiedPaymentConsume55TlvList
        }

    // Optimized utility methods
    private fun logElapsedTime() {
        Log.i(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms")
    }

//    private fun extractServiceCode(track2Data: String?): String {
//        if (track2Data == null) return ""
//
//        val indexOfToken: Int = track2Data.indexOf("D")
//        if (indexOfToken == -1 || indexOfToken + 8 > track2Data.length) {
//            return ""
//        }
//
//        val indexOfServiceCode: Int = indexOfToken + 5
//        return track2Data.substring(indexOfServiceCode, indexOfServiceCode + 3)
//    }

    // Existing complex methods (emvFinish, selApp, etc.) remain the same
    // but can be further optimized based on specific requirements
    @Throws(RemoteException::class) private fun emvFinish(emvResult: Int, emvTransOutputData: EmvTransOutputData) {
//        var transactionResultCode: TransactionResultCode = TransactionResultCode.DECLINED_BY_ONLINE
        Log.d(TAG, "emvFinish: $emvResult")

        if (emvResult == EmvConstant.EmvTransResultCode.SUCCESS) {
//            if (emvFlow == EmvConstant.EmvTransFlow.SIMPLE) {
//                transactionResultCode = TransactionResultCode.APPROVED_BY_OFFLINE
//            } else {
//                when (emvTransOutputData.getAcType()){EmvConstant.EmvACType.TC -> transactionResultCode = if (mOnlineRespEntitiy == null) TransactionResultCode.APPROVED_BY_OFFLINE else TransactionResultCode.APPROVED_BY_ONLINE
//                    EmvConstant.EmvACType.ARQC -> {
//                        Log.d(LOG_TAG, "onFinish: ARQC")
//                        if (debug) {
//                            transactionResultCode = TransactionResultCode.APPROVED_BY_ONLINE
//                        } else {
//                            transactionResultCode = if (mOnlineRespEntitiy != null && "00" == mOnlineRespEntitiy.respCode) TransactionResultCode.APPROVED_BY_ONLINE else TransactionResultCode.DECLINED_BY_OFFLINE
//                        }
//                    }}
//            }
        } else if (emvResult == EmvConstant.EmvTransResultCode.EMV_RESULT_NOAPP) {
            handleFallback()
//        } else if (emvResult == EmvConstant.EmvTransResultCode.EMV_RESULT_STOP) {
//            transactionResultCode = TransactionResultCode.ERROR_TRANSCATION_CANCEL
//        } else {
//            transactionResultCode = if (mOnlineRespEntitiy != null && "00" == mOnlineRespEntitiy.respCode) TransactionResultCode.DECLINED_BY_TERMINAL_NEED_REVERSE else TransactionResultCode.ERROR_UNKNOWN

        }

//        Log.d(TAG, "emvFinish: " + transactionResultCode)
    }

    @Throws(RemoteException::class) private fun handleFallback() {
        mCardReader.searchCard(true, false, false, 30, object : AidlCheckCardListener.Stub() {
            @Throws(RemoteException::class) override fun onFindMagCard(trackData: TrackData) {
                Log.d(LOG_TAG, "card NO:" + trackData.cardNo)

                // Process mag card data efficiently
                val fallbackCard = CardReadResult()
                fallbackCard.applicationPrimaryAccountNumber = trackData.cardNo
                fallbackCard.expirationDate = trackData.expiryDate

                logMagCardData(trackData)
                stopSearch()

//                var transactionResultCode: TransactionResultCode = TransactionResultCode.APPROVED_BY_ONLINE
//                if (mOnlineRespEntitiy != null && "00" != mOnlineRespEntitiy.respCode) {
//                    transactionResultCode = TransactionResultCode.DECLINED_BY_OFFLINE
//                }
            }

            @Throws(RemoteException::class) override fun onSwipeCardFail() {}
            @Throws(RemoteException::class) override fun onFindICCard() {}
            @Throws(RemoteException::class) override fun onFindRFCard(i: Int) {}

            @Throws(RemoteException::class) override fun onTimeout() {
                Log.d(LOG_TAG, "fallback onTimeout")

                CardManager.Companion.instance.callBackTimeOut()
            }

            @Throws(RemoteException::class) override fun onCancelled() {
                Log.d(LOG_TAG, "fallback onCancelled")
                CardManager.Companion.instance.callBackCanceled()
            }

            @Throws(RemoteException::class) override fun onError(i: Int) {
                Log.d(LOG_TAG, "fallback onError: $i")
                Log.d(LOG_TAG, "fallback onCancelled")
                CardManager.Companion.instance.callBackError(i)
            }
        })
    }

    private fun logMagCardData(trackData: TrackData) {
        // Reuse StringBuilder for efficient logging
        stringBuilder.setLength(0)
        stringBuilder.append("Card: ").append(trackData.cardNo)
            .append("\nTk1: ").append(trackData.track1Data)
            .append("\nTk2: ").append(trackData.track2Data)
            .append("\nTk3: ").append(trackData.track3Data)
            .append("\ntrackKSN: ").append(trackData.ksn)
            .append("\nExpiryDate: ").append(trackData.expiryDate)
            .append("\nCardholderName: ").append(trackData.cardholderName)
        Log.d(LOG_TAG, "FallBack onFindMagCard: $stringBuilder")
    }

    private fun stopSearch() {
        try {
            mCardReader.cancelSearchCard()
        }catch (e: RemoteException) {
            Log.e(TAG, "Error stopping card search", e)
        }
    }

    private fun selApp(appList: MutableList<CandidateAID?>) {
        val options: Array<String?> = arrayOfNulls<String>(appList.size)
        for (i in appList.indices) {
            options[i] = appList[i]?.appLabel
        }

        val alertBuilder: AlertDialog.Builder = AlertDialog.Builder(mContext)
        alertBuilder.setTitle("Please select app")
        alertBuilder.setItems(options, DialogInterface.OnClickListener {dialogInterface: DialogInterface?, index: Int -> try {
            mEmvL2.requestAidSelectResp(index)
        }catch (e: RemoteException) {
            Log.e(TAG, "Error selecting AID", e)
        }
        })

        val alertDialog: AlertDialog = alertBuilder.create()
        alertDialog.show()
    }

    private val emvCardInfo: Unit
        get() {
            try {
                // Get card sequence number
                val cardsn: String? = mEmvL2.getTagValue(EmvTags.EMV_TAG_IC_PANSN)
                if (!TextUtils.isEmpty(cardsn)) {
                    creditCard.cardSeqenceNumber = cardsn.toString()
                }

                // Get track2 data
                var track2: String? = mEmvL2.getTagValue(EmvTags.EMV_TAG_IC_TRACK2DATA)
                if (TextUtils.isEmpty(track2)) {
                    track2 = mEmvL2.getTagValue(EmvTags.M_TAG_IC_9F6B)
                }

                if ( track2 != null && !TextUtils.isEmpty(track2) &&track2.length > 20) {
                    processTrack2Data(track2)
                }

                // Get cardholder name
                val name: String? = EmvUtil.readCardHolder()
                if (!TextUtils.isEmpty(name)) {
                    creditCard.cardHolderName = ConvertUtils.formatHexString(name)
                }

                Log.d(TAG, "onFindICCard: " + creditCard.track2Data)
                PosApplication.getApp().mConsumeData?.secondTrackData = track2
                PosApplication.getApp().mConsumeData?.cardReadResult = creditCard

            }catch (e: RemoteException) {
                Log.e(TAG, "Error getting EMV card info", e)
            }
        }

    private fun processTrack2Data(track2: String) {
        // Remove F padding
        var track2: String = track2
        if (track2.endsWith("F") || track2.endsWith("f")) {
            track2 = track2.substring(0, track2.length - 1)
        }

        val formatTrack2: String = track2.uppercase(Locale.getDefault()).replace('=', 'D')
        val idx: Int = formatTrack2.indexOf('D')

        if (idx != -1 && idx + 5 < formatTrack2.length) {
            val expDate: String = track2.substring(idx + 1, idx + 5)
            creditCard.expirationDate = expDate
            PosApplication.getApp().mConsumeData?.expiryData = expDate

            val pan: String = track2.substring(0, idx)
            creditCard.applicationPrimaryAccountNumber = pan
            PosApplication.getApp().mConsumeData?.cardno = pan
//            val emvData: EmvData = EmvData("", formatTrack2, this.emvRecordTLV)
            //                creditCard.setEmvData(emvData);
        }
        val name: String? = EmvUtil.readCardHolder()
        println("name of card holder $name")
//        creditCard.cardHolderName = ConvertUtils.formatHexString(name)


        Log.d(TAG, "onFindICCard: " + creditCard.track2Data)
        PosApplication.getApp().mConsumeData?.secondTrackData = track2

        //            PosApplication.getApp().mConsumeData.setSerialNum(creditCard.getCardSeqenceNumber());
        PosApplication.getApp().mConsumeData?.cardReadResult = creditCard
    }


//    val emvRecordTLV: String?
//        get() {
//            val standard_Tags: Array<String?> = arrayOf<String?>("9f26",
//                "9f27",
//                "9f10",
//                "9f37",
//                "9f36",
//                "95",
//                "9a",
//                "9c",
//                "9f02",
//                "5f2a",
//                "9f1a",
//                "82",
//                "9f33",
//                "9f34",
//                "9f03",
//                "84",
//                "9F08",
//                "9f09",
//                "9f35",
//                "9f1e",
//                "9F53",
//                "9f41",
//                "9f63",
//                "9F6E",
//                "9F4C",
//                "9F5D",
//                "9B",
//                "5F34",
//                "50",
//                "9F12",
//                "91",
//                "DF31",
//                "8F"
//            )
//            try {
//                return mEmvL2.getTlvByTags(standard_Tags)
//            }catch (e: RemoteException) {
//                e.printStackTrace()
//            }
//            return null
//        }

    companion object {
        private val LOG_TAG: String = EmvStartListener::class.java.getSimpleName()
        private const val TAG: String = "EmvStartListener"

        // Pre-compiled date formatter (thread-safe)
//        private val DATE_FORMATTER: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        private fun processTrack2(track: String?): String {
            if (track == null) return ""

            // Use StringBuilder for efficient string building
            val builder: StringBuilder = StringBuilder(track.length)
            for (i in 0 ..< track.length) {
                val c: Char = track[i]
                if (c != 'F' && c != 'f') {
                    builder.append(c)
                }
            }
            return builder.toString()
        }

    }}

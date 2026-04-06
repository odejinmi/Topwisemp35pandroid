package com.a5starcompany.horizonpay.pay

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager.Companion.instance
import com.a5starcompany.topwisemp35p.emvreader.util.CardSearchErrorUtil
import com.a5starcompany.horizonpay.utils.EmvUtil.getInitTermConfig
import com.horizonpay.smartpossdk.aidl.cardreader.IAidlCardReader
import com.horizonpay.smartpossdk.aidl.emv.AidlCheckCardListener
import com.horizonpay.smartpossdk.aidl.emv.EmvTransData
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2
import com.horizonpay.smartpossdk.aidl.magcard.TrackData
import com.horizonpay.smartpossdk.data.EmvConstant

class CheckCardListener(
    private val mEmvL2: IAidlEmvL2,
    private val mContext: Context?,
    private val mCardReader: IAidlCardReader
) : AidlCheckCardListener.Stub() {
    private val LOG_TAG: String = CheckCardListener::class.java.getSimpleName()

    private val startTick: Long

    private var emvFlow = EmvConstant.EmvTransFlow.FULL

    init {
        startTick = System.currentTimeMillis()
    }

    @Throws(RemoteException::class)
    override fun onFindMagCard(data: TrackData) {
        Log.d(LOG_TAG, "card NO:" + data.getCardNo())
        //        mCardReadMode = CardReadMode.SWIPE;
//                    creditCard.setCardReadMode(CardReadMode.SWIPE);
//        creditCard.setApplicationPrimaryAccountNumber(data.getCardNo());
//        creditCard.setExpirationDate(data.getExpiryDate());
//        creditCard.setCardHolderName(data.getCardholderName());
//            creditCard.setServiceCode(data.getServiceCode());
//        val magData = MagData(data.getTrack1Data(), data.getTrack2Data())
        //            creditCard.setMagData(magData);
//        mListener.onCardDetected(CardReadMode.SWIPE, creditCard);
        val cardNo = data.getCardNo()
        var track2 = data.getTrack2Data()
        var track3 = data.getTrack3Data()

        Log.d(LOG_TAG, "onFindMagCard cardNo : " + cardNo + " track2 : " + track2)
        if (cardNo == null || isTrack2Error(track2)) {
            stopSearch()
            instance.callBackError(CardSearchErrorUtil.CARD_SEARCH_ERROR_REASON_MAG_READ)
        } else if (isEmvCard(track2)) {
            stopSearch()
            instance.callBackError(CardSearchErrorUtil.CARD_SEARCH_ERROR_REASON_MAG_EMV)
        } else {
            PosApplication.getApp().mConsumeData?.cardType = ConsumeData.CARD_TYPE_MAG
            PosApplication.getApp().mConsumeData?.cardno = cardNo
            PosApplication.getApp().mConsumeData?.expiryData = data.expiryDate
            track2 = track2.replace("=", "D")
            PosApplication.getApp().mConsumeData?.secondTrackData = track2
            if (track3 != null) {
                track3 = track3.replace("=", "D")
                PosApplication.getApp().mConsumeData?.thirdTrackData = track3
            }


            //CardManager.getInstance().startActivity(mContext, null, CardConfirmActivity.class);
        }
    }

    @Throws(RemoteException::class)
    override fun onSwipeCardFail() {
        stopSearch()
        instance.callBackError(CardSearchErrorUtil.CARD_SEARCH_ERROR_REASON_MAG_READ)
    }

    @Throws(RemoteException::class)
    override fun onFindICCard() {
        Log.i(LOG_TAG, "onFindICCard: ")
        Log.i(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms")

        stopSearch()
        Log.i(LOG_TAG, "startEMVProcess>>>>>: ")
        startEMVProcess()
    }

    @Throws(RemoteException::class)
    override fun onFindRFCard(ctlsCardType: Int) {
        Log.d(LOG_TAG, "onFindRFCard: ")
        Log.i(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms")
        PosApplication.getApp().mConsumeData?.cardType = ConsumeData.CARD_TYPE_RF
        stopSearch()
        startEMVProcess()
    }

    @Throws(RemoteException::class)
    override fun onTimeout() {
        Log.d(LOG_TAG, "SearchCard = onTimeout ")
        instance.callBackTimeOut()
    }

    @Throws(RemoteException::class)
    override fun onCancelled() {
        Log.d(LOG_TAG, "SearchCard = onCancelled ")
        instance.callBackCanceled()
    }

    @Throws(RemoteException::class)
    override fun onError(errCode: Int) {
        Log.e(LOG_TAG, "SearchCard = onError " + errCode)
        instance.callBackError(errCode)
    }

    private fun stopSearch() {
        try {
            mCardReader.cancelSearchCard()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun startEMVProcess() {
        initEmvParam()
        Log.d(LOG_TAG, "startEMVProcess: " + PosApplication.getApp().mConsumeData?.amount)
        val emvTransData = EmvTransData()
        Log.d(LOG_TAG, "startEMVProcess: " + PosApplication.getApp().mConsumeData?.amount)
        emvTransData.setAmount(PosApplication.getApp().mConsumeData?.amount?.toLong() ?: 0)
        emvTransData.setOtherAmount(0)
        emvTransData.setForceOnline(true)
        emvTransData.setEmvFlowType(EmvConstant.EmvTransFlow.FULL)
        emvFlow = emvTransData.getEmvFlowType()
        emvTransData.setTransType(1.toByte())
        emvTransData.setTransTime("60000")
        setEmvTransDataExt() // Set ext trans data.
        Log.d(LOG_TAG, "startEMVProcess: " + mEmvL2)
        try {
            mEmvL2.startEmvProcess(emvTransData, EmvStartListener(mEmvL2, mContext, mCardReader))
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun initEmvParam() {
        try {
            mEmvL2.setTermConfig(getInitTermConfig())
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    private fun setEmvTransDataExt() {
        val config = Bundle()
        config.putInt(EmvConstant.EmvTransDataConstants.KERNEL_MODE, 0x01)
        config.putByte(EmvConstant.EmvTransDataConstants.TRANS_TYPE, 0x00.toByte())

        config.putStringArrayList(
            EmvConstant.EmvTerminalConstraints.CONFIG,
            createArrayList<String?>("DF81180170", "DF81190118", "DF811B0130")
        )
        //test
        config.putString(
            EmvConstant.EmvTerminalConstraints.AMEX_DRL_CONFIG,
            "df011adf2303200000df2403010000df2503010000df300138df320101df0100df011adf2303200000df2403010000df2503010000df300138df320101df0100df0100df0100df0100df0100df0100df0100df0100df0100df0100df0100df0100df011adf2303200000df2403010000df2503010000df300138df320101df0100"
        )
        try {
            mEmvL2.setTransDataConfigExt(config)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    private fun <T> createArrayList(vararg elements: T?): ArrayList<T?> {
        val list = ArrayList<T?>()
        for (element in elements) {
            list.add(element)
        }
        return list
    }

    private fun isTrack2Error(track2: String?): Boolean {
        Log.i(LOG_TAG, "isTrack2Error = " + track2)
        //Log.i(TAG, "isTrack2Error length = " + track2.length());
        if (track2 == null || track2.length < 21 || track2.length > 37 || track2.indexOf("=") < 12) {
            return true
        }

        return false
    }


    private fun isEmvCard(track2: String?): Boolean {
        Log.i(LOG_TAG, "isEmvCard: track2: " + track2)
        if ((track2 != null) && (track2.length > 0)) {
            val index = track2.indexOf("=")
            val subTrack2 = track2.substring(index)

            if (subTrack2.get(5) == '2' || subTrack2.get(5) == '6') {
                Log.i(LOG_TAG, "isEmvCard: true")
                return true
            }
        }
        Log.i(LOG_TAG, "isEmvCard: false")
        return false
    }
}
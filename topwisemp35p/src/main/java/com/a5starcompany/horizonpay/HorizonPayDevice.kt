package com.a5starcompany.horizonpay

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.horizonpay.DeviceHelper.getPrinter
import com.a5starcompany.horizonpay.pay.CheckCardListener
import com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadResult
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadState
import com.a5starcompany.topwisemp35p.emvreader.emv.Processor
import com.a5starcompany.topwisemp35p.emvreader.emv.TransactionMonitor
import com.a5starcompany.topwisemp35p.emvreader.printer.PrintTemplate
import com.a5starcompany.topwisemp35p.emvreader.util.Format
import com.horizonpay.smartpossdk.aidl.beeper.IAidlBeeper
import com.horizonpay.smartpossdk.aidl.cardreader.IAidlCardReader
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2
import com.horizonpay.smartpossdk.aidl.pinpad.IAidlPinpad
import com.horizonpay.smartpossdk.aidl.printer.AidlPrinterListener
import com.horizonpay.smartpossdk.aidl.printer.IAidlPrinter
import com.horizonpay.smartpossdk.data.PrinterConst
import com.horizonpay.smartpossdk.data.SysConst.DeviceInfo

class HorizonPayDevice (val context: Context, val callback: (TransactionMonitor) -> Unit) {

    var printer: IAidlPrinter? = null
    var mEmvL2: IAidlEmvL2? = null

    var mCardReader: IAidlCardReader? = null

    private var beeper: IAidlBeeper? = null
    private var mPinPad: IAidlPinpad? = null
    private var isCardDetected = false

    var devInfo: Bundle? = null

    init {
        if (Build.MANUFACTURER == "HorizonPay" && Build.BRAND =="SPRD" && Build.MODEL == "S60" && Build.DEVICE == "sl8541e_1h10_32b") {
            try {
                Handler(Looper.getMainLooper()).postDelayed({
                    mCardReader = DeviceHelper.getCardReader()
                    mEmvL2 = DeviceHelper.getEmvHandler()
                    beeper = DeviceHelper.getDevice()?.getBeeper()
//            Log.d(TAG, "onCreate: " + mEmvL2)
                    mPinPad = DeviceHelper.getPinpad()
                    printer = getPrinter()
                    devInfo = DeviceHelper.getSysHandle()!!.getDeviceInfo()
//            isSupport = mEmvL2!!.isSupport()
                }, 1000) // 5000 milliseconds = 5 seconds
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    val serialnumber: String
        get() = devInfo?.getString(DeviceInfo.DEVICE_SN).toString()

//        get() = DeviceHorizonsdkServiceManager.instance?.systemManager?.deviceInfo?.getString(DeviceInfo.DEVICE_SN).toString()


    fun enterpin(directpin: String) {

// Convert the string to a ByteArray
        val pin: ByteArray? = BCDASCII.hexStringToBytes(directpin)
//        val pin = call.argument<String>("pin")!!
        Log.i("TAG", "onConfirmInput(), pin = " + BCDASCII.bytesToHexString(pin))
        val mCardNo = PosApplication.getApp().mConsumeData?.cardno

        PosApplication.getApp().mConsumeData?.pin = pin
//        PosApplication.getApp().mConsumeData?.pinBlock = bytesToHexString(pin)
        PosApplication.getApp().mConsumeData?.pinBlock = BCDASCII.bytesToHexString(
            Format.pinblock(
                mCardNo.toString(),
                directpin
            )
        )

        mEmvL2!!.requestPinResp(pin, true)

    }

    fun printDoc(template: PrintTemplate){
        try {
            printer!!.printGray = PrinterConst.Gray.LEVEL_3
            printer!!.printBmp(
                true,
                false,
                template.printBitmap,
                0,
                object : AidlPrinterListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onError(i: Int) {
                        val message = when(i){
                            PrinterConst.RetCode.ERROR_PRINT_NOPAPER-> "Print Failed (Please check paper)"
                            PrinterConst.RetCode.ERROR_DEV-> "Print Failed (Error device)"
                            PrinterConst.RetCode.ERROR_DEV_IS_BUSY-> "Print Failed (Device is busy)"
                            PrinterConst.RetCode.ERROR_OTHER-> "Print Failed (Other error)"
                            else -> "Print Failed (Other error)"
                        }
                        Log.d("TAG", "onError: ")
                        callback.invoke(TransactionMonitor(
                            CardReadState.Printer,
                            message,
                            false,
                            null as CardReadResult?
                        ))
                    }

                    @Throws(RemoteException::class)
                    override fun onPrintSuccess() {
                        Log.d("TAG", "onPrintSuccess: ")
                        callback.invoke(TransactionMonitor(
                            CardReadState.Printer,
                            "Print Success",
                            false,
                            null as CardReadResult?
                        ))
                    }
                })
        } catch (e: RemoteException) {
            e.printStackTrace()
        } finally {
//            button!!.setEnabled(true)
        }
    }

    fun readCard(amount: String) {
        isCardDetected = false
        val totalamount = amount.toFloat()*100
        PosApplication.getApp().mConsumeData?.amount = "${totalamount.toInt()}"
        PosApplication.getApp().transactionType = PosApplication.CONSUME
        PosApplication.getApp().processor = Processor.INTERSWITCH
        closeCardReader()
        try {
            //            startEMVProcess();
            mCardReader!!.searchCard(
                true,
                true,
                true,
                100,
                CheckCardListener(mEmvL2!!, context, mCardReader!!)
            )
            callback.invoke(TransactionMonitor(
                CardReadState.Loading,
                "card loading",
                true,
                null as CardReadResult?
            ))
        } catch (e: RemoteException) {
            CardManager.instance.callBackError(e.hashCode())
            e.printStackTrace()
        }

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
                    "card error $errorCode",
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
                CardManager.instance.setCardFoundListener(null)
                callback.invoke(TransactionMonitor(
                    CardReadState.CardData,
                    "card data",
                    true,
                    cardReadResult
                ))
            }
        })

    }


    fun closeCardReader() {
        try {
//            Log.d(TAG, "stopEmvProcess: " + mEmvL2)
            mEmvL2!!.stopEmvProcess()
        } catch (e: RemoteException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }
}

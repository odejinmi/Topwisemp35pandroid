package com.a5starcompany.topwisemp35p.emvreader.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.a5starcompany.topwisemp35p.R
import com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII
import com.a5starcompany.topwisemp35p.emvreader.DeviceTopUsdkServiceManager
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager
import com.a5starcompany.topwisemp35p.emvreader.util.Format
import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad
import com.topwise.cloudpos.aidl.pinpad.GetPinListener

class PinpadDialogFragment : DialogFragment() {
    private var mPinpad: AidlPinpad? = null
    private var mCardType = ConsumeData.CARD_TYPE_MAG
    private var mPinInput: String? = null
    private var mCardNo: String? = null
    private var mAmount: String? = null
    private var mParam: Bundle? = null
    private var mKeytype = 0 // the card type (online/offline card)
    private var mIsCancleInputKey = false
    private var pinTries = 0

    private lateinit var pin1: ImageView
    private lateinit var pin2: ImageView
    private lateinit var pin3: ImageView
    private lateinit var pin4: ImageView
    private lateinit var cardMaskedPan: TextView
    private lateinit var amount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mParam = arguments

        // Initialize parameters from bundle
        if (mParam != null) {
            mKeytype = mParam!!.getInt("keytype")
            pinTries = mParam!!.getInt("pinRetryTimes")
        }
        mAmount = PosApplication.getApp().mConsumeData?.amount
        mCardNo = PosApplication.getApp().mConsumeData?.cardno
        mPinpad = DeviceTopUsdkServiceManager.instance?.getPinpadManager(0)
        mCardType = PosApplication.getApp().mConsumeData?.cardType!!

        CardManager.instance.finishPreActivity()
        showPinpadActivity(mCardNo!!, mAmount!!)
        CardManager.instance.initCardExceptionCallBack(mCallBack)
        if (pinTries == -1) {
            CardManager.instance.stopCardDealService(requireContext())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_pin_pad, container, false)
        amount = view.findViewById(R.id.amount)
        cardMaskedPan = view.findViewById(R.id.cardMaskedPan)

        var finalPan = ""
        mCardNo?.let {
            val numbersOfStars = mCardNo!!.length - (mCardNo!!.take(5).length + mCardNo!!.takeLast(4).length)
            var stars = ""
            for (i in 1..numbersOfStars)
                stars += "*"
            finalPan = mCardNo!!.take(5) + stars + mCardNo!!.takeLast(4)
        }
        amount.text = "N$mAmount"
        cardMaskedPan.text = finalPan

        pin1 = view.findViewById(R.id.digit1)
        pin2 = view.findViewById(R.id.digit2)
        pin3 = view.findViewById(R.id.digit3)
        pin4 = view.findViewById(R.id.digit4)

        return view
    }

    private val mHandler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                1 -> {
                    setText()
                }
                else -> {
                }
            }
        }
    }

    private fun setText() {
        when (mPinInput?.length) {
            1 -> {
                pin1.visibility = View.VISIBLE
                pin2.visibility = View.GONE
                pin3.visibility = View.GONE
                pin4.visibility = View.GONE
            }
            2 -> {
                pin2.visibility = View.VISIBLE
                pin1.visibility = View.VISIBLE
                pin3.visibility = View.GONE
                pin4.visibility = View.GONE
            }
            3 -> {
                pin3.visibility = View.VISIBLE
                pin2.visibility = View.VISIBLE
                pin1.visibility = View.VISIBLE
                pin4.visibility = View.GONE
            }
            4 -> {
                pin4.visibility = View.VISIBLE
                pin2.visibility = View.VISIBLE
                pin3.visibility = View.VISIBLE
                pin1.visibility = View.VISIBLE
            }
            else -> {
                pin1.visibility = View.GONE
                pin2.visibility = View.GONE
                pin3.visibility = View.GONE
                pin4.visibility = View.GONE
            }
        }
    }

    fun showPinpadActivity(cardNo: String, amount: String) {
        object : Thread() {
            override fun run() {
                try {
                    mPinpad!!.setPinKeyboardMode(1)
                    mPinpad!!.getPin(getParam(cardNo, amount), mPinListener)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun getParam(cardNo: String, amount: String): Bundle {
        val param = Bundle()
        param.putInt("wkeyid", 0x01)
        param.putInt("keytype", 0x01)
        param.putInt("key_type", 0x0d)
        param.putByteArray("random", null)
        param.putInt("inputtimes", 1)
        param.putInt("minlength", 4)
        param.putInt("maxlength", 4)
        param.putString("pan", cardNo)
        param.putString("tips", "RMB:$amount")
        param.putBoolean("is_lkl", false)
        return param
    }

    private val mPinListener: GetPinListener = object : GetPinListener.Stub() {
        @Throws(RemoteException::class)
        override fun onInputKey(len: Int, msg: String) {
            mPinInput = msg
            mHandler.sendEmptyMessage(1)
        }

        @Throws(RemoteException::class)
        override fun onError(errorCode: Int) {
            if (ConsumeData.CARD_TYPE_MAG != mCardType) {
                CardManager.instance.setImportPin("")
            }
            CardManager.instance.stopCardDealService(requireContext())
            dismiss()
        }

        @Throws(RemoteException::class)
        override fun onConfirmInput(pin: ByteArray?) {
            var isOnline = false
            if (mParam != null) {
                isOnline = mParam!!.getBoolean("online")
            }

            PosApplication.getApp().mConsumeData?.pin = pin
            PosApplication.getApp().mConsumeData?.pinBlock = BCDASCII.bytesToHexString(
                    Format.pinblock(mCardNo, BCDASCII.bytesToHexString(pin))
            )

            CardManager.instance.setImportPin(BCDASCII.bytesToHexString(pin))
            dismiss()
        }

        @Throws(RemoteException::class)
        override fun onCancelKeyPress() {
            mIsCancleInputKey = true
            if (ConsumeData.CARD_TYPE_MAG != mCardType) {
                CardManager.instance.setImportPin("")
            }
            CardManager.instance.stopCardDealService(requireContext())
            dismiss()
        }

        @Throws(RemoteException::class)
        override fun onStopGetPin() {
            if (ConsumeData.CARD_TYPE_MAG != mCardType) {
                CardManager.instance.setImportPin("")
            }
            CardManager.instance.stopCardDealService(requireContext())
            dismiss()
        }
    }

    var mCallBack: CardManager.CardExceptionCallBack = object : CardManager.CardExceptionCallBack {
        override fun callBackTimeOut() {}
        override fun callBackError(errorCode: Int) {}
        override fun callBackCanceled() {}
        override fun callBackTransResult(result: Int) {}
        override fun finishPreActivity() {}
    }

    companion object {
        private val TAG = StringUtil.TAGPUBLIC + PinpadDialogFragment::class.java.simpleName

        fun newInstance(keytype: Int, pinRetryTimes: Int): PinpadDialogFragment {
            val fragment = PinpadDialogFragment()
            val args = Bundle()
            args.putInt("keytype", keytype)
            args.putInt("pinRetryTimes", pinRetryTimes)
            fragment.arguments = args
            return fragment
        }
    }
}

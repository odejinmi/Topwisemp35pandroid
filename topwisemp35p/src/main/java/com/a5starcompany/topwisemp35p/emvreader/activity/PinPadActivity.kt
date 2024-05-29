package com.a5starcompany.topwisemp35p.emvreader.activity


import android.content.Intent
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.a5starcompany.topwisemp35p.emvreader.DeviceTopUsdkServiceManager
import  com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import  com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager
import com.a5starcompany.topwisemp35p.emvreader.card.CardManager.CardExceptionCallBack
import  com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII
import  com.a5starcompany.topwisemp35p.emvreader.util.CardSearchErrorUtil
import  com.a5starcompany.topwisemp35p.emvreader.util.Format
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad
import com.topwise.cloudpos.aidl.pinpad.GetPinListener
import com.a5starcompany.topwisemp35p.R
import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil

internal class PinpadActivity : AppCompatActivity() {
    //private AidlPinpad mPinpadManager;
    private var mPinpad: AidlPinpad? = null
    private var mCardType = ConsumeData.CARD_TYPE_MAG
    private val mPinBlock: ByteArray? = null
    private var mPinInput: String? = null
    private var mCardNo: String? = null
    private var mAmount: String? = null
    private var mIntent: Intent? = null
    private var mParam: Bundle? = null

    /*******jeremy  add   */
    private var mKeytype = 0 //the card type (online /offlin card)
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
        setContentView(R.layout.activity_pin_pad)
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mParam = intent.extras

        /*******jeremy  add get card type  */
        if (mParam != null) {
            mKeytype = mParam!!.getInt("keytype")
            pinTries = mParam!!.getInt("pinRetryTimes")
        }
        mAmount = PosApplication.getApp().mConsumeData?.amount
        mCardNo = PosApplication.getApp().mConsumeData?.cardno
        mPinpad = DeviceTopUsdkServiceManager.instance?.getPinpadManager(0)
        mCardType = PosApplication.getApp().mConsumeData?.cardType!!
        var finalPan = ""
        mCardNo?.let {
            val numbersOfStars =
                mCardNo!!.length - (mCardNo!!.take(5).length + mCardNo!!.takeLast(4).length)
            var stars = ""
            for (i in 1..numbersOfStars)
                stars += "*"
            finalPan = mCardNo!!.take(5) + stars + mCardNo!!.takeLast(4)
        }
        amount = findViewById(R.id.amount)
        cardMaskedPan = findViewById(R.id.cardMaskedPan)
        amount.text = "N$mAmount"
        cardMaskedPan.text = finalPan
        CardManager.instance.finishPreActivity()
        showPinpadActivity(
            PosApplication.getApp().mConsumeData?.cardno!!,
            PosApplication.getApp().mConsumeData?.amount!!
        )
        CardManager.instance.initCardExceptionCallBack(mCallBack)
        if (pinTries == -1) {
            CardManager.instance.stopCardDealService(applicationContext)
        }



        pin1 = findViewById(R.id.digit1)
        pin2 = findViewById(R.id.digit2)
        pin3 = findViewById(R.id.digit3)
        pin4 = findViewById(R.id.digit4)
    }

    /*******jeremy  add avoid  repetitive create  Activity */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mKeytype = 0X01
        showPinpadActivity(
            PosApplication.getApp().mConsumeData?.cardno!!,
            PosApplication.getApp().mConsumeData?.amount!!
        )
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
        if (TextUtils.isEmpty(mPinInput)) {
//            editTextText.setText("0.00")
//            binding.digit1.visibility = View.GONE
//            binding.digit2.visibility = View.GONE
//            binding.digit3.visibility = View.GONE
//            binding.digit4.visibility = View.GONE
            pin1.visibility = View.GONE
            pin2.visibility = View.GONE
            pin3.visibility = View.GONE
            pin4.visibility = View.GONE
            return
        }
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
        }
    }
    fun showPinpadActivity(cardNo: String, amount: String) {
        object : Thread() {
            override fun run() {
                try {
                    mPinpad!!.setPinKeyboardMode(1)
                    mPinpad!!.getPin(getParam(cardNo, amount), mPinListener)
                } catch (e: RemoteException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun getParam(cardNo: String, amount: String): Bundle {
        Log.i(TAG, "getParam()")
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

    override fun onDestroy() {
        super.onDestroy()
    }

    private val mPinListener: GetPinListener = object : GetPinListener.Stub() {
        @Throws(RemoteException::class)
        override fun onInputKey(len: Int, msg: String) {
            Log.i(TAG, "onInputKey(), len = $len, msg = $msg")
            mPinInput = msg
            mHandler.sendEmptyMessage(1)
        }

        @Throws(RemoteException::class)
        override fun onError(errorCode: Int) {
            Log.i(TAG, "onError(), errorCode = $errorCode")
            if (ConsumeData.CARD_TYPE_MAG != mCardType) {
                CardManager.instance.setImportPin("")
            } else {
            }
            CardManager.instance.stopCardDealService(this@PinpadActivity)
            finish()
        }

        @Throws(RemoteException::class)
        override fun onConfirmInput(pin: ByteArray?) {
            Log.i(TAG, "onConfirmInput(), pin = " + BCDASCII.bytesToHexString(pin))
            var isOnline = false
            if (mIntent != null) {
                val bundle = mIntent!!.extras
                if (bundle != null) {
                    isOnline = bundle.getBoolean("online")
                }
            }
            Log.d(TAG, "isOnline: $isOnline")
            PosApplication.getApp().mConsumeData?.setPin(pin)
            PosApplication.getApp().mConsumeData?.setPinBlock(
                BCDASCII.bytesToHexString(
                    Format.pinblock(
                        mCardNo,
                        BCDASCII.bytesToHexString(pin)
                    )
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
            CardManager.instance.setImportPin(BCDASCII.bytesToHexString(pin))
            finish()

        }

        @Throws(RemoteException::class)
        override fun onCancelKeyPress() {
            Log.i(TAG, "onCancelKeyPress()")
            mIsCancleInputKey = true
            if (ConsumeData.CARD_TYPE_MAG != mCardType) {
                CardManager.instance.setImportPin("")
            }
            CardManager.instance.stopCardDealService(this@PinpadActivity)
            finish()
        }

        @Throws(RemoteException::class)
        override fun onStopGetPin() {
            Log.i(TAG, "onStopGetPin()")
            if (ConsumeData.CARD_TYPE_MAG != mCardType) {
                CardManager.instance.setImportPin("")
            }
            CardManager.instance.stopCardDealService(this@PinpadActivity)
            finish()
        }
    }
    var mCallBack: CardExceptionCallBack = object : CardExceptionCallBack {
        override fun callBackTimeOut() {
            Log.i(TAG, "onDestroy()")
        }

        override fun callBackError(errorCode: Int) {
            Log.i(TAG, "onDestroy()")
        }

        override fun callBackCanceled() {
            Log.i(TAG, "onDestroy()")
        }

        override fun callBackTransResult(result: Int) {
            Log.d(TAG, "callBackTransResult result : $result")
            if (mIsCancleInputKey) {
                return
            }
            var resultDetail: String? = null
            if (result == CardSearchErrorUtil.TRANS_REASON_REJECT) {
                resultDetail = "Transaction refused"
            } else if (result == CardSearchErrorUtil.TRANS_REASON_STOP) {
                resultDetail = "Transaction is stoped"
            } else if (result == CardSearchErrorUtil.TRANS_REASON_FALLBACK) {
                resultDetail = "FALLBACK"
            } else if (result == CardSearchErrorUtil.TRANS_REASON_OTHER_UI) {
                resultDetail = "Please use others function"
            } else if (result == CardSearchErrorUtil.TRANS_REASON_STOP_OTHERS) {
                resultDetail = "Others"
            }
        }

        override fun finishPreActivity() {
            Log.i(TAG, "onDestroy()")
//            finish()
        }
    }



    companion object {
        private val TAG = StringUtil.TAGPUBLIC + PinpadActivity::class.java.simpleName
    }
}
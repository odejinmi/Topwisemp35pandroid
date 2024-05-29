package com.a5starcompany.topwisemp35p.emvreader.card

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentTransaction
import com.a5starcompany.topwisemp35p.emvreader.activity.PinpadDialogFragment
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadResult
import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil
import com.topwise.cloudpos.aidl.card.AidlCheckCardListener

/**
 * Created by zhoulin on 17-9-19.
 */
class CardManager {
    private var cardReadChannel: AidlCheckCardListener.Stub? = null
    private var resultCallBack: CardResultCallBack? = null
    private var exceptionCallBack: CardExceptionCallBack? = null
    private var c: Card? = null
    fun initCardResultCallBack(callBack: CardResultCallBack?) {
        resultCallBack = callBack
    }

    fun initCardExceptionCallBack(callBack: CardExceptionCallBack?) {
        exceptionCallBack = callBack
    }

    fun setCardFoundListener(c: Card?) {
        this.c = c
    }

    /**
     * 消费金额
     *
     * @param amount
     */
    fun setConsumeAmount(amount: String?) {
        if (null == resultCallBack) {
            Log.d(TAG, "setConsumeAmount callBack is null")
            return
        }
        resultCallBack!!.consumeAmount(amount)
    }


    /**
     * 多应用选择
     *
     * @param index
     */
    fun setAidSelect(index: Int) {
        if (null == resultCallBack) {
            Log.d(TAG, "setAidSelect callBack is null")
            return
        }
        resultCallBack!!.aidSelect(index)
    }

    /**
     * 确认是否使用电子现金
     *
     * @param confirm
     */
    fun setEcashTipsConfirm(confirm: Boolean) {
        if (null == resultCallBack) {
            Log.d(TAG, "setEcashTipsConfirm callBack is null")
            return
        }
        resultCallBack!!.eCashTipsConfirm(confirm)
    }

    /**
     * 确认卡信息
     *
     * @param confirm
     */
    fun setConfirmCardInfo(confirm: Boolean) {
        if (null == resultCallBack) {
            Log.d(TAG, "setConfirmCardInfo callBack is null")
            return
        }
        resultCallBack!!.confirmCardInfo(confirm)
    }

    fun setImportPin(pin: String?) {
        if (null == resultCallBack) {
            Log.d(TAG, "setConfirmCardInfo callBack is null")
            return
        }
        resultCallBack!!.importPin(pin)
    }

    /**
     * 身份认证
     *
     * @param auth
     */
    fun setUserAuth(auth: Boolean) {
        if (null == resultCallBack) {
            Log.d(TAG, "setUserAuth callBack is null")
            return
        }
        resultCallBack!!.userAuth(auth)
    }

    /**
     * 联机
     *
     * @param online
     * @param respCode
     * @param icc55
     */
    fun setRequestOnline(online: Boolean, respCode: String?, icc55: String?) {
        if (null == resultCallBack) {
            Log.d(TAG, "setRequestOnline callBack is null")
            return
        }
        resultCallBack!!.requestOnline(online, respCode, icc55)
    }

    interface CardResultCallBack {
        fun consumeAmount(amount: String?) //消费金额
        fun aidSelect(index: Int) //多应用选择
        fun eCashTipsConfirm(confirm: Boolean) //否使用电子现金
        fun confirmCardInfo(confirm: Boolean) //确认卡信息
        fun importPin(pin: String?) //密码
        fun userAuth(auth: Boolean) //身份认证
        fun requestOnline(online: Boolean, respCode: String?, icc55: String?) //请求联机
    }

    /**
     * 回调超时
     */
    fun callBackTimeOut() {
        if (null == exceptionCallBack) {
            Log.d(TAG, "callBackTimeOut callBack is null")
            return
        }
        exceptionCallBack!!.callBackTimeOut()
    }

    /**
     * 回调出错
     *
     * @param errorCode
     */
    fun callBackError(errorCode: Int) {
        if (null == exceptionCallBack) {
            Log.d(TAG, "callBackError callBack is null")
            return
        }
        exceptionCallBack!!.callBackError(errorCode)
    }

    /**
     * 回调取消
     */
    fun callBackCanceled() {
        if (null == exceptionCallBack) {
            Log.d(TAG, "callBackCanceled callBack is null")
            return
        }
        exceptionCallBack!!.callBackCanceled()
    }

    /**
     * 交易结果
     *
     * @param result
     */
    fun callBackTransResult(result: Int) {
        if (null == exceptionCallBack) {
            Log.d(TAG, "callBackTransResult callBack is null")
            return
        }
        exceptionCallBack!!.callBackTransResult(result)
    }

    interface CardExceptionCallBack {
        fun callBackTimeOut() //回调超时
        fun callBackError(errorCode: Int) //回调出错
        fun callBackCanceled() //回调取消
        fun callBackTransResult(result: Int)
        fun finishPreActivity()
    }

    val cardListener: Card?
        get() {
            if (c == null) {
                Log.d(TAG, "No card Listener found")
            }
            return c
        }

    fun cardDetected(number: String) {
        if (c == null) {
            Log.d(TAG, "No card Listener found")
            return
        }
        c!!.onCardDetected(number)
    }


    private var cardSchemeListener: CardSchemeListener? = null

    fun setCardSchemeListener(cardSchemeListener: CardSchemeListener) {
        this.cardSchemeListener = cardSchemeListener
    }

    interface CardSchemeListener {
        fun onCardScheme(cardScheme: String, maskedPan: String)
    }

    fun sendCardScheme(cardScheme: String, maskedPan: String) {
        if (null == cardSchemeListener) {
            Log.d(TAG, "No card Scheme Listener found Listener found")
            return
        }
        cardSchemeListener?.onCardScheme(cardScheme, maskedPan)
    }


    fun setCardReadResult(cardReadResult: CardReadResult) {
        if (c == null) {
            Log.d(TAG, "No card Listener found")
            return
        }
        c!!.onCardReadResult(cardReadResult)
    }

    interface Card {
        fun searching()
        fun onCardDetected(pan: String)
        fun onCardReadResult(cardReadResult: CardReadResult)
    }

    /**
     * 关闭上一个界面
     */
    fun finishPreActivity() {
        if (null == exceptionCallBack) {
            Log.d(TAG, "finishPreActivity callBack is null")
            return
        }
        exceptionCallBack!!.finishPreActivity()
    }

    fun startCardDealService(mContext: Context) {
        Log.d(TAG, "startCardDealService")
        c?.searching()
        val intent = Intent(mContext, CardMoniterService::class.java)
        mContext.startService(intent)
    }

    fun stopCardDealService(mContext: Context) {
        val intent = Intent(mContext, CardMoniterService::class.java)
        mContext.stopService(intent)
    }

    fun startActivity(mContext: Context, bundle: Bundle?, cls: Class<*>?) {
        try {
            val intent = Intent()
            intent.setClass(mContext, cls!!)
            if (null != bundle) intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showdialog(supportFragmentManager: FragmentTransaction, tag: String){
        val pinpadDialog = PinpadDialogFragment.newInstance(1, 3)
        pinpadDialog.show(supportFragmentManager, tag)
    }

    companion object {
        private val TAG = StringUtil.TAGPUBLIC + CardManager::class.java.simpleName
        val instance = CardManager()
    }
}
package com.a5starcompany.topwisemp35p.emvreader.card;

import static com.a5starcompany.topwisemp35p.emvreader.util.EncryptionKt.IPEK_LIVE;
import static com.a5starcompany.topwisemp35p.emvreader.util.EncryptionKt.KSN_LIVE;


import android.content.*;
import android.os.*;
import android.util.*;

import androidx.fragment.app.FragmentTransaction;

import com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvDeviceManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.OnEmvProcessListener;
import com.a5starcompany.topwisemp35p.emvreader.util.DukptHelper;
import com.a5starcompany.topwisemp35p.emvreader.util.Format;
import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil;
import com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII;
import com.a5starcompany.topwisemp35p.emvreader.activity.PinpadActivity;
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication;
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadResult;
import com.a5starcompany.topwisemp35p.emvreader.emv.Processor;
import com.topwise.cloudpos.aidl.emv.*;
import com.topwise.cloudpos.aidl.emv.level2.*;
import com.a5starcompany.topwisemp35p.emvreader.emv.*;
import com.topwise.tool.api.*;
import com.topwise.tool.api.convert.*;
import com.topwise.tool.api.packer.*;
import com.topwise.tool.impl.*;

import java.text.*;
import java.util.*;

public class ICPbocStartListenerSub implements OnEmvProcessListener {
    private static final String TAG = StringUtil.TAGPUBLIC + ICPbocStartListenerSub.class.getSimpleName();

    private Context mContext;
    private FragmentTransaction msupportFragmentManager;
    private EmvManager mPbocManager;
    private boolean isOnline = false;

    private boolean isGetPin = false;
    private Processor processor;

    AidlEmvL2 aidlEmvL2 = EmvDeviceManager.getInstance().getEmvL2();

    public ICPbocStartListenerSub(Context context, FragmentTransaction supportFragmentManager) {
        mContext = context;
        msupportFragmentManager = supportFragmentManager;
        mPbocManager = EmvManager.getInstance();
        CardManager.Companion.getInstance().initCardResultCallBack(callBack);
    }

    @Override
    public void finalAidSelect() throws RemoteException {
//        EmvTerminalInfo emvTerminalInfo = aidlEmvL2.EMV_GetTerminalInfo();
//        emvTerminalInfo.setAucTerminalCountryCode(BCDASCII.hexStringToBytes("0566"));
//        emvTerminalInfo.setAucTransCurrencyCode(BCDASCII.hexStringToBytes("0566"));
//        emvTerminalInfo.setAucTransRefCurrencyCode(BCDASCII.hexStringToBytes("0566"));
//        aidlEmvL2.EMV_SetTerminalInfo(emvTerminalInfo);
        mPbocManager.setTlv("9F1A", BCDASCII.hexStringToBytes("0566"));
        mPbocManager.setTlv("5F2A", BCDASCII.hexStringToBytes("0566"));
        mPbocManager.setTlv("9f3c", BCDASCII.hexStringToBytes("0566"));
        mPbocManager.importFinalAidSelectRes(true);
    }

    /**
     * 请求输入金额 ，简易流程时不回调此方法
     */
    @Override
    public void requestImportAmount(int type) throws RemoteException {
        Log.d(TAG, "requestImportAmount(), type: " + type);
        Log.d(TAG, "requestImportAmount(), Amount: " + PosApplication.getApp().mConsumeData.getAmount());

        String s = "0840";
        mPbocManager.setTlv("9F1A", BCDASCII.hexStringToBytes(s));
        mPbocManager.setTlv("5F2A", BCDASCII.hexStringToBytes(s));
        boolean isSuccess = mPbocManager.importAmount(PosApplication.getApp().mConsumeData.getAmount());
        Log.d(TAG, "isSuccess():" + isSuccess);
    }

    /**
     * 请求提示信息
     */
    @Override
    public void requestTipsConfirm(String msg) throws RemoteException {
        Log.d(TAG, "requestTipsConfirm(), msg: " + msg);
    }

    /**
     * 请求多应用选择
     */
    @Override
    public void requestAidSelect(int times, String[] aids) throws RemoteException {
        Log.d(TAG, "requestAidSelect(), times: " + times + ", aids.length = " + aids.length);

        boolean isSuccess = mPbocManager.importAidSelectRes(0);
        Log.d(TAG, "isSuccess() : " + isSuccess);
    }

    /**
     * 请求确认是否使用电子现金
     */
    @Override
    public void requestEcashTipsConfirm() throws RemoteException {
        Log.d(TAG, "requestEcashTipsConfirm()");

        boolean isSuccess = mPbocManager.importECashTipConfirmRes(false);
        Log.d(TAG, "isSuccess() : " + isSuccess);
    }

    /**
     * 请求确认卡信息
     */
    @Override
    public void onConfirmCardInfo(String cardNo) throws RemoteException {
        Log.d(TAG, "onConfirmCardInfo(), cardNo: " + cardNo);
        isEcCard();
        PosApplication.getApp().mConsumeData.setCardType(ConsumeData.CARD_TYPE_IC);
        PosApplication.getApp().mConsumeData.setCardno(cardNo);
        //CardManager.getInstance().startActivity(mContext, null, CardConfirmActivity.class);
        getIad();
        if (PosApplication.getApp().getTransactionType() == PosApplication.CARD_SCHEME) {
            String cardScheme = "";
            if (cardNo.length() == 19) {
                cardScheme = "verve";
            } else if (cardNo.length() != 19 && cardNo.charAt(0) == '4') {
                cardScheme = "visa";
            } else {
                cardScheme = "master";
            }
            Log.d(TAG, "Card type " + cardScheme);
            CardManager.Companion.getInstance().sendCardScheme(cardScheme, cardNo);
            CardManager.Companion.getInstance().stopCardDealService(mContext);
        } else {
            //CardManager.Companion.getInstance().cardDetected(cardNo);
            CardManager.Companion.getInstance().setConfirmCardInfo(true);
        }

    }

    private String extractServiceCode(String track2Data) {
        int indexOfToken = track2Data.indexOf("D");
        int indexOfServiceCode = indexOfToken + 5;
        int lengthOfServiceCode = 3;
        return track2Data.substring(indexOfServiceCode, indexOfServiceCode + lengthOfServiceCode);
    }

    public static IPacker packer;
    public static IConvert convert;
    public static ITool topTool;

    void getIad() {
        topTool = TopTool.getInstance(mContext);
        packer = topTool.getPacker();
        convert = topTool.getConvert();
        ITlv tlv = packer.getTlv();
        ITlv.ITlvDataObjList list = null;
        try {
            list = tlv.unpack(getConsume55());
        } catch (TlvException e) {
            e.printStackTrace();
        }
        // tag 9F10
        byte[] value9F10 = list.getValueByTag(0x9F10);
        if (value9F10 != null && value9F10.length > 0) {
            String temp = convert.bcdToStr(value9F10);
            Log.d(TAG, "EmvResultUtlis value9F10 ================= " + temp);
        }
    }
    /*void getIad() {
        topTool = TopTool.getInstance(mContext);
        packer = topTool.getPacker();
        convert = topTool.getConvert();
        ITlv tlv = packer.getTlv();

        ITlv.ITlvDataObjList list = null;
        try {
            list = tlv.unpack(getConsume55());
        } catch (TlvException e) {
            e.printStackTrace();
        }
        // tag 9F10
        byte[] value9F10 = list.getValueByTag(0x9F33);
        if (value9F10 != null && value9F10.length > 0) {
            String temp = convert.bcdToStr(value9F10);
            Log.d(TAG, "EmvResultUtlis value9F10 ================= " + temp);
        } else {
            Log.d(TAG, "EmvResultUtlis value9F10 ================= null");

        }
    }*/

    /**
     * @param type     the type of card  1,2: offline pin 3: offline pin
     * @param lastFlag is the last offline pin
     * @param amt      Amount
     * @deprecated
     */
    @Deprecated
    @Override
    public void requestImportPin(int type, boolean lastFlag, String amt, int pinRetryTimes)  {
        Log.d(TAG, "requestImportPin(), type: " + type + "; lasttimeFlag: " + pinRetryTimes + "; amt: " + amt);
        isGetPin = true;
      /*  Bundle param = new Bundle();
        param.putInt("type", type);
        CardManager.getInstance().startActivity(mContext, param, PinpadActivity.class);
  */
        /********* Jeremy  modify 20200602
         * goto the input pin activity
         * pass the pin type to PinpadActivity
         * ************/

        Bundle bundle = new Bundle();
        if (type == 0x03) {
            Log.d(TAG, "requestImportPin: online");
            bundle.putInt("keytype", 0);
            bundle.putBoolean("online", true);
        } else {
            Log.d(TAG, "requestImportPin: offline");

            bundle.putInt("keytype", 1);
            bundle.putBoolean("online", false);

        }


        Log.v(TAG, "requestImportPin");
        Log.v(TAG, "lastFlag " + lastFlag + "|pinRetryTimes " + pinRetryTimes);
        byte pinType;
        if (type == 0x03) {
            /*******online pin **********/
            pinType = 0x00;
            // showResult("please input Pin");
        } else {
            /****offline pin**********/
            pinType = 0x01;
            if (!lastFlag) {
                if (pinRetryTimes == 3) {
                    bundle.putInt("pinRetryTimes", 3);
                } else if (pinRetryTimes == 2) {
                    bundle.putInt("pinRetryTimes", 2);

                } else if (pinRetryTimes == 1) {
                    bundle.putInt("pinRetryTimes", 1);

                } else {
                    //return stop pin
                    bundle.putInt("pinRetryTimes", -1);
                    CardManager.Companion.getInstance().stopCardDealService(mContext);
                }
            } else {

                bundle.putInt("pinRetryTimes", -1);

            }
        }
            if (msupportFragmentManager == null) {
                CardManager.Companion.getInstance().startActivity(mContext, bundle, PinpadActivity.class);
            }else {
                CardManager.Companion.getInstance().showdialog(msupportFragmentManager, "PinpadDialog");
            }

    }

    /**
     * 请求身份认证
     */
    @Override
    public void requestUserAuth(int certype, String certnumber) throws RemoteException {
        Log.d(TAG, "requestUserAuth(), certype: " + certype + "; certnumber: " + certnumber);

        boolean isSuccess = mPbocManager.importUserAuthRes(true);
        Log.d(TAG, "isSuccess() : " + isSuccess);
    }

    /**
     * 请求联机
     */
    @Override
    public void onRequestOnline() throws RemoteException {
        Log.d(TAG, "onRequestOnline()");
        processor = PosApplication.getApp().getProcessor();
        setExpired();
        setSeqNum();
        setTrack2();
        setConsume55();
        setConsumePositive55();
        getUnifiedPaymentConsume55();
       /* isOnline = true;
        if (!isGetPin) {

            Bundle bundle = new Bundle();
            bundle.putBoolean("online", true);
            CardManager.getInstance().startActivity(mContext, bundle, PinpadActivity.class);
        } else {
            //socket通信
            Bundle bundle = new Bundle();
            bundle.putInt(PacketProcessUtils.PACKET_PROCESS_TYPE, PacketProcessUtils.PACKET_PROCESS_CONSUME);
            CardManager.getInstance().startActivity(mContext, bundle, PacketProcessActivity.class);
            Log.d(TAG, "onRequestOnline()");

        *//*byte[] sendData = PosApplication.getApp().mConsumeData.getICData();
        Log.d(TAG, BCDASCII.bytesToHexString(sendData));
        JsonAndHttpsUtils.sendJsonData(mContext, BCDASCII.bytesToHexString(sendData));*//*
        }*/

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        String transmissionDate = sdf.format(new Date());
        CardReadResult cardReadResult = new CardReadResult();
        Log.d(TAG, "onRequestOnline: " + processor.toString());
        cardReadResult.setApplicationTransactionCounter(getApplicationTransactionCounter());
        cardReadResult.setCryptogram(getCryptogram());
        cardReadResult.setCryptogramInformationData(getCryptogramInformationData());
        cardReadResult.setCardholderVerificationMethod(getCVMResult());
        cardReadResult.setIssuerApplicationData(getIAD());
        cardReadResult.setTerminalVerificationResults(getTerminalVerificationResult());
        cardReadResult.setTerminalType(getTerminalType());
        cardReadResult.setAmount(PosApplication.getApp().mConsumeData.getAmount());
        cardReadResult.setAmountAuthorized(getAmountAuthorized());
        cardReadResult.setApplicationVersionNumber(getApplicationVersionNumber());
        getAcquirerIdentifier();
        vfd55();
        cardReadResult.setTransactionSequenceCounter(getTransactionSequenceCounter());
        cardReadResult.setTransactionDate(getTransactionDate());
        cardReadResult.setTransactionType(getTransactionType());
        cardReadResult.setUnpredictableNumber(getUnpredictedNumner());
        cardReadResult.setInterfaceDeviceSerialNumber(getInterfaceDeviceSerialNumber());
        cardReadResult.setCardHolderName(BCDASCII.hexToAscii(getCardHolderName()));
        cardReadResult.setApplicationInterchangeProfile(getApplicationInterchangeProfile());
        cardReadResult.setDedicatedFileName(getDedicatedFileName());

        cardReadResult.setTerminalCapabilities(getTerminalCapability());
        cardReadResult.setTerminalCountryCode(getTerminalCountryCode());
        cardReadResult.setCashBackAmount(getAmountOther());
        cardReadResult.setTransactionCurrencyCode(getTransactionCurrencycode());


        cardReadResult.setApplicationIssuerData(getAid());
        cardReadResult.setApplicationPrimaryAccountNumber(PosApplication.getApp().mConsumeData.getCardno());
        cardReadResult.setExpirationDate(PosApplication.getApp().mConsumeData.getExpiryData());
        cardReadResult.setTrack2Data(PosApplication.getApp().mConsumeData.getSecondTrackData());
        Log.d(TAG, "track2: " + PosApplication.getApp().mConsumeData.getSecondTrackData());
        Log.d(TAG, "service code: " + extractServiceCode(PosApplication.getApp().mConsumeData.getSecondTrackData()));
        Log.d(TAG, "acquiringInstitutionalCode: " + PosApplication.getApp().mConsumeData.getSecondTrackData().substring(0, 6));
        cardReadResult.setCardSeqenceNumber(PosApplication.getApp().mConsumeData.getSerialNum());
        cardReadResult.setIccDataString(BCDASCII.bytesToHexString(PosApplication.getApp().mConsumeData.getICData()));
        cardReadResult.setUnifiedPaymentIccData(BCDASCII.bytesToHexString(PosApplication.getApp().mConsumeData.getUnifiedPaymentIccData()));
        cardReadResult.setPinBlockDUKPT(
                DukptHelper.INSTANCE.DesEncryptDukpt(
                        DukptHelper.INSTANCE.getSessionKey(IPEK_LIVE, KSN_LIVE)
                        , PosApplication.getApp().mConsumeData.getCardno(),
                        BCDASCII.bytesToHexString(PosApplication.getApp().mConsumeData.getPin())
                )
        );
        cardReadResult.setPlainPinKey(BCDASCII.bytesToHexString(
                Format.pinblock(
                        cardReadResult.getApplicationPrimaryAccountNumber(),
                        BCDASCII.bytesToHexString(PosApplication.getApp().mConsumeData.getPin())
                )
        ));
        cardReadResult.setPinBlock(PosApplication.getApp().mConsumeData.getPinBlock());
        PosApplication.getApp().mConsumeData.setCardReadResult(cardReadResult);
        CardManager.Companion.getInstance().setCardReadResult(cardReadResult);
    }


    private String getApplicationTransactionCounter() {
        String[] seqNumTag = new String[]{"9F36"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getApplicationTransactionCounter() " + cardSeqNum);
        return cardSeqNum;
    }


    private String getAmountAuthorized() {
        String[] seqNumTag = new String[]{"9F02"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getAmountAuthorized() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getAcquirerIdentifier() {
        String[] seqNumTag = new String[]{"9F01"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            Log.i(TAG, "getAcquirerIdentifier() full" + cardSeqNum);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);

        }
        Log.i(TAG, "getAcquirerIdentifier() " + cardSeqNum);
        return cardSeqNum;
    }

    //735ED8522707D58A833F17446DD13928
    private String getTransactionCurrencycode() {
        String[] seqNumTag = new String[]{"5F2A"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getTransactionCurrencycode() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getTerminalCountryCode() {
        String[] seqNumTag = new String[]{"9F1A"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getTerminalCountryCode() " + cardSeqNum);
        return cardSeqNum;
    }


    private String getTerminalCapability() {
        String[] seqNumTag = new String[]{"9F33"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getTerminalCapability() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getAmountOther() {
        String[] seqNumTag = new String[]{"9F03"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getTerminalCapability() " + cardSeqNum);
        return cardSeqNum;
    }


    private String getApplicationVersionNumber() {
        String[] seqNumTag = new String[]{"9F09"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            Log.i(TAG, "getApplicationVersionNumber() full" + cardSeqNum);

            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getApplicationVersionNumber() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getTransactionSequenceCounter() {
        String[] seqNumTag = new String[]{"9F41"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {

            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            Log.i(TAG, "getTransactionSequenceCounter() full" + cardSeqNum);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getTransactionSequenceCounter() " + cardSeqNum);
        return cardSeqNum;
    }


    private String getApplicationInterchangeProfile() {


        String[] seqNumTag = new String[]{"82"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;
        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getApplicationInterchangeProfile() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getCryptogram() {


        String[] seqNumTag = new String[]{"9F26"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getCryptogram() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getCryptogramInformationData() {


        String[] seqNumTag = new String[]{"9F27"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getCryptogramInformationData() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getCVMResult() {


        String[] seqNumTag = new String[]{"9F34"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }

        Log.i(TAG, "getCVMResult() " + cardSeqNum);

        return cardSeqNum;
    }

    private String getIAD() {


        String[] seqNumTag = new String[]{"9F10"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getIAD() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getTerminalVerificationResult() {


        String[] seqNumTag = new String[]{"95"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getTerminalVerificationResult()) " + cardSeqNum);
        return cardSeqNum;
    }

    private String getTerminalType() {


        String[] seqNumTag = new String[]{"9F35"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getTerminalType() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getTransactionDate() {


        String[] seqNumTag = new String[]{"9A"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getTransactionDate() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getTransactionType() {

        String[] seqNumTag = new String[]{"9C"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getTransactionType() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getInterfaceDeviceSerialNumber() {

        String[] seqNumTag = new String[]{"9F1E"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getInterfaceDeviceSerialNumber() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getUnpredictedNumner() {


        String[] seqNumTag = new String[]{"9F37"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getUnpredictedNumner() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getCardHolderName() {


        String[] seqNumTag = new String[]{"5F20"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(6);
        }
        Log.i(TAG, "getCardHolderName() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getDedicatedFileName() {


        String[] seqNumTag = new String[]{"84"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getDedicatedFileName() " + cardSeqNum);
        return cardSeqNum;
    }

    private String getAid() {


        String[] seqNumTag = new String[]{"9F06"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(4);
        }
        Log.i(TAG, "getAid() " + cardSeqNum);
        return cardSeqNum;
    }

    /**
     * 返回读取卡片脱机余额结果
     */
    @Override
    public void onReadCardOffLineBalance(String moneyCode, String balance, String secondMoneyCode, String secondBalance) throws RemoteException {
        Log.d(TAG, "onReadCardOffLineBalance(), moneyCode: " + moneyCode + "; balance"
                + "; secondMoneyCode: " + secondMoneyCode + "; secondBalance: " + secondBalance);
    }

    /**
     * 返回读取卡片交易日志结果
     */
    @Override
    public void onReadCardTransLog(PCardTransLog[] log) throws RemoteException {
        Log.d(TAG, "onReadCardTransLog()");
        if (log == null) {
            return;
        }
        Log.d(TAG, "onReadCardTransLog log.length: " + log.length);
    }

    /**
     * 返回读取卡片圈存日志结果
     */
    @Override
    public void onReadCardLoadLog(String atc, String checkCode, PCardLoadLog[] logs) throws RemoteException {
        Log.d(TAG, "onReadCardLoadLog(), atc: " + atc + "; checkCode: " + checkCode + "logs.length: " + logs.length);
        if (logs == null) {
            return;
        }
    }

    /**
     * 交易结果
     * 批准: 0x01
     * 拒绝: 0x02
     * 终止: 0x03
     * FALLBACK: 0x04
     * 采用其他界面: 0x05
     * 其他：0x06
     * EMV简易流程不回调此方法
     */
    @Override
    public void onTransResult(int result) throws RemoteException {
        Log.d(TAG, "onTransResult result: " + result + isOnline);
        CardManager.Companion.getInstance().stopCardDealService(mContext);
        if (!isOnline) {
            CardManager.Companion.getInstance().callBackTransResult(result);
        }
    }

    @Override
    public void onError(int errorCode) throws RemoteException {
        Log.d(TAG, "onError errorCode: " + errorCode);
        CardManager.Companion.getInstance().callBackError(errorCode);
    }


    CardManager.CardResultCallBack callBack = new CardManager.CardResultCallBack() {
        @Override
        public void consumeAmount(String amount) {
            Log.d(TAG, "consumeAmount amount : " + amount);
            if (null != mPbocManager) {
                mPbocManager.importAmount(amount);
            }
        }

        @Override
        public void aidSelect(int index) {
            Log.d(TAG, "aidSelect index : " + index);
            if (null != mPbocManager) {
                mPbocManager.importAidSelectRes(index);
            }
        }

        @Override
        public void eCashTipsConfirm(boolean confirm) {
            Log.d(TAG, "eCashTipsConfirm confirm : " + confirm);
            if (null != mPbocManager) {
                mPbocManager.importECashTipConfirmRes(confirm);
            }
        }

        @Override
        public void confirmCardInfo(boolean confirm) {
            Log.d(TAG, "confirmCardInfo confirm : " + confirm);
            if (null != mPbocManager) {

                mPbocManager.importConfirmCardInfoRes(confirm);

            }
        }

        @Override
        public void importPin(String pin) {
            Log.d(TAG, "importPin pin : " + pin);
            if (null != mPbocManager) {
                mPbocManager.importPin(pin);
            }
        }

        @Override
        public void userAuth(boolean auth) {
            Log.d(TAG, "userAuth auth : " + auth);
            if (null != mPbocManager) {

                mPbocManager.importUserAuthRes(auth);

            }
        }

        @Override
        public void requestOnline(boolean online, String respCode, String icc55) {
            Log.d(TAG, "requestOnline online : " + online + " respCode : " + respCode + " icc55 : " + icc55);
            if (null != mPbocManager) {
                mPbocManager.importOnlineResp(online, respCode, icc55);
            }
        }
    };

    private boolean isEcCard() {
        Log.i(TAG, "isEcCard()");
        String ecCard = "A000000333010106";

        String[] aidTag = new String[]{"84"};
        String cardAid = BCDASCII.bytesToHexString(getTlv(aidTag));

        Log.i(TAG, "cardAid: " + cardAid);
        return cardAid.contains(ecCard);
    }

    private void setExpired() {
        Log.i(TAG, "getExpired()");
        String[] dataTag = new String[]{"5F24"};
        byte[] dataTlvList = getTlv(dataTag);
        String expired = null;

        if (dataTlvList != null) {
            expired = BCDASCII.bytesToHexString(dataTlvList);
            expired = expired.substring(expired.length() - 6, expired.length() - 2);
        }
        Log.d(TAG, "setExpired : " + expired);
        PosApplication.getApp().mConsumeData.setExpiryData(expired);
    }

    private String getConsume552() {
        Log.i(TAG, "getConsume55()");
        /*String[] consume55Tag = new String[]{"9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A",
                "82", "9F1A", "9F03", "9F33", "9F34", "9F35", "9F1E", "84", "9F09",
                "91", "71", "72", "DF32", "DF33", "DF34"};*/
        String[] consume55Tag = new String[]{"4F", "82", "95", "9A", "9B", "9C", "5F24",
                "5F2A", "9F02", "9F03", "9F06", "9F10", "9F12", "9F1A", "9F1C", "9F26",
                "9F27", "9F33", "9F34", "9F36", "9F37", "C2", "CD", "CE", "C0", "C4",
                "C7", "C8"};
        byte[] consume55TlvList = getTlv(consume55Tag);
        String filed55 = BCDASCII.bytesToHexString(consume55TlvList);
        Log.d(TAG, "setConsume55 consume55TlvList : " + filed55);

        topTool = TopTool.getInstance(mContext);
        packer = topTool.getPacker();
        convert = topTool.getConvert();


        ITlv tlv = packer.getTlv();
        ITlv.ITlvDataObjList list = null;
        try {
            list = tlv.unpack(consume55TlvList);
        } catch (TlvException e) {
            e.printStackTrace();
        }
        // tag 9F10
        byte[] value9F10 = list.getValueByTag(0x9F10);
        if (value9F10 != null && value9F10.length > 0) {
            String temp = convert.bcdToStr(value9F10);
            Log.d(TAG, "value9F10 value9F10 : " + temp);
        }

        String[] consume9F10Tag = new String[]{"9F10"};
        byte[] consume9F10Tags = getTlv(consume9F10Tag);
        String f10Str = BCDASCII.bytesToHexString(consume9F10Tags);
        Log.d(TAG, "f10Str f10Str : " + f10Str);
        return filed55;
    }


    private void setSeqNum() {

        Log.i(TAG, "getSeqNum()");
        String[] seqNumTag = new String[]{"5F34"};
        byte[] seqNumTlvList = getTlv(seqNumTag);
        String cardSeqNum = null;

        if (seqNumTlvList != null) {
            cardSeqNum = BCDASCII.bytesToHexString(seqNumTlvList);
            if (processor == Processor.INTERSWITCH)
                cardSeqNum = cardSeqNum.substring(cardSeqNum.length() - 2, cardSeqNum.length());
        }
        Log.d(TAG, "setSeqNum : " + cardSeqNum);
        PosApplication.getApp().mConsumeData.setSerialNum(cardSeqNum);
    }

    private void setTrack2() {
        String[] track2Tag = new String[]{"57"};
        byte[] track2TlvList = getTlv(track2Tag);

        byte[] temp = new byte[track2TlvList.length - 2];
        System.arraycopy(track2TlvList, 2, temp, 0, temp.length);
        String track2 = processTrack2(BCDASCII.bytesToHexString(track2TlvList));
        track2 = track2.substring(4);
        PosApplication.getApp().mConsumeData.setSecondTrackData(track2);
        Log.i(TAG, "getTrack2() " + track2);

    }

    private static String processTrack2(String track) {
        Log.i(TAG, "processTrack2()");
        StringBuilder builder = new StringBuilder();
        String subStr = null;

        String resultStr = null;
        for (int i = 0; i < track.length(); i++) {
            subStr = track.substring(i, i + 1);
            if (!subStr.endsWith("F")) {
                /*if(subStr.endsWith("D")) {
                    builder.append("=");
                } else {*/
                builder.append(subStr);
                /*}*/
            }
        }
        resultStr = builder.toString();
        return resultStr;
    }

    private void setConsume55() {
        Log.i(TAG, "setConsume55()");
        /*String[] consume55Tag = new String[]{"9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A",
                "82", "9F1A", "9F03", "9F33", "9F34", "9F35", "9F1E", "84", "9F09",
                "91", "71", "72", "DF32", "DF33", "DF34"};*/
        String[] consume55Tag = new String[]{"4F", "82", "95", "9A", "9B", "9C", "5F24",
                "5F2A", "9F02", "9F03", "9F06", "9F10", "9F12", "9F1A", "9F1C", "9F26",
                "9F27", "9F33", "9F34", "9F36", "9F37", "C2", "CD", "CE", "C0", "C4",
                "C7", "C8"};
        byte[] consume55TlvList = getTlv(consume55Tag);
        Log.d(TAG, "setConsume55 consume55TlvList : " + BCDASCII.bytesToHexString(consume55TlvList));
        PosApplication.getApp().mConsumeData.setICData(consume55TlvList);
    }


    private void vfd55() {
        Log.i(TAG, "setConsume55()");
        /*String[] consume55Tag = new String[]{"9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A",
                "82", "9F1A", "9F03", "9F33", "9F34", "9F35", "9F1E", "84", "9F09",
                "91", "71", "72", "DF32", "DF33", "DF34"};*/
//        String[] vfd55 = new String[]{
//                "9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A" ,"5F34"
//                , "82", "9F1A", "9F03", "9F33", "84",
//                 "9F34", "9F35", "4F"
//        };
        String[] vfd55 = new String[]{
                // "9F01",
                "9F02"
                , "9F03"
                , "9F09"
                , "9F10"
                , "9F15"
                , "9F26"
                , "9F27"
                , "9F33"
                , "9F34"
                , "9F35"
                , "9F36"
                , "9F37"
                , "9F41"
                , "9F1A"
                , "9F1E"
                , "95"
                , "9A"
                , "9C"
                , "5F24"
                , "5F2A"
                , "5F34"
                , "82"
                , "84"
        };

//        byte[] consume55TlvList = getTlv(vfd55);
//        Log.d(TAG, "vfd55: " + BCDASCII.bytesToHexString(consume55TlvList));
        byte[] consume55TlvList = getTlv(vfd55);
        String filed55 = BCDASCII.bytesToHexString(consume55TlvList);
        Log.d(TAG, "setConsume55 consume55TlvList : " + filed55);

        topTool = TopTool.getInstance(mContext);
        packer = topTool.getPacker();
        convert = topTool.getConvert();


        ITlv tlv = packer.getTlv();
        ITlv.ITlvDataObjList list = null;
        try {
            list = tlv.unpack(consume55TlvList);
        } catch (TlvException e) {
            e.printStackTrace();
        }
        // tag 9F10
        byte[] value9F10 = list.getValueByTag(0x9F10);
        if (value9F10 != null && value9F10.length > 0) {
            String temp = convert.bcdToStr(value9F10);
            Log.d(TAG, "value9F10 value9F10 : " + temp);
        }

        String[] consume9F10Tag = new String[]{"9F01"};
        byte[] consume9F10Tags = getTlv(consume9F10Tag);
        String f10Str = BCDASCII.bytesToHexString(consume9F10Tags);
        Log.d(TAG, "9F01Str 9f01Str : " + f10Str);
        PosApplication.getApp().mConsumeData.setICData(consume55TlvList);
    }

    private byte[] getUnifiedPaymentConsume55() {
        Log.i(TAG, "getUnifiedPaymentConsume55()");

        String[] consume55Tag = new String[]{"82", "95", "9A", "9C",
                "5F2A", "9F02", "9F03", "9F10", "9F1A", "9F26", "9F33", "9F34", "9F35", "9F36", "9F27", "9F37"};
        byte[] unifiedPaymentConsume55TlvList = getTlv(consume55Tag);
        PosApplication.getApp().mConsumeData.setUnifiedPaymentIccData(unifiedPaymentConsume55TlvList);
        Log.d(TAG, "getUnifiedPaymentConsume55() " + BCDASCII.bytesToHexString(unifiedPaymentConsume55TlvList));
        return unifiedPaymentConsume55TlvList;
    }

    private byte[] getConsume55() {
        Log.i(TAG, "getConsume55()");
        /*String[] consume55Tag = new String[]{"9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A",
                "82", "9F1A", "9F03", "9F33", "9F34", "9F35", "9F1E", "84", "9F09",
                "91", "71", "72", "DF32", "DF33", "DF34"};*/
        String[] consume55Tag = new String[]{"4F", "82", "95", "9A", "9B", "9C", "5F24",
                "5F2A", "9F02", "9F03", "9F06", "9F09", "9F10", "9F12", "9F1A", "9F1C", "9F26",
                "9F27", "9F33", "9F34", "9F36", "9F37", "C2", "CD", "CE", "C0", "C4",
                "C7", "C8"};


        byte[] consume55TlvList = getTlv(consume55Tag);

        return consume55TlvList;
    }
//9F79,


    private void setConsumePositive55() {
        Log.i(TAG, "getConsumePositive55()");
        String[] postive55Tag = new String[]{"95", "9F1E", "9F10", "9F36"};
        byte[] postive55TagTlvList = getTlv(postive55Tag);
        Log.d(TAG, "setConsume55 postive55TagTlvList : " + BCDASCII.bytesToHexString(postive55TagTlvList));
    }
//
//    private byte[] getTlv(String[] tags) {
//        StringBuilder sb = new StringBuilder();
//        for (String tag : tags) {
//            byte[] value = mPbocManager.getTlv(tag);
//            if (value != null && value.length > 0) {
//                Tlv<String, Integer, String> tlv = new Tlv<>(
//                        tag, value.length, BCDASCII.bytesToHexString(value));
//                sb.append(EmvTlvUtils.tlvToString(tlv));
//            }
//        }
//        return BCDASCII.hexStringToBytes(sb.toString());
//    }


//    private byte[] getTlv(String[] tags) {
//        StringBuilder sb = new StringBuilder();
//        for (String tag : tags) {
//            byte[] tempByte = mPbocManager.getTlv(tag);
//            String strResult = BCDASCII.bytesToHexString(tempByte);
//            Log.d(TAG, "temp: " + strResult);
//            sb.append(strResult);
//        }
//        return BCDASCII.hexStringToBytes(sb.toString());
//    }

    private byte[] getTlv(String[] tags) {
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            byte[] tempByte = mPbocManager.getTlv(tag);
            String strResult = BCDASCII.bytesToHexString(tempByte);
            Log.d(TAG, "temp: " + strResult);
            sb.append(strResult);
        }
        return BCDASCII.hexStringToBytes(sb.toString());
    }

}
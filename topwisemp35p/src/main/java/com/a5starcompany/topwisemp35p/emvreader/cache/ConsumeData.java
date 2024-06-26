package com.a5starcompany.topwisemp35p.emvreader.cache;

import android.util.*;

import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadResult;
import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil;


public class ConsumeData {
    private static final String TAG = StringUtil.TAGPUBLIC + "ConsumeDat";
    public static final int CONSUME_TYPE_CARD = 1;
    public static final int CONSUME_TYPE_SCAN = 2;
    public static final int CONSUME_TYPE_CASHBACK = 3;

    public static final int CARD_TYPE_MAG = 100;
    public static final int CARD_TYPE_IC = 101;
    public static final int CARD_TYPE_RF = 102;

    private String mAmount;

    private int mConsumeType;
    private int mCardType;

    private String mScanResult;
    private String mCardno;
    private String mSerialNum;
    private String mExpiryData;
    private String mSecondTrackData;
    private String mThirdTrackData;
    private String PINBLOCK;
    private byte[] mPinBlock;
    private byte[] mICData;
    private byte[] mICPositiveData;
    private byte[] unifiedPaymentIccData;
    private byte[] mKsnValue;
    private CardReadResult cardReadResult;

    public void setCardReadResult(CardReadResult cardReadResult) {
        this.cardReadResult = cardReadResult;
    }

    public CardReadResult getCardReadResult() {
        return cardReadResult;
    }

    public byte[] getKsnValue() {
        return mKsnValue;
    }

    public void setKsnValue(byte[] ksnValue) {
        mKsnValue = ksnValue;
    }

    public void setPinBlock(String pinBlock) {
        PINBLOCK = pinBlock;
    }

    public String getPinBlock() {
        return PINBLOCK;
    }

    public void setConsumeType(int type) {
        mConsumeType = type;
    }

    public int getConsumeType() {
        return mConsumeType;
    }

    public void setAmount(String amount) {
        mAmount = amount;
    }

    public String getAmount() {
        return mAmount;
    }

    public void setCardType(int cardType) {
        mCardType = cardType;
    }

    public int getCardType() {
        return mCardType;
    }

    public void setCardno(String cardno) {
        Log.i(TAG, "setCardno():" + cardno);
        mCardno = cardno;
    }

    public String getCardno() {
        Log.i(TAG, "getCardno():" + mCardno);
        return mCardno;
    }

    public void setExpiryData(String expiryData) {
        mExpiryData = expiryData;
    }

    public String getExpiryData() {
        return mExpiryData;
    }

    public void setSerialNum(String expiryData) {
        mSerialNum = expiryData;
    }

    public String getSerialNum() {
        return mSerialNum;
    }


    public void setSecondTrackData(String secondTrackData) {
        Log.i("lakaladebug", "secondTrackData = " + secondTrackData);
        mSecondTrackData = secondTrackData;
    }

    public String getSecondTrackData() {
        return mSecondTrackData;
    }

    public void setThirdTrackData(String thirdTrackData) {
        Log.i("lakaladebug", "thirdTrackData = " + thirdTrackData);

        mThirdTrackData = thirdTrackData;
    }

    public String getThirdTrackData() {
        return mThirdTrackData;
    }

    public void setPin(byte[] pin) {
        mPinBlock = pin;
    }

    public byte[] getPin() {
        return mPinBlock;
    }

    public void setICData(byte[] icData) {
        mICData = icData;
    }

    public byte[] getICData() {
        return mICData;
    }

    public void setICPositiveData(byte[] icPositiveData) {
        mICPositiveData = icPositiveData;
    }

    public void setUnifiedPaymentIccData(byte[] unifiedPaymentIccData) {
        this.unifiedPaymentIccData = unifiedPaymentIccData;
    }

    public byte[] getUnifiedPaymentIccData() {
        return this.unifiedPaymentIccData;
    }

    public byte[] getICPositiveData() {
        return mICPositiveData;
    }

    public void setScanResult(String scanResult) {
        mScanResult = scanResult;
    }

    public String getScanResult() {
        return mScanResult;
    }

    public void clearConsumeDate() {
        mAmount = null;
        mConsumeType = 0;
        mCardType = 0;
        mScanResult = null;
        mCardno = null;
        mSerialNum = null;
        mExpiryData = null;
        mSecondTrackData = null;
        mThirdTrackData = null;
    }
}
package com.a5starcompany.topwisemp35p.emvreader.emv.impl;

import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.a5starcompany.topwisemp35p.emvreader.database.table.Aid;
import com.a5starcompany.topwisemp35p.emvreader.database.table.DBManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvDeviceManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvTransData;
import com.a5starcompany.topwisemp35p.emvreader.emv.OnEmvProcessListener;
import com.a5starcompany.topwisemp35p.emvreader.util.CommonFunction;
import com.a5starcompany.topwisemp35p.emvreader.util.SDKLog;
import com.topwise.cloudpos.aidl.emv.level2.AidlAmex;
import com.topwise.cloudpos.aidl.emv.level2.AidlEntry;
import com.topwise.cloudpos.aidl.emv.level2.AidlPaypass;
import com.topwise.cloudpos.aidl.emv.level2.AidlPaywave;
import com.topwise.cloudpos.aidl.emv.level2.AidlPure;
import com.topwise.cloudpos.aidl.emv.level2.AidlQpboc;
import com.topwise.cloudpos.aidl.emv.level2.Combination;
import com.topwise.cloudpos.aidl.emv.level2.PreProcResult;
import com.topwise.cloudpos.aidl.emv.level2.TransParam;
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad;
import com.topwise.cloudpos.struct.BytesUtil;
import com.topwise.cloudpos.struct.Tlv;
import com.topwise.cloudpos.struct.TlvList;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


/**
 * 外卡处理类
 *
 * @author xukun
 * @version 1.0.0
 * @date 19-8-16
 */

public class ClsCardProcess implements EmvProcessInterface {

    private static final String TAG = ClsCardProcess.class.getSimpleName();
    private volatile static ClsCardProcess process = null;

    private static final int LATCH_COUNT = 1;

    private AidlPinpad pinPad = EmvDeviceManager.getInstance().getPinpadManager();
    private AidlEntry entryL2 = EmvDeviceManager.getInstance().getL2Entry();
    private AidlPaywave paywave = EmvDeviceManager.getInstance().getL2Paywave();
    private AidlPaypass paypass = EmvDeviceManager.getInstance().getL2Paypass();
    private AidlPure pure = EmvDeviceManager.getInstance().getL2Pure();
    private AidlAmex amex = EmvDeviceManager.getInstance().getL2Amex();
    private AidlQpboc qpboc = EmvDeviceManager.getInstance().getL2Qpboc();
    private CountDownLatch mDownLatch;

    private String mAmount = null;
    private byte[] mCardNo = null;
    private static int aidCount = 0;
    private boolean isEndEmv = false;
    private String mImportAmt = null;
    private PayDataUtil.CallbackSort mCallbackSort = PayDataUtil.CallbackSort.DEFAULT_MENU;
    private TransParam mTransParam = null;
    private byte cardPayType = -1;

    private OnEmvProcessListener mEmvProcessListener;
    private EmvTransData mEmvTransData;
    private StartPayThread mPayThread = null;
    private TlvList mTlvList = null;
    //private Map<String, byte[]> mTlvMap = null;
    private DBManager db = DBManager.getInstance();

    private ClsCardProcess() {
        //mTlvMap = new HashMap<>();
        mTlvList = new TlvList();
    }

    public static ClsCardProcess getInstance() {
        if (process == null) {
            synchronized (ClsCardProcess.class) {
                if (process == null) {
                    process = new ClsCardProcess();
                }
            }
        }
        return process;
    }

    public int processEntryLib(EmvTransData transData, OnEmvProcessListener listener) throws RemoteException {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "processEntryLib");
        isEndEmv = false;
        mImportAmt = null;
        cardPayType = -1;
        mEmvProcessListener = listener;
        mEmvTransData = transData;
        if (mPayThread != null && mPayThread.isAlive()) {
            mPayThread.interrupt();
            mPayThread = null;
        }
        byte[] version = new byte[64];
        int i = 0;
        int ret = entryL2.getVersion(version, i);
        if (ret == PayDataUtil.EMV_OK) {
            try {
                String buffer = BytesUtil.bytes2HexString(version);
                if (buffer.contains("00")) {
                    version = BytesUtil.hexString2Bytes(buffer.split("00")[0]);
                }
                SDKLog.d(TAG, "entryLib version: " + new String(version, "gbk"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return buildCandidate();
    }

    @Override
    public boolean importAmount(String amt) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importAmount amt: " + amt);
        if (amt != null && mEmvTransData != null &&
                mCallbackSort == PayDataUtil.CallbackSort.REQUEST_IMPORT_AMT && mDownLatch != null) {
            mImportAmt = amt;
            mAmount = new PayDataUtil().getFixedAmount(amt);
            SDKLog.d(TAG, "amount in data: " + mAmount);
            mDownLatch.countDown();
            return true;
        }
        isEndEmv = true;
        return false;
    }

    @Override
    public boolean importFinalAidSelectRes(boolean res) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importFinalAidSelectRes" +
                " res: " + res);
        if (mDownLatch != null && mCallbackSort == PayDataUtil.CallbackSort.REQUEST_FINAL_AID_SELECT) {
            mDownLatch.countDown();
            return true;
        }
        isEndEmv = true;
        return false;
    }

    @Override
    public boolean importAidSelectRes(int index) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importAidSelectRes" +
                " index: " + index);
        if (mDownLatch != null && mCallbackSort == PayDataUtil.CallbackSort.REQUEST_AID_SELECT) {
            mDownLatch.countDown();
            return true;
        }
        isEndEmv = true;
        return false;
    }

    @Override
    public boolean importPin(String pin) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importPin" +
                " pin: " + pin);
        if (mDownLatch != null && mCallbackSort == PayDataUtil.CallbackSort.REQUEST_IMPORT_PIN) {
            if (!TextUtils.isEmpty(pin)) {
                mDownLatch.countDown();
                return true;
            }
        }
        isEndEmv = true;
        return false;
    }

    @Override
    public boolean importUserAuthRes(boolean res) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importUserAuthRes" +
                " res: " + res);
        return false;
    }

    @Override
    public boolean importMsgConfirmRes(boolean confirm) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importMsgConfirmRes" +
                " confirm: " + confirm);
        return false;
    }

    @Override
    public boolean importECashTipConfirmRes(boolean confirm) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importECashTipConfirmRes" +
                " confirm: " + confirm);
        return false;
    }

    @Override
    public boolean importOnlineResp(boolean onlineRes, String respCode, String icc55) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importOnlineResp" +
                " onlineRes: " + onlineRes + ";respCode: " + respCode + ";icc55: " + icc55);
        SDKLog.d(TAG, "mCallbackSort=" + mCallbackSort);
        if (mCallbackSort == PayDataUtil.CallbackSort.REQUEST_ONLINE) {
            if (cardPayType == PayDataUtil.KERNTYPE_VISA) {
                return new PayWaveProcess().scriptProcess(onlineRes, respCode, icc55, mResultListener);
            } else if (cardPayType == PayDataUtil.KERNTYPE_MC) {
                return new PayPassProcess().scriptProcess(onlineRes, respCode, icc55, mResultListener);
            } else if (cardPayType == PayDataUtil.KERNTYPE_QPBOC) {
                return new QpbocProcess().scriptProcess(onlineRes, respCode, icc55, mResultListener);
            }
        }
        return false;
    }

    @Override
    public boolean importConfirmCardInfoRes(boolean res) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importConfirmCardInfoRes" +
                " res: " + res);
        if (mDownLatch != null && res && mCallbackSort == PayDataUtil.CallbackSort.REQUEST_CARDINFO_CONFIRM) {
            mDownLatch.countDown();
            return true;
        }
        isEndEmv = true;
        return false;
    }


    @Override
    public void endEmv() {
        SDKLog.d(TAG, "endEmv");
        isEndEmv = true;
        if (mPayThread != null && mPayThread.isAlive()) {
            mPayThread.interrupt();
            mPayThread = null;
        }
        mDownLatch.countDown();
    }

    /**
     * 是否已经请求导入金额
     *
     * @return isRequestAmt
     */
    public String getImportAmt() {
        SDKLog.d(TAG, "getImportAmt: " + mImportAmt);
        return mImportAmt;
    }

    private int buildCandidate() throws RemoteException {
        //init
        initData();
        if (mEmvTransData.getTransType() != PayDataUtil.CardCode.LKL_BALANCE) {
            selectCallback(PayDataUtil.CallbackSort.REQUEST_IMPORT_AMT, null, 0);
        }
        //pre processing
        mTransParam = new TransParam();
        if (mAmount != null) {
            mTransParam.setAucAmount(BytesUtil.hexString2Bytes(mAmount));
        } else {
            mTransParam.setAucAmount(BytesUtil.hexString2Bytes("000000000001"));
        }
        mTransParam.setAucAmountOther(null);
        PayDataUtil dataUtil = new PayDataUtil();
        mTransParam.setAucTransDate(BytesUtil.hexString2Bytes(dataUtil.getTransDateTime(0)));
        mTransParam.setAucTransTime(BytesUtil.hexString2Bytes(dataUtil.getTransDateTime(1)));
        mTransParam.setAucRFU(null);
        mTransParam.setAucUnNumber(PayDataUtil.getHexRandom(4));
        mTransParam.setUlTransNo(PayDataUtil.getSerialNumber());
        //TO DO 货币代码需要设置内核数据
        byte[] transCurCode = BytesUtil.hexString2Bytes("0566");
        if (mTlvList.getList().containsKey("5F2A"))
            transCurCode = mTlvList.getTlv("5F2A").getValue();
        SDKLog.d(TAG, "transCurCode: " + BytesUtil.bytes2HexString(transCurCode));
        mTransParam.setAucTransCurCode(transCurCode);
        mTransParam.setUcTransType(mEmvTransData.getTransType());
        int res = entryL2.preProcessing(mTransParam);
        if (res != PayDataUtil.EMV_OK) {
            SDKLog.d(TAG, "preProcessing fail, ret: " + res);
            if (res == PayDataUtil.CLSS_USE_CONTACT) {
                Bundle param = new Bundle();
                param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_USE_OTHER_INTERFACE);
                selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, param, -1);
            } else {
                int errorCode = entryL2.getErrorCode();
                SDKLog.d(TAG, "getErrorCode: " + errorCode);
                Bundle param = new Bundle();
                param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_STOP);
                selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, null, errorCode);
            }
            return PayDataUtil.DEFAULT_RETURN_CODE;
        }
        res = entryL2.buildCandidate(0, 0);
        if (res != PayDataUtil.EMV_OK) {
            SDKLog.d(TAG, "buildCandidate fail, error code: " + res);
            // need to handle 6A82 error
            if (res == PayDataUtil.ENTRY_KERNEL_6A82_ERR) {

            }
            int errorCode = entryL2.getErrorCode();
            SDKLog.d(TAG, "getErrorCode: " + errorCode);
            Bundle param = new Bundle();
            param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_STOP);
            selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, param, errorCode);
            return PayDataUtil.DEFAULT_RETURN_CODE;
        }

        return startFinalSelect();
    }

    private int startFinalSelect() throws RemoteException {
        SDKLog.d(TAG, "startFinalSelect");
        byte[] ucKernType = new byte[1];
        byte[] outData = new byte[300];
        byte[] data = null;
        int[] len = new int[1];
        boolean isSelectOk = false;
        int count = 0;
        while (count++ < aidCount) {
            Arrays.fill(outData, (byte) 0x00);
            int res = entryL2.finalSelect(ucKernType, outData, len);
            SDKLog.d(TAG, "finalSelect res: " + res);

            if (res != PayDataUtil.EMV_OK) {
                if (res == PayDataUtil.CLSS_USE_CONTACT || res == PayDataUtil.ICC_CMD_ERR) {
                    break;
                } else {
                    SDKLog.d(TAG, "finalSelect fail, error code: " + res);
                    int errorCode = entryL2.getErrorCode();
                    SDKLog.d(TAG, "getErrorCode: " + errorCode);
                    entryL2.delCandListCurApp();
                }
            } else {
                isSelectOk = true;
                if (len[0] > 0) {
                    data = new byte[len[0]];
                    System.arraycopy(outData, 0, data, 0, len[0]);
                }
                break;
            }
        }
        if (!isSelectOk) {
            SDKLog.d(TAG, "finalSelect fail!");
            Bundle param = new Bundle();
            param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_USE_OTHER_INTERFACE);
            selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, param, -1);
            return PayDataUtil.DEFAULT_RETURN_CODE;
        }

        PreProcResult preProcResult = new PreProcResult();
        int ret = entryL2.getPreProcResult(preProcResult);
        SDKLog.d(TAG, "getPreProcResult ret: " + ret);
        SDKLog.d(TAG, "preProcResult.getAucReaderTTQ(): " + BytesUtil.bytes2HexString(preProcResult.getAucReaderTTQ()));
        if (ret != PayDataUtil.EMV_OK) {
            Bundle param = new Bundle();
            param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_STOP);
            selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, param, ret);
            return PayDataUtil.DEFAULT_RETURN_CODE;
        }

        SDKLog.d(TAG, "ucKernType: " + ucKernType[0]);
        SDKLog.d(TAG, "outData: " + BytesUtil.bytes2HexString(data));

        cardPayType = ucKernType[0];
        Bundle bundle = new Bundle();
        bundle.putInt(PayDataUtil.CardCode.FINAL_SELECT_LEN, len[0]);
        bundle.putByteArray(PayDataUtil.CardCode.FINAL_SELECT_DATA, data);
        bundle.putParcelable(PayDataUtil.CardCode.PREPROC_RESULT, preProcResult);
        bundle.putParcelable(PayDataUtil.CardCode.TRANS_PARAM, mTransParam);

        mPayThread = new StartPayThread();
        mPayThread.setAucType(cardPayType, bundle);
        mPayThread.start();
        return cardPayType;

    }

    private class StartPayThread extends Thread {

        private byte aucType = -1;
        private Bundle bundle = null;

        @Override
        public void run() {
            BasePayProcess payProcess = null;
            switch (aucType) {
                case PayDataUtil.KERNTYPE_MC:
                    payProcess = new PayPassProcess();
                    payProcess.startPay(bundle, mResultListener);
                    break;
                case PayDataUtil.KERNTYPE_VISA:
                    payProcess = new PayWaveProcess();
                    payProcess.startPay(bundle, mResultListener);
                    break;
                case PayDataUtil.KERNTYPE_PURE:
                    payProcess = new PureProcess();
                    payProcess.startPay(bundle, mResultListener);
                    break;
                case PayDataUtil.KERNTYPE_AMEX:
                    payProcess = new AMexPayProcess();
                    payProcess.startPay(bundle, mResultListener);
                    break;
                case PayDataUtil.KERNTYPE_QPBOC:
                    payProcess = new QpbocProcess();
                    payProcess.startPay(bundle, mResultListener);
                    break;
                default:
                    mResultListener.onProcessResult(false, true, PayDataUtil.CardCode.TRANS_STOP);
                    break;
            }
        }

        void setAucType(byte aucType, Bundle param) {
            SDKLog.d(TAG, "setAucType aucType: " + aucType);
            this.aucType = aucType;
            this.bundle = param;
        }

        void finalSelectAgain() throws RemoteException {
            SDKLog.d(TAG, "startFinalSelect again");
            byte[] ucKernType = new byte[1];
            byte[] outData = new byte[300];
            byte[] data = null;
            int[] len = new int[1];
            boolean isSelectOk = false;
            int count = 0;
            while (count++ < aidCount) {
                Arrays.fill(outData, (byte) 0x00);
                int res = entryL2.finalSelect(ucKernType, outData, len);
                SDKLog.d(TAG, "finalSelect res: " + res);

                if (res != PayDataUtil.EMV_OK) {
                    if (res == PayDataUtil.CLSS_USE_CONTACT || res == PayDataUtil.ICC_CMD_ERR) {
                        break;
                    } else {
                        SDKLog.d(TAG, "finalSelect fail, error code: " + res);
                        int errorCode = entryL2.getErrorCode();
                        SDKLog.d(TAG, "getErrorCode: " + errorCode);
                        entryL2.delCandListCurApp();
                    }
                } else {
                    isSelectOk = true;
                    if (len[0] > 0) {
                        data = new byte[len[0]];
                        System.arraycopy(outData, 0, data, 0, len[0]);
                    }
                    break;
                }
            }
            if (!isSelectOk) {
                SDKLog.d(TAG, "finalSelect fail!");
                Bundle param = new Bundle();
                param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_USE_OTHER_INTERFACE);
                selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, param, -1);
            } else {
                PreProcResult preProcResult = new PreProcResult();
                int ret = entryL2.getPreProcResult(preProcResult);
                SDKLog.d(TAG, "getPreProcResult ret: " + ret);
                if (ret != PayDataUtil.EMV_OK) {
                    Bundle param = new Bundle();
                    param.putInt(PayDataUtil.CardCode.TRANS_RESULT, PayDataUtil.CardCode.TRANS_STOP);
                    selectCallback(PayDataUtil.CallbackSort.ON_TRANS_RESULT, param, ret);
                    return;
                }
                SDKLog.d(TAG, "ucKernType: " + ucKernType[0]);
                SDKLog.d(TAG, "outData: " + BytesUtil.bytes2HexString(data));
                Bundle bundle = new Bundle();
                bundle.putInt(PayDataUtil.CardCode.FINAL_SELECT_LEN, len[0]);
                bundle.putByteArray(PayDataUtil.CardCode.FINAL_SELECT_DATA, data);
                bundle.putParcelable(PayDataUtil.CardCode.PREPROC_RESULT, preProcResult);
                bundle.putParcelable(PayDataUtil.CardCode.TRANS_PARAM, mTransParam);
                BasePayProcess payProcess = null;
                switch (ucKernType[0]) {
                    case PayDataUtil.KERNTYPE_MC:
                        payProcess = new PayPassProcess();
                        payProcess.startPay(bundle, mResultListener);
                        break;
                    case PayDataUtil.KERNTYPE_VISA:
                        payProcess = new PayWaveProcess();
                        payProcess.startPay(bundle, mResultListener);
                        break;
                    case PayDataUtil.KERNTYPE_PURE:
                        payProcess = new PureProcess();
                        payProcess.startPay(bundle, mResultListener);
                        break;
                    case PayDataUtil.KERNTYPE_AMEX:
                        payProcess = new AMexPayProcess();
                        payProcess.startPay(bundle, mResultListener);
                        break;
                    case PayDataUtil.KERNTYPE_QPBOC:
                        payProcess = new QpbocProcess();
                        payProcess.startPay(bundle, mResultListener);
                        break;
                    default:
                        mResultListener.onProcessResult(false, true, PayDataUtil.CardCode.TRANS_STOP);
                        break;
                }
            }
        }
    }

    private TransResultListener mResultListener = new TransResultListener() {
        @Override
        public void onFail(int errorCode) {
            SDKLog.d(TAG, "fail error code: " + errorCode);
            mCallbackSort = PayDataUtil.CallbackSort.ON_ERROR;
            try {
                mEmvProcessListener.onError(errorCode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProcessResult(boolean isOnline, boolean isTransResult, int resultValue) {
            SDKLog.d(TAG, "onProcessResult isTransResult: " + isTransResult +
                    ";resultValue: " + resultValue);
            try {
                if (isTransResult) {
                    mCallbackSort = PayDataUtil.CallbackSort.ON_TRANS_RESULT;
                    mEmvProcessListener.onTransResult(resultValue);
                } else {
                    if (isOnline) {
                        mCallbackSort = PayDataUtil.CallbackSort.REQUEST_ONLINE;
                        mEmvProcessListener.onRequestOnline();
                    } else {
                        //TO DO
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void nextTransStep(PayDataUtil.CallbackSort sort, Bundle data) {
            SDKLog.d(TAG, "nextTransStep CallbackSort: " + sort);
            selectCallback(sort, data, -1);
        }

        @Override
        public void finalSelectAgain() {
            SDKLog.d(TAG, "finalSelectAgain");
            if (mPayThread != null && mPayThread.isAlive()) {
                try {
                    mPayThread.finalSelectAgain();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void setKernalData(int aucType, byte[] aid) {
            SDKLog.d(TAG, "setKernalData aucType: " + aucType);
            setTransKernelData(aucType, aid);
        }

        @Override
        public void setCardNo(byte[] cardNo) {
            SDKLog.d(TAG, "setCardNo cardNo: " + BytesUtil.bytes2HexString(cardNo));
            mCardNo = cardNo;
        }
    };

    /**
     * 获取tlv数据
     *
     * @param tag tlv的tag
     * @return tlv数据的value值
     */
    @Override
    public byte[] getTlv(String tag) {
        SDKLog.d(TAG, "getTlvData() tag: " + tag);
        if ("5A".equals(tag)) {
            Tlv tlv = new Tlv(tag, mCardNo);
            return tlv.getBytes();
        }

        try {
            int res = -1;
            byte[] data = new byte[512];
            int[] dataLen = new int[1];
            byte[] tagByte = BytesUtil.hexString2Bytes(tag);

            switch (cardPayType) {
                case PayDataUtil.KERNTYPE_MC:
                    res = paypass.getTLVDataList(tagByte, tagByte.length, data.length, data, dataLen);
                    break;
                case PayDataUtil.KERNTYPE_VISA:
                    res = paywave.getTLVDataList(tagByte, tagByte.length, data.length, data, dataLen);
                    break;
                case PayDataUtil.KERNTYPE_PURE:
                    res = pure.getTLVDataList(tagByte, tagByte.length, data.length, data, dataLen);
                    break;
                case PayDataUtil.KERNTYPE_AMEX:
                    res = amex.getTLVDataList(tagByte, tagByte.length, data.length, data, dataLen);
                    break;
                case PayDataUtil.KERNTYPE_QPBOC: {
                    byte[] bTag4Bytes = new byte[4];
                    int iTag = 0;

                    Arrays.fill(bTag4Bytes, (byte) 0);
                    System.arraycopy(tagByte, 0, bTag4Bytes, bTag4Bytes.length - tagByte.length, tagByte.length);
                    Log.d(TAG, "bTag4Bytes: " + BytesUtil.bytes2HexString(bTag4Bytes));

                    //The first parameter of 'BytesUtil.bytes2Int' must be 4 bytes
                    iTag = BytesUtil.bytes2Int(bTag4Bytes, true);
                    Log.d(TAG, "iTag: " + iTag);

                    data = qpboc.getTLVData(iTag);
                    if (data != null) {
                        res = PayDataUtil.EMV_OK;
                        dataLen[0] = data.length;
                    }
                    Log.d(TAG, "data: " + BytesUtil.bytes2HexString(data));
                }
                break;
                default:
                    break;
            }
            SDKLog.d(TAG, "getTlvData res: " + res);
            SDKLog.d(TAG, "getTlvData dataLen: " + dataLen[0]);
            SDKLog.d(TAG, "getTlvData data: " + BytesUtil.bytes2HexString(data));
            if (res == PayDataUtil.EMV_OK && dataLen[0] > 0) {
                byte[] outData = new byte[dataLen[0]];
                System.arraycopy(data, 0, outData, 0, dataLen[0]);
                Tlv tlv = new Tlv(tag, outData);
                return tlv.getBytes();
            } else {
                return null;
            }
        } catch (RemoteException e) {
            SDKLog.d(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置tlv数据
     *
     * @param tag   tlv的tag
     * @param value tlv的value值
     */
    @Override
    public void setTlv(String tag, byte[] value) {
        if (tag != null && value != null) {
            mTlvList.addTlv(tag, value);
        }
    }

    private void setTransKernelData(int ucType, byte[] aid) {
        try {
            TlvList mList = handleKernalData(aid);
            byte[] data = mList.getBytes();
            if (ucType == PayDataUtil.KERNTYPE_MC) {
                paypass.setTLVDataList(data, data.length);
            } else if (ucType == PayDataUtil.KERNTYPE_VISA) {
                paywave.setTLVDataList(data, data.length);
            } else if (ucType == PayDataUtil.KERNTYPE_PURE) {
                pure.setTLVDataList(data, data.length);
            } else if (ucType == PayDataUtil.KERNTYPE_AMEX) {
                amex.setTLVDataList(data, data.length);
            } else if (ucType == PayDataUtil.KERNTYPE_QPBOC) {
                QpbocProcess.setTLVDataList(data, data.length);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化交易库数据
     */
    private void initData() throws RemoteException {
        entryL2.initialize();
        entryL2.delAllCombination();

        mAmount = null;

        List<Aid> mList = db.getAidDao().findAllAid();
        if (mList == null || mList.size() == 0) {
            SDKLog.d(TAG, "aid is null!");
            selectCallback(PayDataUtil.CallbackSort.ON_ERROR, null, PayDataUtil.CardCode.NO_AID_ERROR);
        } else {
            aidCount = mList.size();
            for (Aid aid : mList) {
                Combination combination = new Combination();
                combination.setUcAidLen(aid.getAid().length() / 2);
                combination.setAucAID(BytesUtil.hexString2Bytes(aid.getAid()));
                combination.setUcPartMatch(1);
                combination.setUcKernIDLen(1);
                byte kernelId = aid.getKernelType();
                combination.setAucKernelID(new byte[]{kernelId});

                //Byte 1
                //bit 6: 1 = EMV mode supported
                //bit 5: 1 = EMV contact chip supported
                //bit 3: 1 = Online PIN supported
                //bit 2: 1 = Signature supported
                //Byte 3
                //bit 8: 1 = Issuer Update Processing supported
                //bit 7: 1 = Consumer Device CVM supported
                byte[] TTQ = new byte[]{0x36, 0x00, (byte) 0xC0, 0x00};
                ;
                combination.setAucReaderTTQ(TTQ);

                if (aid.isFloorLimitFlg()) {
                    combination.setUcTermFLmtFlg(PayDataUtil.CLSS_TAG_EXIST_WITHVAL);
                    combination.setUlTermFLmt(aid.getFloorLimit());
                } else {
                    combination.setUcTermFLmtFlg(PayDataUtil.CLSS_TAG_NOT_EXIST);
                }

                //if (aid.isRdCVMLimitFlg()) {
                if (true) {
                    combination.setUcRdCVMLmtFlg(PayDataUtil.CLSS_TAG_EXIST_WITHVAL);
                    combination.setAucRdCVMLmt(BytesUtil.hexString2Bytes(String.format("%012d", aid.getRdCVMLimit())));
                    Log.d(TAG, "aid.getRdCVMLimit(): " + String.format("%012d", aid.getRdCVMLimit()));
                } else {
                    combination.setUcRdCVMLmtFlg(PayDataUtil.CLSS_TAG_NOT_EXIST);
                }

                //if (aid.isRdClssTxnLimitFlg()) {
                if (true) {
                    combination.setUcRdClssTxnLmtFlg(PayDataUtil.CLSS_TAG_EXIST_WITHVAL);
                    combination.setAucRdClssTxnLmt(BytesUtil.hexString2Bytes(String.format("%012d", aid.getRdClssTxnLimit())));
                    Log.d(TAG, "aid.getRdClssTxnLimit(): " + String.format("%012d", aid.getRdClssTxnLimit()));
                } else {
                    combination.setUcRdClssTxnLmtFlg(PayDataUtil.CLSS_TAG_NOT_EXIST);
                }

                //if (aid.isRdClssFloorLimitFlg()) {
                if (true) {
                    combination.setUcRdClssFLmtFlg(PayDataUtil.CLSS_TAG_EXIST_WITHVAL);
                    combination.setAucRdClssFLmt(BytesUtil.hexString2Bytes(String.format("%012d", aid.getRdClssFloorLimit())));
                    Log.d(TAG, "aid.getRdClssFloorLimit(): " + String.format("%012d", aid.getRdClssFloorLimit()));
                } else {
                    combination.setUcRdClssFLmtFlg(PayDataUtil.CLSS_TAG_NOT_EXIST);
                }
                combination.setUcZeroAmtNoAllowed(0);
                combination.setUcStatusCheckFlg(0);
                combination.setUcCrypto17Flg(1);
                combination.setUcExSelectSuppFlg(0);


                Log.d(TAG, "combination.getAucRdClssTxnLmt(): " + BytesUtil.bytes2HexString(combination.getAucRdClssTxnLmt()));
                Log.d(TAG, "combination.getAucRdClssFLmt(): " + BytesUtil.bytes2HexString(combination.getAucRdClssFLmt()));
                Log.d(TAG, "combination.getAucRdCVMLmt(): " + BytesUtil.bytes2HexString(combination.getAucRdCVMLmt()));
                entryL2.addCombination(combination);
            }
        }
    }

    /**
     * 选择需要回调的方法
     *
     * @param index  方法序号
     * @param bundle 传入数据
     */
    private void selectCallback(PayDataUtil.CallbackSort index, Bundle bundle, int errorCode) {
        SDKLog.d(TAG, "selectCallback: " + index);
        try {
            if (isEndEmv) {
                mCallbackSort = PayDataUtil.CallbackSort.ON_TRANS_RESULT;
                mEmvProcessListener.onTransResult(PayDataUtil.CardCode.TRANS_STOP);
                return;
            }
            mDownLatch = new CountDownLatch(LATCH_COUNT);
            switch (index) {
                case REQUEST_IMPORT_AMT:
                    //默认金额类型为授权金额(1)
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_IMPORT_AMT;
                    mEmvProcessListener.requestImportAmount(1);
                    mDownLatch.await();
                    break;
                case REQUEST_FINAL_AID_SELECT:
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_FINAL_AID_SELECT;
                    mEmvProcessListener.finalAidSelect();
                    mDownLatch.await();
                    break;
                case REQUEST_AID_SELECT:
                    int times = bundle.getInt(PayDataUtil.CardCode.IMPORT_AMT_TIMES, 1);
                    String[] aids = bundle.getStringArray(PayDataUtil.CardCode.IMPORT_AMT_AIDS);
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_AID_SELECT;
                    mEmvProcessListener.requestAidSelect(times, aids);
                    mDownLatch.await();
                    break;
                case REQUEST_TIPS_CONFIRM:
                    //暂时没用
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_TIPS_CONFIRM;
                    mEmvProcessListener.requestTipsConfirm(null);
                    mDownLatch.await();
                    break;
                case REQUEST_ECASHTIPS_CONFIRM:
                    //暂时没用
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_ECASHTIPS_CONFIRM;
                    mEmvProcessListener.requestEcashTipsConfirm();
                    mDownLatch.await();
                    break;
                case REQUEST_CARDINFO_CONFIRM:
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_CARDINFO_CONFIRM;
                    String cardNo = bundle.getString(PayDataUtil.CardCode.CARDINFO_CARDNO);
                    mEmvProcessListener.onConfirmCardInfo(cardNo);
                    mDownLatch.await();
                    break;
                case REQUEST_IMPORT_PIN:
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_IMPORT_PIN;
                    int pinType = bundle.getInt(PayDataUtil.CardCode.IMPORT_PIN_TYPE);
                    try {

                        mEmvProcessListener.requestImportPin(pinType, true, mImportAmt, -1);
                    } catch (Exception e) {

                    }
                    mDownLatch.await();
                    break;
                case REQUEST_USER_AUTH:
                    //暂时没用
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_USER_AUTH;
                    mEmvProcessListener.requestUserAuth(0, null);
                    mDownLatch.await();
                    break;
                case REQUEST_ONLINE:
                    mCallbackSort = PayDataUtil.CallbackSort.REQUEST_ONLINE;
                    mEmvProcessListener.onRequestOnline();
                    break;
                case ON_OFFLINE_BALANCE:
                    mCallbackSort = PayDataUtil.CallbackSort.ON_OFFLINE_BALANCE;
                    mEmvProcessListener.onReadCardOffLineBalance(null,
                            null, null, null);
                    break;
                case ON_CARD_TRANSLOG:
                    mCallbackSort = PayDataUtil.CallbackSort.ON_CARD_TRANSLOG;
                    mEmvProcessListener.onReadCardTransLog(null);
                    break;
                case ON_CARD_LOADLOG:
                    mCallbackSort = PayDataUtil.CallbackSort.ON_CARD_LOADLOG;
                    mEmvProcessListener.onReadCardLoadLog(null, null, null);
                    break;
                case ON_TRANS_RESULT:
                    mCallbackSort = PayDataUtil.CallbackSort.ON_TRANS_RESULT;
                    int transRes = bundle.getInt(PayDataUtil.CardCode.TRANS_RESULT);
                    mEmvProcessListener.onTransResult(transRes);
                    break;
                case ON_ERROR:
                    mCallbackSort = PayDataUtil.CallbackSort.ON_ERROR;
                    mEmvProcessListener.onError(errorCode);
                    break;
                default:
                    break;
            }
        } catch (RemoteException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * assets 文件保存
     */
    private TlvList handleKernalData(byte[] aid) {
        PayDataUtil dataUtil = new PayDataUtil();
        TlvList list = dataUtil.getDefaultKernal();

        //set kernel data
        if (mTlvList.getList() != null && mTlvList.getList().size() > 0) {
            for (Map.Entry<String, Tlv> entry : mTlvList.getList().entrySet()) {
                SDKLog.d(TAG, "settlv out: " + entry.getValue().toHex());
                list.addTlv(entry.getValue());
            }
            mTlvList.clear();
        }

        //Amount, Authorised (Numeric)
        SDKLog.d(TAG, "txn amount: " + mAmount);
        if (mAmount != null) {
            list.addTlv("9F02", mAmount);
        }

        //Transaction Type
        String tradeType = BytesUtil.bytes2HexString(new byte[]{mEmvTransData.getTransType()});
        SDKLog.d(TAG, "txn type: " + tradeType);
        list.addTlv("9C", tradeType);

        //Transaction Sequence Counter
        list.addTlv("9F41", dataUtil.getSequenceCounter());

        //Transaction Date
        list.addTlv("9A", dataUtil.getTransDateTime(PayDataUtil.TRANS_DATE_YYMMDD));

        //Transaction Time
        list.addTlv("9F21", dataUtil.getTransDateTime(PayDataUtil.TRANS_TIME_HHMMSS));

        //The getRandom function returns a fixed 8 byte random number
        byte[] random = new byte[0];
        try {
            random = pinPad.getRandom();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        byte[] unpredictableNum = new byte[4];
        System.arraycopy(random, 0, unpredictableNum, 0, 4);
        list.addTlv("9F37", unpredictableNum);

        //aid parameters
        if (aid == null) {
            return list;
        }
        TlvList aidList = new TlvList();
        aidList.fromBytes(aid);

        //Reader Contactless Floor Limit
        String data = null;
        if (aidList.getTlv("DF19") != null)
            data = aidList.getTlv("DF19").getHexValue();
        SDKLog.d(TAG, "DF8123(DF19) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8123", data);
        } else {
            list.addTlv("DF8123", "000000030000");
        }

        //Reader Contactless Transaction Limit (No On-device CVM)
        data = null;
        if (aidList.getTlv("DF20") != null)
            data = aidList.getTlv("DF20").getHexValue();
        SDKLog.d(TAG, "DF8124(DF20) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8124", data);
        } else {
            list.addTlv("DF8124", "000099999999");
        }

        //Reader Contactless Transaction Limit (On-device CVM)
        data = null;
        if (aidList.getTlv("DF20") != null)
            data = aidList.getTlv("DF20").getHexValue();
        SDKLog.d(TAG, "DF8125(DF20) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8125", data);
        } else {
            list.addTlv("DF8125", "000099999999");
        }

        //Reader CVM Required Limit
        data = null;
        if (aidList.getTlv("DF21") != null)
            data = aidList.getTlv("DF21").getHexValue();
        SDKLog.d(TAG, "DF8126(DF21) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8126", data);
        } else {
            list.addTlv("DF8126", "000000030000");
        }

        //TAC-Default
        data = null;
        if (aidList.getTlv("DF11") != null)
            data = aidList.getTlv("DF11").getHexValue();
        SDKLog.d(TAG, "DF8120(DF11) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8120", data);
        } else {
            list.addTlv("DF8120", "0000000000");
        }

        //TAC-Online
        data = null;
        if (aidList.getTlv("DF12") != null)
            data = aidList.getTlv("DF12").getHexValue();
        SDKLog.d(TAG, "DF8122(DF12) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8122", data);
        } else {
            list.addTlv("DF8122", "0000000000");
        }

        //TAC-Denial
        data = null;
        if (aidList.getTlv("DF13") != null)
            data = aidList.getTlv("DF13").getHexValue();
        SDKLog.d(TAG, "DF8121(DF13) data: " + data);
        if (!TextUtils.isEmpty(data)) {
            list.addTlv("DF8121", data);
        } else {
            list.addTlv("DF8121", "0000000000");
        }

        return list;
    }
}

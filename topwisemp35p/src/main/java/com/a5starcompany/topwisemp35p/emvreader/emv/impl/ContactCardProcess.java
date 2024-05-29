package com.a5starcompany.topwisemp35p.emvreader.emv.impl;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.a5starcompany.topwisemp35p.emvreader.database.table.Aid;
import com.a5starcompany.topwisemp35p.emvreader.database.table.Capk;
import com.a5starcompany.topwisemp35p.emvreader.database.table.DBManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvDefinition;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvDeviceManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvTransData;
import com.a5starcompany.topwisemp35p.emvreader.emv.OnEmvProcessListener;
import com.a5starcompany.topwisemp35p.emvreader.util.AmountUtil;
import com.a5starcompany.topwisemp35p.emvreader.util.CommonFunction;
import com.topwise.cloudpos.aidl.emv.level2.AidlEmvL2;
import com.topwise.cloudpos.aidl.emv.level2.EmvCallback;
import com.topwise.cloudpos.aidl.emv.level2.EmvCandidateItem;
import com.topwise.cloudpos.aidl.emv.level2.EmvCapk;
import com.topwise.cloudpos.aidl.emv.level2.EmvKernelConfig;
import com.topwise.cloudpos.aidl.emv.level2.EmvTerminalInfo;
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad;
import com.topwise.cloudpos.struct.BytesUtil;
import com.topwise.cloudpos.struct.Tlv;
import com.topwise.cloudpos.struct.TlvList;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/*
 *
 * Contact card EMV transaction
 * */
public class ContactCardProcess implements EmvProcessInterface {
    private static final String TAG = "ContactCardProcess";
    private volatile static ContactCardProcess instance = null;
    private AidlPinpad pinPad = EmvDeviceManager.getInstance().getPinpadManager();
    private AidlEmvL2 emvL2 = EmvDeviceManager.getInstance().getEmvL2();
    private DBManager db = DBManager.getInstance();
    private EmvTransData emvTransData;
    private OnEmvProcessListener emvListener;
    private EmvKernelConfig emvKernelConfig;
    private EmvTerminalInfo emvTerminalInfo;
    private CountDownLatch countDownLatch;
    private boolean isEndEmv;


    //importAidSelectRes()
    private int importAidSelectIndex;

    //importFinalAidSelectRes()
    private boolean importFinalAidSelectRes;

    //public boolean importMsgConfirmRes(boolean res)
    private boolean importMsgConfirmResult;

    //public boolean importAmount(String amt)
    private String importFixedAmount = null;

    //public boolean importConfirmCardInfoRes(boolean res)
    private boolean importConfirmCardInfoResult;

    //public boolean importPin(String pin)
    private String importPinStr;

    //public boolean importOnlineResp(boolean onlineRes, String respCode, String icc55)
    private boolean importOnlineRes;
    private String importRespCode;
    private String importIcc55;

    private int pinRetryTimes = 3;

    private ContactCardProcess() {
    }

    public static ContactCardProcess getInstance() {
        if (instance == null) {
            synchronized (ContactCardProcess.class) {
                if (instance == null) {
                    instance = new ContactCardProcess();
                }
            }
        }
        return instance;
    }

    private void countDownLatchNew() {
        countDownLatch = new CountDownLatch(1);
    }

    private void countDownLatchAwait() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void countDownLatchdDown() {
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    private void initTransData() {
        isEndEmv = false;
        importAidSelectIndex = -1;
        importFinalAidSelectRes = false;
        importMsgConfirmResult = false;
        importFixedAmount = null;
        importConfirmCardInfoResult = false;
        importPinStr = null;
        importOnlineRes = false;
        importRespCode = null;
        importIcc55 = null;
    }

    private EmvCallback m_emvCallback = new EmvCallback.Stub() {
        private static final String TAG = "EmvCallback";

        /***
         *  Require online PIN processing
         * @param b : [IN] Is allow PIN entry bypass ?
         * @param bytes : [IN] PAN, The value of 5A
         * @param i : [IN ] PAN length,  The length of 5A
         * @param booleans : [OUT]  PIN entry bypassed ?
         * @return 1: Bypassed or Successfully entered
         *         0: No entry PIN or PIN pad is malfunctioning
         * @throws RemoteException
         */
        @Override
        public int cGetOnlinePin(boolean b, byte[] bytes, int i, boolean[] booleans) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cGetOnlinePin");
            Log.d(TAG, "Is allow PIN entry bypass: " + b);
            Log.d(TAG, "PAN: " + BytesUtil.bytes2HexString(bytes));
            Log.d(TAG, "PAN length: " + i);

            countDownLatchNew();
            emvListener.requestImportPin(PayDataUtil.PINTYPE_ONLINE, false, importFixedAmount, 3);
            countDownLatchAwait();
            if ((importPinStr == null) || (importPinStr.equals(""))) {
                return 0;
            }

            Log.d(TAG, "cGetOnlinePin, importPinStr: " + importPinStr);
            if (importPinStr.equals("bypass")) {
                booleans[0] = true;
            } else {
                booleans[0] = false;
            }
            return 1;
        }

        /***
         * Require offline PIN processing
         * @param b :  [IN] Is allow PIN entry bypass ?
         * @param bytes : [OUT] The plaintext offline PIN block
         * @param i : [IN] PIN block buffer size
         * @param booleans : [OUT]  PIN entry bypassed ?
         * @return 1: Bypassed or Successfully entered
         *         0: No entry PIN or PIN pad is malfunctioning
         * @throws RemoteException
         */
        @Override
        public int cGetPlainTextPin(boolean b, byte[] bytes, int i, boolean[] booleans) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cGetPlainTextPin");
            byte[] pinTryCnt = new byte[0];

            TlvList tlvList = new TlvList();
            tlvList.fromHex(BytesUtil.bytes2HexString(getTlv("9F17")));
            Tlv tlv = tlvList.getTlv("9F17");
            if (tlv != null)
                pinTryCnt = tlv.getValue();
            if (pinTryCnt != null)
                Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "pinTryCnt:" + BytesUtil.bytes2HexString(pinTryCnt));
            boolean lastTimeFlag = false;
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "pinRetryTimes:" + pinRetryTimes);
            if (0 == pinRetryTimes || 1 == pinRetryTimes) {
                lastTimeFlag = true;
            } else if (pinTryCnt == null || pinTryCnt.length != 1) {
                lastTimeFlag = true;
            } else if (pinTryCnt != null && (1 == pinTryCnt.length) && (0x00 == pinTryCnt[0] || 0x01 == pinTryCnt[0])) {
                Log.d(TAG, "pinTryCnt = " + BytesUtil.bytes2HexString(pinTryCnt));
                lastTimeFlag = true;
            } else {
                lastTimeFlag = false;
            }
            countDownLatchNew();
            emvListener.requestImportPin(PayDataUtil.PINTYPE_OFFLINE, lastTimeFlag, null, pinRetryTimes);
            countDownLatchAwait();
            if ((importPinStr == null) || (importPinStr.equals(""))) {
                return 0;
            }

            Log.d(TAG, "cGetPlainTextPin, importPinStr: " + importPinStr);
            if (importPinStr.equals("bypass")) {
                booleans[0] = true;
            } else {
                booleans[0] = false;
                byte[] pinBlock = getOfflinePinBlock(importPinStr);
                System.arraycopy(pinBlock, 0, bytes, 0, pinBlock.length);
                Log.d(TAG, "getOfflinePinBlock(): " + BytesUtil.bytes2HexString(pinBlock));
            }

            return 1;
        }

        /***
         *
         * @param i : [IN] The number of remaining PIN tries
         * @return
         * @throws RemoteException
         */
        @Override
        public int cDisplayPinVerifyStatus(int i) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cDisplayPinVerifyStatus");
            Log.d(TAG, "The number of remaining PIN tries: " + i);
            pinRetryTimes = i;
            return 1;
        }

        /***
         * Check CardHolder Credentials (Only used by UnionPay PBOC)
         * @param i
         * @param bytes
         * @param i1
         * @param booleans
         * @return
         * @throws RemoteException
         */
        @Override
        public int cCheckCredentials(int i, byte[] bytes, int i1, boolean[] booleans) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cCheckCredentials");
            return 0;
        }

        /***
         *
         * @param bytes
         * @param i
         * @return
         * @throws RemoteException
         */
        @Override
        public int cIssuerReferral(byte[] bytes, int i) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cIssuerReferral");
            return 0;
        }

        @Override
        public int cGetTransLogAmount(byte[] bytes, int i, int i1) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cGetTransLogAmount");
            return 0;
        }

        @Override
        public int cCheckExceptionFile(byte[] bytes, int i, int i1) throws RemoteException {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "cCheckExceptionFile");
            return 0;
        }

        @Override
        public int cRFU1() throws RemoteException {
            return 0;
        }

        @Override
        public int cRFU2() throws RemoteException {
            return 0;
        }

        @Override
        public int cRFU3() throws RemoteException {
            return 0;
        }

        @Override
        public int cRFU4() throws RemoteException {
            return 0;
        }
    };


    /**
     * EMV library initialization processing, each transaction must be called.
     *
     * @return
     * @throws RemoteException
     */
    private int EMVInit() throws RemoteException {
        int emvRet = 0;

        emvKernelConfig = new EmvKernelConfig();
        emvTerminalInfo = new EmvTerminalInfo();

        emvRet = emvL2.EMV_Initialize();
        if (emvRet != EmvDefinition.EMV_OK) {
            return emvRet;
        }

        emvRet = emvL2.EMV_SetKernelType((byte) EmvDefinition.KERNTYPE_EMV);
        if (emvRet != EmvDefinition.EMV_OK) {
            return emvRet;
        }

        emvRet = emvL2.EMV_SetCallback(m_emvCallback);
        if (emvRet != EmvDefinition.EMV_OK) {
            return emvRet;
        }

        emvKernelConfig.setbPSE((byte) 1);
        emvKernelConfig.setbCardHolderConfirm((byte) 1);
        emvKernelConfig.setbPreferredDisplayOrder((byte) 0);
        emvKernelConfig.setbLanguateSelect((byte) 1);
        emvKernelConfig.setbRevocationOfIssuerPublicKey((byte) 1);
        emvKernelConfig.setbDefaultDDOL((byte) 1);
        emvKernelConfig.setbBypassPINEntry((byte) 1);
        emvKernelConfig.setbSubBypassPINEntry((byte) 1);
        emvKernelConfig.setbGetdataForPINTryCounter((byte) 1);
        emvKernelConfig.setbFloorLimitCheck((byte) 1);
        emvKernelConfig.setbRandomTransSelection((byte) 1);
        emvKernelConfig.setbVelocityCheck((byte) 1);
        emvKernelConfig.setbTransactionLog((byte) 1);
        emvKernelConfig.setbExceptionFile((byte) 1);
        emvKernelConfig.setbTerminalActionCode((byte) 1);
        emvKernelConfig.setbDefaultActionCodeMethod((byte) EmvDefinition.EMV_DEFAULT_ACTION_CODE_AFTER_GAC1);
        emvKernelConfig.setbTACIACDefaultSkipedWhenUnableToGoOnline((byte) 0);
        emvKernelConfig.setbCDAFailureDetectedPriorTerminalActionAnalysis((byte) 1);
        emvKernelConfig.setbCDAMethod((byte) EmvDefinition.EMV_CDA_MODE1);
        //emvKernelConfig.setbForcedOnline((byte) 0);
        emvKernelConfig.setbForcedOnline((byte) 1);
        emvKernelConfig.setbForcedAcceptance((byte) 0);
        emvKernelConfig.setbAdvices((byte) 0);
        emvKernelConfig.setbIssuerReferral((byte) 1);
        emvKernelConfig.setbBatchDataCapture((byte) 0);
        emvKernelConfig.setbOnlineDataCapture((byte) 1);
        emvKernelConfig.setbDefaultTDOL((byte) 1);
        emvKernelConfig.setbTerminalSupportAccountTypeSelection((byte) 1);
        emvRet = emvL2.EMV_SetKernelConfig(emvKernelConfig);
        if (emvRet != EmvDefinition.EMV_OK) {
            return emvRet;
        }


        //emvTerminalInfo.setUnTerminalFloorLimit(20000);
        emvTerminalInfo.setUnTerminalFloorLimit(0);
        emvTerminalInfo.setUnThresholdValue(10000);
        emvTerminalInfo.setAucTerminalID("00000001");
        emvTerminalInfo.setAucIFDSerialNumber("12345678");
        emvTerminalInfo.setAucTerminalCountryCode(new byte[]{0x05, 0x66});
        emvTerminalInfo.setAucMerchantID("0000000100000001");
        emvTerminalInfo.setAucMerchantCategoryCode(new byte[]{0x54, 0x11});
        emvTerminalInfo.setAucMerchantNameLocation(new byte[]{0x30, 0x30, 0x30, 0x31}); //"0001"
        emvTerminalInfo.setAucTransCurrencyCode(new byte[]{0x05, 0x66});
        emvTerminalInfo.setUcTransCurrencyExp((byte) 2);
        emvTerminalInfo.setAucTransRefCurrencyCode(new byte[]{0x05, 0x66});
        emvTerminalInfo.setUcTransRefCurrencyExp((byte) 2);
        emvTerminalInfo.setUcTerminalEntryMode((byte) 0x05);

        emvTerminalInfo.setAucTerminalAcquireID("123456");
        emvTerminalInfo.setAucAppVersion(new byte[]{0x00, 0x030});
        emvTerminalInfo.setAucDefaultDDOL(new byte[]{(byte) 0x9F, 0x37, 0x04});
        emvTerminalInfo.setAucDefaultTDOL(new byte[]{(byte) 0x9F, 0x37, 0x04});
        emvTerminalInfo.setAucTACDenial(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        emvTerminalInfo.setAucTACOnline(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        emvTerminalInfo.setAucTACDefault(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});

        emvTerminalInfo.setUcTerminalType((byte) 0x22);
        //emvTerminalInfo.setAucTerminalCapabilities(new byte[] {(byte)0xE0, (byte)0xF8, (byte)0xC8});
        emvTerminalInfo.setAucTerminalCapabilities(new byte[]{(byte) 0xE0, (byte) 0xF8, (byte) 0xC8});
        emvTerminalInfo.setAucAddtionalTerminalCapabilities(new byte[]{(byte) 0xFF, (byte) 0x00, (byte) 0xF0, (byte) 0xA0, 0x01});

        emvTerminalInfo.setUcTargetPercentage((byte) 20);
        emvTerminalInfo.setUcMaxTargetPercentage((byte) 50);
        emvTerminalInfo.setUcAccountType((byte) 0);
        emvTerminalInfo.setUcIssuerCodeTableIndex((byte) 0);
        emvRet = emvL2.EMV_SetTerminalInfo(emvTerminalInfo);
        if (emvRet != EmvDefinition.EMV_OK) {
            return emvRet;
        }

        emvRet = emvL2.EMV_SetSupport_PBOC((byte) 0, (byte) 0, 0);
        if (emvRet != EmvDefinition.EMV_OK) {
            return emvRet;
        }

        return emvRet;
    }

    public synchronized void EmvProcess(EmvTransData transData, OnEmvProcessListener listener) throws RemoteException {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "EmvProcess");
        if (transData == null) {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "transData == null");
            return;
        }

        if (listener == null) {
            Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "listener == null");
            return;
        }

        emvTransData = transData;
        emvListener = listener;
        initTransData();

        int emvRet = 0;

        //EMV library initialization processing, each transaction must be called.
        emvRet = EMVInit();
        if (emvRet != EmvDefinition.EMV_OK) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Add AID to the EMV kernel from local code
//        byte[] pbocaid1 = {(byte) 0xA0, 0x00, 0x00, 0x03, 0x33, 0x01, 0x01};
//        emvL2.EMV_DelAllAIDList();
//        emvL2.EMV_AddAIDList(pbocaid1, (byte) pbocaid1.length, (byte) 1);

        //Add AID to the EMV kernel from databaseEMV_TermActionAnalyze
        List<Aid> mList = db.getAidDao().findAllAid();
        if (mList == null || mList.size() == 0) {
            Log.d(TAG, "findAllAid is null!");
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        } else {
            emvL2.EMV_DelAllAIDList();
            for (Aid aid : mList) {
                Log.d(TAG, "aid.getAid(): " + aid.getAid());
                byte[] aucAid = BytesUtil.hexString2Bytes(aid.getAid());
                emvL2.EMV_AddAIDList(aucAid, (byte) aucAid.length, (byte) 1);
            }
        }

        //Building the Candidate List
        //Create a list of ICC applications that are supported by the terminal.
        emvRet = emvL2.EMV_AppCandidateBuild((byte) 0);
        Log.d(TAG, "EMV_AppCandidateBuild emvRet : " + emvRet);
        if (emvRet != EmvDefinition.EMV_OK) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        while (true) {
            int candListCount = 0;
            int selectedAppIndex = 0;

            if (isEndEmv()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            candListCount = emvL2.EMV_AppGetCandListCount();
            if ((candListCount > 1) && (1 == emvKernelConfig.getbCardHolderConfirm())) {

                String strDisplayName[] = new String[candListCount];
                for (int i = 0; i < candListCount; i++) {
                    strDisplayName[i] = new String(emvL2.EMV_AppGetCandListItem(i).getAucDisplayName());
                }

                //Cardholder selects application from candidate list
                countDownLatchNew();
                emvListener.requestAidSelect(0, strDisplayName);
                countDownLatchAwait();
                if ((importAidSelectIndex < 0) || (importAidSelectIndex >= candListCount)) {
                    emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                    endEmv();
                    return;
                } else {
                    selectedAppIndex = importAidSelectIndex;
                }
            } else {
                selectedAppIndex = 0;
            }

            EmvCandidateItem emvCandidateItem = emvL2.EMV_AppGetCandListItem(selectedAppIndex);
            if (emvCandidateItem == null) {
                listener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //The terminal issues the SELECT command
            emvRet = emvL2.EMV_AppFinalSelect(emvCandidateItem);
            Log.d(TAG, "EMV_AppFinalSelect emvRet : " + emvRet);
            if ((EmvDefinition.EMV_APP_BLOCKED == emvRet)
                    || (EmvDefinition.EMV_NO_APP == emvRet)
                    || (EmvDefinition.EMV_INVALID_RESPONSE == emvRet)
                    || (EmvDefinition.EMV_INVALID_TLV == emvRet)
                    || (EmvDefinition.EMV_DATA_NOT_EXISTS == emvRet)) {
                candListCount = emvL2.EMV_AppGetCandListCount();
                if (candListCount > 1) {
                    emvL2.EMV_AppDelCandListItem(selectedAppIndex);
                    continue;
                }
            } else if (emvRet != EmvDefinition.EMV_OK) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            if (isEndEmv()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //Request amount
            countDownLatchNew();
            emvListener.requestImportAmount(1);
            countDownLatchAwait();
            if (importFixedAmount == null || importFixedAmount.length() == 0) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //Set amount , other amount
            if (0 != setTransAmount(Long.parseLong(importFixedAmount), 0)) {
                emvListener.onTransResult(EmvDefinition.EMV_TERMINATED);
                endEmv();
                return;
            }

            //Set some transaction data. Transaction Type , Transaction Date , Transaction Time   ... ...
            if (0 != setTransData()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //Set parameters according to each AID. Terminal floor limit, Trans currency code   ... ...
            if (0 != setTransDataFromAid()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //Callback after final Aid Select, We Can set the TLV parameters
            countDownLatchNew();
            emvListener.finalAidSelect();
            countDownLatchAwait();
            if (false == importFinalAidSelectRes) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }
            if (isEndEmv()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //Initiate Application Processing
            //The terminal issues the GET PROCESSING OPTIONS command
            emvRet = emvL2.EMV_GPOProc();
            Log.d(TAG, "EMV_GPOProc emvRet : " + emvRet);
            if (emvRet != EmvDefinition.EMV_OK) {
                int lastSW = emvL2.EMV_GetLastStatusWord();
                if (lastSW != 0x9000) {
                    candListCount = emvL2.EMV_AppGetCandListCount();
                    if (candListCount > 1) {
                        emvL2.EMV_AppDelCandListItem(selectedAppIndex);
                        continue;
                    }
                } else {
                    emvListener.onTransResult(getAppEmvtransResult(emvRet));
                    endEmv();
                    return;
                }
            }

            if (isEndEmv()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            //Jump out of while code block and continue to execute the following code
            break;
        }

        //Read Application Data
        //The terminal shall read the files and records indicated in the AFL using the
        //READ RECORD command identifying the file by its SFI.
        emvRet = emvL2.EMV_ReadRecordData();
        Log.d(TAG, "EMV_ReadRecordData emvRet : " + emvRet);
        if (emvRet != EmvDefinition.EMV_OK) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Wait for the cardholder to confirm the card number
        String cardNo = getCardNo();
        countDownLatchNew();
        emvListener.onConfirmCardInfo(cardNo);
        countDownLatchAwait();
        if (false == importConfirmCardInfoResult) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Offline Data Authentication
        //The terminal uses the RID and index to retrieve the terminal-stored CAPK
        emvRet = retrieveCAPK();
        if (emvRet != 0) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        emvRet = emvL2.EMV_OfflineDataAuth();
        Log.d(TAG, "EMV_OfflineDataAuth emvRet : " + emvRet);
        if ((emvRet == EmvDefinition.EMV_ICC_ERROR) || (emvRet == EmvDefinition.EMV_TERMINATED)) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Terminal Risk Management
        emvRet = emvL2.EMV_TerminalRiskManagement();
        Log.d(TAG, "EMV_TerminalRiskManagement emvRet : " + emvRet);
        if (emvRet != EmvDefinition.EMV_OK) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Processing Restrictions
        emvRet = emvL2.EMV_ProcessingRestrictions();
        Log.d(TAG, "EMV_ProcessingRestrictions emvRet : " + emvRet);
        if (emvRet != EmvDefinition.EMV_OK) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Cardholder Verification
        emvRet = emvL2.EMV_CardHolderVerify();
        Log.d(TAG, "EMV_CardHolderVerify emvRet : " + emvRet);
        if (emvRet != EmvDefinition.EMV_OK) {
            emvListener.onTransResult(getAppEmvtransResult(emvRet));
            endEmv();
            return;
        }
        if (isEndEmv()) {
            emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
            endEmv();
            return;
        }

        //Terminal Action Analysis
        emvRet = emvL2.EMV_TermActionAnalyze();
        Log.d(TAG, "EMV_TermActionAnalyze emvRet : " + emvRet);
        if (emvRet == EmvDefinition.EMV_ONLINE_REQUEST) {
            int onlineResult = EmvDefinition.EMV_ONLINE_CONNECT_FAILED;
            byte[] authCode = null; //89 Authorisation Code
            byte[] authRespCode = null; //8A Authorisation Response Code
            byte[] issueAuthData = null; //91 Issuer Authentication Data
            byte[] issueScript71 = null; //71 Issuer Script
            byte[] issueScript72 = null; //72 Issuer Script

            if (isEndEmv()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            emvRet = emvL2.EMV_OnlineTransEx();
            Log.d(TAG, "EMV_OnlineTransEx emvRet : " + emvRet);
            if (emvRet == EmvDefinition.EMV_OK) {
                //Perform online processing
                countDownLatchNew();
                emvListener.onRequestOnline();
                countDownLatchAwait();

                if (isEndEmv()) {
                    emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                    endEmv();
                    return;
                }

                /*if (importOnlineRes) {
                    onlineResult = EmvDefinition.EMV_ONLINE_APPROVED;
                } else {
                    onlineResult = EmvDefinition.EMV_ONLINE_ERROR;
                }*/

                if (importOnlineRes && !TextUtils.isEmpty(importRespCode)) {
                    if ("00".equals(importRespCode))
                        onlineResult = EmvDefinition.EMV_ONLINE_APPROVED;
                    if ("01".equals(importRespCode))
                        onlineResult = EmvDefinition.EMV_ONLINE_VOICE_PREFER;
                    else // 联机成功，F39 ！=null && ！=00 && ！=01 返回拒绝
                        onlineResult = EmvDefinition.EMV_ONLINE_REJECT;
                } else {
                    onlineResult = EmvDefinition.EMV_ONLINE_ERROR;
                }

                authRespCode = BytesUtil.hexString2Bytes(importRespCode);

                String strRespIcc55 = importIcc55;
                TlvList tlvList = new TlvList();
                tlvList.fromHex(strRespIcc55);
                Tlv tlv = tlvList.getTlv("89");
                if (tlv != null)
                    authCode = tlv.getValue();
                tlv = tlvList.getTlv("91");
                if (tlv != null)
                    issueAuthData = tlv.getValue();
                tlv = tlvList.getTlv("71");
                if (tlv != null)
                    issueScript71 = tlv.getValue();
                tlv = tlvList.getTlv("72");
                if (tlv != null)
                    issueScript72 = tlv.getValue();

                emvRet = emvL2.EMV_ProcessOnlineRespData(onlineResult, issueAuthData, authRespCode, authCode);
                Log.d(TAG, "EMV_ProcessOnlineRespData emvRet : " + emvRet);
            }

            if (isEndEmv()) {
                emvListener.onTransResult(getAppEmvtransResult(EmvDefinition.EMV_TERMINATED));
                endEmv();
                return;
            }

            if (emvRet != EmvDefinition.EMV_TERMINATED) {
                emvL2.EMV_IssueToCardScript((byte) 1, issueScript71);
            }

            if (emvRet == EmvDefinition.EMV_OK) {
                if (onlineResult == EmvDefinition.EMV_ONLINE_APPROVED) {
                    emvRet = emvL2.EMV_Completion((byte) 1);
                } else if (onlineResult == EmvDefinition.EMV_ONLINE_VOICE_PREFER) {
                    emvRet = emvL2.EMV_Completion((byte) 1);
                } else {
                    emvRet = emvL2.EMV_Completion((byte) 0);
                }
            } else if (emvRet == EmvDefinition.EMV_DECLINED) {
                emvRet = emvL2.EMV_Completion((byte) 0);
            } else if (emvRet == EmvDefinition.EMV_APPROVED) {
                emvRet = emvL2.EMV_Completion((byte) 1);
            }

            if (emvRet != EmvDefinition.EMV_TERMINATED) {
                emvL2.EMV_IssueToCardScript((byte) 0, issueScript72);
            }

            //In order to make the transaction successful
            //the results of the second GAC were artificially modified
            emvRet = EmvDefinition.EMV_APPROVED;
        }

        emvListener.onTransResult(getAppEmvtransResult(emvRet));
        endEmv();
    }

    @Override
    public void endEmv() {
        Log.d(TAG, "endEmv");
        isEndEmv = true;
//        try {
//            Log.d(TAG, "emvL2.EMV_FreeCallback >>>");
//            emvL2.EMV_FreeCallback();
//            Log.d(TAG, "emvL2.EMV_FreeCallback <<<");
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
        countDownLatchdDown();
    }

    private boolean isEndEmv() {
        Log.d(TAG, "isEndEmv() : " + isEndEmv);
        return isEndEmv;
    }

    @Override
    public void setTlv(String tag, byte[] value) {
        Log.d(TAG, "setTlvData tag: " + tag + ", value: " + BytesUtil.bytes2HexString(value));
        if ((null == tag) || (null == value)) {
            return;
        }

        if ((tag.length() <= 0) || (tag.length() > 8)) {
            return;
        }

        byte[] bTag = BytesUtil.hexString2Bytes(tag);
        Log.d(TAG, "bTag: " + BytesUtil.bytes2HexString(bTag));

        byte[] bTag4Bytes = new byte[4];
        java.util.Arrays.fill(bTag4Bytes, (byte) 0);
        System.arraycopy(bTag, 0, bTag4Bytes, bTag4Bytes.length - bTag.length, bTag.length);
        Log.d(TAG, "bTag4Bytes: " + BytesUtil.bytes2HexString(bTag4Bytes));

        //The first parameter of 'BytesUtil.bytes2Int' must be 4 bytes
        int iTag = BytesUtil.bytes2Int(bTag4Bytes, true);
        Log.d(TAG, "iTag: " + iTag);

        try {
            emvL2.EMV_SetTLVData(iTag, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getTlv(String tag) {
        Log.d(TAG, "getTlvData tag: " + tag);
        if (null == tag) {
            return null;
        }

        int iTag = Integer.parseInt(tag, 16);
        Log.d(TAG, "getTlvData iTag: " + iTag);

        byte[] value = new byte[0];
        try {
            value = emvL2.EMV_GetTLVData(iTag);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        TlvList tlvList = new TlvList();
        tlvList.addTlv(tag, value);
        byte[] tlv = tlvList.getBytes();

        Log.d(TAG, "getTlvData value: " + BytesUtil.bytes2HexString(value));
        Log.d(TAG, "getTlvData getBytes: " + BytesUtil.bytes2HexString(tlv));
        return tlv;
    }

    /**
     * The terminal uses the RID and index to retrieve the terminal-stored CAPK
     *
     * @return
     * @throws RemoteException
     */
    private int retrieveCAPK() throws RemoteException {
        int emvRet = 0;

        Log.d(TAG, "Into retrieveCAPK()");

        byte[] aid = emvL2.EMV_GetTLVData(0x9F06);
        if ((aid == null) || (aid.length < 5) || (aid.length > 16)) {
            Log.d(TAG, "Get aid(9F06) failed!");
            return -1;
        }
        Log.d(TAG, "aid(9F06): " + BytesUtil.bytes2HexString(aid));

        byte[] index = emvL2.EMV_GetTLVData(0x8F);
        if ((index == null) || (index.length != 1)) {
            Log.d(TAG, "Get CAPK index(8F) failed!");
            return 0;
        }
        Log.d(TAG, "CAPK index(8F): " + BytesUtil.bytes2HexString(index));

        byte[] rid = new byte[5];
        System.arraycopy(aid, 0, rid, 0, 5);
        String strRid = BytesUtil.bytes2HexString(rid);
        Capk capk = db.getCapkDao().findByRidIndex(strRid, index[0]);
        if (null == capk) {
            Log.d(TAG, "findByRidIndex failed!");
            return 0;
        }

        Log.d(TAG, "getRid(): " + capk.getRid());
        Log.d(TAG, "getIndex(): " + capk.getIndex());
        Log.d(TAG, "getArithInd(): " + capk.getArithInd());
        Log.d(TAG, "getHashInd(): " + capk.getHashInd());
        Log.d(TAG, "getExponent(): " + BytesUtil.bytes2HexString(capk.getExponent()));
        Log.d(TAG, "getModul(): " + BytesUtil.bytes2HexString(capk.getModul()));
        Log.d(TAG, "getExpDate(): " + BytesUtil.bytes2HexString(capk.getExpDate()));
        Log.d(TAG, "getCheckSum(): " + BytesUtil.bytes2HexString(capk.getCheckSum()));

        byte[] tempExpDate = new byte[3]; //YYMMDD
        if (4 == capk.getExpDate().length) {
            System.arraycopy(capk.getExpDate(), 1, tempExpDate, 0, 3);
        } else if (8 == capk.getExpDate().length) {
            String strExpDate = new String(capk.getExpDate());
            byte[] bcdExpDate = BytesUtil.hexString2Bytes(strExpDate);
            System.arraycopy(bcdExpDate, 1, tempExpDate, 0, 3);
        } else {
            //301231
            tempExpDate[0] = 0x30;
            tempExpDate[1] = 0x12;
            tempExpDate[2] = 0x31;
        }
        Log.d(TAG, "tempExpDate(): " + BytesUtil.bytes2HexString(tempExpDate));

        EmvCapk emvCapk = new EmvCapk();
        emvCapk.setRID(BytesUtil.hexString2Bytes(capk.getRid()));
        emvCapk.setKeyID(capk.getIndex());
        emvCapk.setArithInd(capk.getArithInd());
        emvCapk.setHashInd(capk.getHashInd());
        emvCapk.setExponent(capk.getExponent());
        emvCapk.setModul(capk.getModul());
        emvCapk.setExpDate(tempExpDate);
        emvCapk.setCheckSum(PayDataUtil.getCAPKChecksum(capk));

        emvL2.EMV_DelAllCAPK();
        emvRet = emvL2.EMV_AddCAPK(emvCapk);
        Log.d(TAG, "EMV_AddCAPK emvRet : " + emvRet);
        if (emvRet != 0) {
            Log.d(TAG, "EMV_AddCAPK failed!");
            return 0;
        }

        //Add CAPK Revocation list
//        EmvRevocList emvRevocList = new EmvRevocList();
//        emvL2.EMV_DelAllRevoIPK();
//        emvRet = emvL2.EMV_AddRevoIPK(emvRevocList);
//        Log.d(TAG, "EMV_AddRevoIPK emvRet : " + emvRet);
//        if (emvRet != 0) {
//            Log.d(TAG, "EMV_AddRevoIPK failed!");
//            return -1;
//        }

        return 0;
    }

    private String getCardNo() throws RemoteException {
        Log.d(TAG, "Into getCardNo()");

        byte[] PAN = emvL2.EMV_GetTLVData(0x5A);
        if (PAN == null) {
            Log.d(TAG, "Get AID(5A) failed!");
            return null;
        }

        String cardNo = BytesUtil.bytes2HexString(PAN);
        if (cardNo == null) {
            return null;
        }

        cardNo = cardNo.toUpperCase().replace("F", "");
        Log.d(TAG, "getCardNo(): " + cardNo);
        return cardNo;
    }

    private int setTransAmount(long amt, int otherAmt) throws RemoteException {
        Log.d(TAG, "Into setTransAmount()");
        if (amt < 0) {
            return 0;
        }

        if (otherAmt < 0) {
            return 0;
        }

        String s9F02 = String.format("%012d", amt);
        String s9F03 = String.format("%012d", otherAmt);
        String s81 = String.format("%08X", amt);
        String s9F04 = String.format("%08X", otherAmt);

        Log.d(TAG, "s9F02: " + s9F02);
        Log.d(TAG, "s9F03: " + s9F03);
        Log.d(TAG, "s81: " + s81);
        Log.d(TAG, "s9F04: " + s9F04);

        emvL2.EMV_SetTLVData(0x9F02, BytesUtil.hexString2Bytes(s9F02));
        emvL2.EMV_SetTLVData(0x9F03, BytesUtil.hexString2Bytes(s9F03));
        emvL2.EMV_SetTLVData(0x81, BytesUtil.hexString2Bytes(s81));
        emvL2.EMV_SetTLVData(0x9F04, BytesUtil.hexString2Bytes(s9F04));

        return 0;
    }

    private int setTransData() throws RemoteException {
        Log.d(TAG, "Into setTransData()");

        PayDataUtil dataUtil = new PayDataUtil();

        //Transaction Type
        byte[] transType = new byte[1];
        transType[0] = emvTransData.getTransType();
        emvL2.EMV_SetTLVData(0x9C, transType);

        //The getRandom function returns a fixed 8 byte random number
        byte[] random = pinPad.getRandom();
        byte[] unpredictableNum = new byte[4];
        System.arraycopy(random, 0, unpredictableNum, 0, 4);
        emvL2.EMV_SetTLVData(0x9F37, unpredictableNum);

        //Transaction Sequence Counter
        int tsc = (int) dataUtil.getSerialNumber();
        emvL2.EMV_SetTLVData(0x9F41, BytesUtil.int2Bytes(tsc, true));

        ///Transaction Date
        String date = dataUtil.getTransDateTime(PayDataUtil.TRANS_DATE_YYMMDD);
        emvL2.EMV_SetTLVData(0x9A, BytesUtil.hexString2Bytes(date));

        //Transaction Time
        String time = dataUtil.getTransDateTime(PayDataUtil.TRANS_TIME_HHMMSS);
        emvL2.EMV_SetTLVData(0x9F21, BytesUtil.hexString2Bytes(time));

        return 0;
    }

    private int setTransDataFromAid() throws RemoteException {
        Log.d(TAG, "Into setTransDataFromAid()");

        int emvRet = 0;

        byte[] aid = emvL2.EMV_GetTLVData(0x9F06);
        if ((aid == null) || (aid.length < 5) || (aid.length > 16)) {
            Log.d(TAG, "Get AID(9F06) failed!");
            return -1;
        }

        String strAid = BytesUtil.bytes2HexString(aid);
        Aid aidParam = db.getAidDao().findByAidAndAsi(strAid);

        emvL2.EMV_SetTLVData(0x9F09, aidParam.getVersion());
        //emvL2.EMV_SetTLVData(0x9F1B, aidParam.getFloorLimit());

        //EmvTerminalInfo emvTerminalInfo = emvL2.EMV_GetTerminalInfo();
        //TODO Set terminal info from the AID parameter stored at the terminal
        //emvL2.EMV_SetTerminalInfo(emvTerminalInfo);

        Log.d(TAG, "getAcquierId = " + aidParam.getAcquierId());
        Log.d(TAG, "getAcquierId(bytes) = " + BytesUtil.bytes2HexString(aidParam.getAcquierId()));
        if ((aidParam.getAcquierId() != null) && (aidParam.getAcquierId().length == 6)) {
            emvTerminalInfo.setAucTerminalAcquireID(new String(aidParam.getAcquierId()));
        }
        return 0;
    }


//    private int setTransDataFromAid() throws RemoteException {
////        Log.d(TAG, "Into setTransDataFromAid()");
////
////        int emvRet = 0;
////
////        byte[] aid = emvL2.EMV_GetTLVData(0x9F06);
////        if ((aid == null) || (aid.length < 5) || (aid.length > 16)) {
////            Log.d(TAG, "Get AID(9F06) failed!");
////            return -1;
////        }
////
////        String strAid = BytesUtil.bytes2HexString(aid);
////        Aid aidParam = db.getAidDao().findByAidAndAsi(strAid);
////
////        //emvL2.EMV_SetTLVData(0x9F09, aidParam.getVersion());
////        //emvL2.EMV_SetTLVData(0x9F1B, aidParam.getFloorLimit());
////
////        //EmvTerminalInfo emvTerminalInfo = emvL2.EMV_GetTerminalInfo();
////        //TODO Set terminal info from the AID parameter stored at the terminal
////        //emvL2.EMV_SetTerminalInfo(emvTerminalInfo);
////        return 0;
//
//        Log.d(TAG, "Into setTransDataFromAid()");
//
//        int emvRet = 0;
//
//        byte[] aid = emvL2.EMV_GetTLVData(0x9F06);
//        if ((aid == null) || (aid.length < 5) || (aid.length > 16)) {
//            Log.d(TAG, "Get AID(9F06) failed!");
//            return -1;
//        }
//
//        String strAid = BytesUtil.bytes2HexString(aid);
//        Aid aidParam = db.getAidDao().findByAidAndAsi(strAid);
//
//        Log.d(TAG, "getAid = " + aidParam.getAid());
//
//        Log.d(TAG, "getVersion = " + BytesUtil.bytes2HexString(aidParam.getVersion()));
//        emvL2.EMV_SetTLVData(0x9F09, aidParam.getVersion());
//
//        Log.d(TAG, "getFloorLimit(int) = " + aidParam.getFloorLimit());
//        byte[] floorLimit = BytesUtil.int2Bytes(aidParam.getFloorLimit(), true);
//        Log.d(TAG, "getFloorLimit(bytes) = " + BytesUtil.bytes2HexString(floorLimit));
//        emvL2.EMV_SetTLVData(0x9F1B, floorLimit);
//
//        //Set terminal info from the AID parameter stored at the terminal
//        EmvTerminalInfo emvTerminalInfo = emvL2.EMV_GetTerminalInfo();
//        Log.d(TAG, "emvTerminalInfo = " + emvTerminalInfo);
//
//        Log.d(TAG, "getFloorLimit(int) = " + aidParam.getFloorLimit());
//        emvTerminalInfo.setUnTerminalFloorLimit(aidParam.getFloorLimit());
//
//        Log.d(TAG, "getThreShold(int) = " + aidParam.getThreShold());
//        emvTerminalInfo.setUnThresholdValue(aidParam.getThreShold());
//
//        Log.d(TAG, "getTermId = " + aidParam.getTermId());
//        Log.d(TAG, "getTermId(bytes) = " + BytesUtil.bytes2HexString(aidParam.getTermId()));
//        if ((aidParam.getTermId() != null) && (aidParam.getTermId().length == 8)) {
//            emvTerminalInfo.setAucTerminalID(new String(aidParam.getTermId()));
//        }
//
//        Log.d(TAG, "getMerchId = " + aidParam.getMerchId());
//        Log.d(TAG, "getMerchId(bytes) = " + BytesUtil.bytes2HexString(aidParam.getMerchId()));
//        if ((aidParam.getMerchId() != null) && (aidParam.getMerchId().length == 15)) {
//            emvTerminalInfo.setAucMerchantID(new String(aidParam.getMerchId()));
//        }
//
//        Log.d(TAG, "getMerchCateCode = " + aidParam.getMerchCateCode());
//        Log.d(TAG, "getMerchCateCode(bytes) = " + BytesUtil.bytes2HexString(aidParam.getMerchCateCode()));
//        if ((aidParam.getMerchCateCode() != null) && (aidParam.getMerchCateCode().length == 2)) {
//            emvTerminalInfo.setAucMerchantCategoryCode(aidParam.getMerchCateCode());
//        }
//
//        Log.d(TAG, "getMerchName = " + aidParam.getMerchName());
//        Log.d(TAG, "getMerchName(bytes) = " + BytesUtil.bytes2HexString(aidParam.getMerchName()));
//        if ((aidParam.getMerchName() != null) && (aidParam.getMerchName().length > 0)) {
//            emvTerminalInfo.setAucMerchantNameLocation(aidParam.getMerchName());
//        }
//
//        Log.d(TAG, "getTransCurrCode = " + aidParam.getTransCurrCode());
//        Log.d(TAG, "getTransCurrCode(bytes) = " + BytesUtil.bytes2HexString(aidParam.getTransCurrCode()));
//        if ((aidParam.getTransCurrCode() != null) && (aidParam.getTransCurrCode().length == 2)) {
//            emvTerminalInfo.setAucTransCurrencyCode(aidParam.getTransCurrCode());
//        }
//
//        Log.d(TAG, "getTransCurrExp = " + aidParam.getTransCurrExp());
//        Log.d(TAG, "getTransCurrExp(bytes) = " + BytesUtil.byte2HexString(aidParam.getTransCurrExp()));
//        if (aidParam.getTransCurrExp() != 0) {
//            emvTerminalInfo.setUcTransCurrencyExp(aidParam.getTransCurrExp());
//        }
//
//        Log.d(TAG, "getReferCurrCode = " + aidParam.getReferCurrCode());
//        Log.d(TAG, "getReferCurrCode(bytes) = " + BytesUtil.bytes2HexString(aidParam.getReferCurrCode()));
//        if ((aidParam.getReferCurrCode() != null) && (aidParam.getReferCurrCode().length == 2)) {
//            emvTerminalInfo.setAucTransRefCurrencyCode(aidParam.getReferCurrCode());
//        }
//
//        Log.d(TAG, "getReferCurrExp = " + aidParam.getReferCurrExp());
//        Log.d(TAG, "getReferCurrExp(bytes) = " + BytesUtil.byte2HexString(aidParam.getReferCurrExp()));
//        if (aidParam.getReferCurrExp() != 0) {
//            emvTerminalInfo.setUcTransRefCurrencyExp(aidParam.getReferCurrExp());
//        }
//
//        Log.d(TAG, "getAcquierId = " + aidParam.getAcquierId());
//        Log.d(TAG, "getAcquierId(bytes) = " + BytesUtil.bytes2HexString(aidParam.getAcquierId()));
//        if ((aidParam.getAcquierId() != null) && (aidParam.getAcquierId().length == 6)) {
//            emvTerminalInfo.setAucTerminalAcquireID(new String(aidParam.getAcquierId()));
//        }
//
//        Log.d(TAG, "getVersion = " + aidParam.getVersion());
//        Log.d(TAG, "getVersion(bytes) = " + BytesUtil.bytes2HexString(aidParam.getVersion()));
//        if ((aidParam.getVersion() != null) && (aidParam.getVersion().length == 2)) {
//            emvTerminalInfo.setAucAppVersion(aidParam.getVersion());
//        }
//
//        Log.d(TAG, "getdDol = " + aidParam.getdDol());
//        Log.d(TAG, "getdDol(bytes) = " + BytesUtil.bytes2HexString(aidParam.getdDol()));
//        if ((aidParam.getdDol() != null) && (aidParam.getdDol().length > 0)) {
//            emvTerminalInfo.setAucDefaultDDOL(aidParam.getdDol());
//        }
//
//        Log.d(TAG, "gettDol = " + aidParam.gettDol());
//        Log.d(TAG, "gettDol(bytes) = " + BytesUtil.bytes2HexString(aidParam.gettDol()));
//        if ((aidParam.gettDol() != null) && (aidParam.gettDol().length > 0)) {
//            emvTerminalInfo.setAucDefaultTDOL(aidParam.gettDol());
//        }
//
//        Log.d(TAG, "getTacDenial = " + aidParam.getTacDenial());
//        Log.d(TAG, "getTacDenial(bytes) = " + BytesUtil.bytes2HexString(aidParam.getTacDenial()));
//        if ((aidParam.getTacDenial() != null) && (aidParam.getTacDenial().length == 5)) {
//            emvTerminalInfo.setAucTACDenial(aidParam.getTacDenial());
//        }
//
//        Log.d(TAG, "getTacOnline = " + aidParam.getTacOnline());
//        Log.d(TAG, "getTacOnline(bytes) = " + BytesUtil.bytes2HexString(aidParam.getTacOnline()));
//        if ((aidParam.getTacOnline() != null) && (aidParam.getTacOnline().length == 5)) {
//            emvTerminalInfo.setAucTACOnline(aidParam.getTacOnline());
//        }
//
//        Log.d(TAG, "getTacDefault = " + aidParam.getTacDefault());
//        Log.d(TAG, "getTacDefault(bytes) = " + BytesUtil.bytes2HexString(aidParam.getTacDefault()));
//        if ((aidParam.getTacDefault() != null) && (aidParam.getTacDefault().length == 5)) {
//            emvTerminalInfo.setAucTACDefault(aidParam.getTacDefault());
//        }
//
//        Log.d(TAG, "getTargetPer = " + aidParam.getTargetPer());
//        Log.d(TAG, "getTargetPer(bytes) = " + BytesUtil.byte2HexString(aidParam.getTargetPer()));
//        emvTerminalInfo.setUcTargetPercentage(aidParam.getTargetPer());
//
//        Log.d(TAG, "getMaxTargetPer = " + aidParam.getMaxTargetPer());
//        Log.d(TAG, "getMaxTargetPer(bytes) = " + BytesUtil.byte2HexString(aidParam.getMaxTargetPer()));
//        emvTerminalInfo.setUcMaxTargetPercentage(aidParam.getMaxTargetPer());
//
//        emvL2.EMV_SetTerminalInfo(emvTerminalInfo);
//        return 0;
//    }

    private byte[] getOfflinePinBlock(String pin) {
        Log.d(TAG, "Into getOfflinePinBlock()");

        if (pin == null) {
            return null;
        }

        if ((pin.length() == 0) || (pin.length() > 14)) {
            return null;
        }

        for (int i = 0; i < pin.length(); i++) {
            if ((pin.charAt(i) < '0') || (pin.charAt(i) > '9')) {
                return null;
            }
        }

        Log.d(TAG, "pin: " + pin);

        String strBlock = new String();
        strBlock += "2";
        strBlock += String.format("%X", pin.length());
        strBlock += pin;

        Log.d(TAG, "strBlock: " + strBlock);

        while (strBlock.length() < 16) {
            strBlock = strBlock.concat("F");
        }

        Log.d(TAG, "strBlock: " + strBlock);
        return BytesUtil.hexString2Bytes(strBlock);
    }

    private byte getAppEmvtransResult(int emvkernelRetCode) {
        byte transResult = 0;

        switch (emvkernelRetCode) {
            case EmvDefinition.EMV_APPROVED:
            case EmvDefinition.EMV_FORCE_APPROVED:
                transResult = PayDataUtil.CardCode.TRANS_APPROVAL;
                break;

            case EmvDefinition.EMV_DECLINED:
                transResult = PayDataUtil.CardCode.TRANS_REFUSE;
                break;

            default:
                transResult = PayDataUtil.CardCode.TRANS_STOP;
                break;
        }

        return transResult;
    }

    @Override
    public boolean importAidSelectRes(int index) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importAidSelectRes index: " + index);
        if (index < 0) {
            endEmv();
            return false;
        }
        importAidSelectIndex = index;
        countDownLatchdDown();
        return true;
    }

    @Override
    public boolean importFinalAidSelectRes(boolean res) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importFinalAidSelectRes res: " + res);
        importFinalAidSelectRes = res;
        countDownLatchdDown();
        return true;
    }

    @Override
    public boolean importAmount(String amt) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importAmount amt: " + amt);
        if (amt == null) {
            endEmv();
            return false;
        }
        //importFixedAmount = String.format("%012d",Long.parseLong(amt));
        importFixedAmount = AmountUtil.getFixedAmount(amt);
        Log.d(TAG, "getFixedAmount(): " + importFixedAmount);
        countDownLatchdDown();
        return true;
    }

    @Override
    public boolean importMsgConfirmRes(boolean res) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importMsgConfirmRes res: " + res);
        importMsgConfirmResult = res;
        countDownLatchdDown();
        return true;
    }

    @Override
    public boolean importECashTipConfirmRes(boolean res) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importECashTipConfirmRes res: " + res);
        return true;
    }

    @Override
    public boolean importConfirmCardInfoRes(boolean res) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importConfirmCardInfoRes res: " + res);
        importConfirmCardInfoResult = res;
        countDownLatchdDown();
        return true;
    }

    @Override
    public boolean importPin(String pin) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importPin pin: " + pin);
        if (pin == null) {
            endEmv();
            return false;
        }
        importPinStr = pin;
        countDownLatchdDown();
        return true;
    }

    @Override
    public boolean importUserAuthRes(boolean res) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importUserAuthRes" + " res: " + res);
        return true;
    }

    @Override
    public boolean importOnlineResp(boolean onlineRes, String respCode, String icc55) {
        Log.d(TAG, CommonFunction._FILE_LINE_FUN_() + "importOnlineResp onlineRes: " + onlineRes + "; respCode: " + respCode + "; icc55: " + icc55);
        if (respCode == null) {
            endEmv();
            return false;
        }
        importOnlineRes = onlineRes;
        importRespCode = respCode;
        importIcc55 = icc55;
        countDownLatchdDown();
        return true;
    }
}

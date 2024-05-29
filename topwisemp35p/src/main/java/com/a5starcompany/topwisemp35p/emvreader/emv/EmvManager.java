package com.a5starcompany.topwisemp35p.emvreader.emv;

import android.os.RemoteException;

import com.a5starcompany.topwisemp35p.emvreader.card.CardType;
import com.a5starcompany.topwisemp35p.emvreader.util.CommonFunction;
import com.a5starcompany.topwisemp35p.emvreader.util.SDKLog;
import com.a5starcompany.topwisemp35p.emvreader.database.table.DBManager;
import com.topwise.cloudpos.aidl.emv.level2.EmvTerminalInfo;
import com.a5starcompany.topwisemp35p.emvreader.emv.impl.ClsCardProcess;
import com.a5starcompany.topwisemp35p.emvreader.emv.impl.ContactCardProcess;
import com.a5starcompany.topwisemp35p.emvreader.emv.impl.EmvProcessInterface;


public class EmvManager implements EmvProcessInterface {
    private static final String TAG = "EmvManager";
    private static EmvManager instance;

    private static final int PARAM_ERROR = -1;
    private EmvTransData emvTransData;
    private OnEmvProcessListener listener;
    private EmvTerminalInfo emvTerminalInfo;
    private volatile EmvProcessThread emvProcessThread = null;
    private DBManager db = DBManager.getInstance();
    private int cardType = CardType.NONE;
    private boolean isProcessEmv = false;
    private EmvProcessInterface emvProcessInterface;

    /**
     * process PBOC thread
     */
    private class EmvProcessThread extends Thread {

        private EmvProcessThread() {
        }

        @Override
        public void run() {
            if (cardType == CardType.IC) {
                try {
                    ContactCardProcess.getInstance().EmvProcess(emvTransData, listener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (cardType == CardType.RF) {
                int entryLibRes = 0;
                try {
                    entryLibRes = ClsCardProcess.getInstance().processEntryLib(emvTransData, listener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                SDKLog.d(TAG, "processEntryLib res: " + entryLibRes);
            }
        }
    }

    private EmvManager() {

    }

    public synchronized static EmvManager getInstance() {
        if (instance == null) {
            instance = new EmvManager();
        }
        return instance;
    }


    public synchronized void startEmvProcess(EmvTransData emvTransData, OnEmvProcessListener listener) {
        if (emvTransData == null || listener == null) {
            SDKLog.d(TAG, "input param is null");
            return;
        }
        SDKLog.d(TAG, emvTransData.toString());
        this.emvTransData = emvTransData;
        this.listener = listener;
        cardType = emvTransData.getCardType();
        if (cardType == CardType.RF) {
            emvProcessInterface = ClsCardProcess.getInstance();
        } else {
            emvProcessInterface = ContactCardProcess.getInstance();
        }

        if (emvProcessThread != null && emvProcessThread.isAlive()) {
            SDKLog.e(TAG, CommonFunction._FILE_LINE_FUN_() + "pbocThread is alive");
            try {
                abortEMV();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //return;
        }
        isProcessEmv = true;
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "EmvTransData: " + emvTransData.toString());
        emvProcessThread = new EmvProcessThread();
        emvProcessThread.start();
    }

    public void setEmvTerminalInfo(EmvTerminalInfo info) {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "info=" + info.toString());
        emvTerminalInfo = info;
    }

    public EmvTerminalInfo getEmvTerminalInfo() {
        return emvTerminalInfo;
    }

    /**
     * End EMV process
     *
     * @throws RemoteException RemoteException
     */
    @Override
    public void endEmv() {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "endEMV");
        if (emvProcessInterface != null)
            emvProcessInterface.endEmv();
        if (emvProcessThread != null) {
            emvProcessThread.interrupt();
        }
    }

    /**
     * abort EMV process, equivalent to the endPBOC method
     *
     * @throws RemoteException RemoteException
     */
    public void abortEMV() throws RemoteException {
        SDKLog.d(TAG, CommonFunction._FILE_LINE_FUN_() + "abortPBOC");
        endEmv();
    }

    public void setTlv(String tag, byte[] data) {
        emvProcessInterface.setTlv(tag, data);
    }

    public byte[] getTlv(String tag) {
        return emvProcessInterface.getTlv(tag);
    }


    /**
     * update aid param
     */
    public boolean updateAID(int cmd, String aid) {
        boolean retval;

        if (aid == null) {
            return false;
        }

        retval = true;
        switch (cmd) {
            case 0x01:
                DBManager.getInstance().getAidDao().addAid(aid);
                break;
            case 0x02:
                DBManager.getInstance().getAidDao().deleteAid(aid);
                break;
            case 0x03:
                DBManager.getInstance().getAidDao().deleteAllAid();
                break;
            default:
                retval = false;
                break;
        }

        return retval;

    }

    public boolean updateCAPK(int cmd, String capk) {
        boolean retval;

        if (capk == null) {
            return false;
        }

        retval = true;
        switch (cmd) {
            case 0x01:
                DBManager.getInstance().getCapkDao().addCapk(capk);
                break;
            case 0x02:
                DBManager.getInstance().getCapkDao().deleteCapk(capk);
                break;
            case 0x03:
                DBManager.getInstance().getCapkDao().deleteAllCapk();
                break;
            default:
                retval = false;
                break;
        }

        return retval;
    }

    @Override
    public boolean importAmount(String amt) {
        return emvProcessInterface.importAmount(amt);
    }

    @Override
    public boolean importFinalAidSelectRes(boolean res) {
        return emvProcessInterface.importFinalAidSelectRes(res);
    }

    @Override
    public boolean importAidSelectRes(int index) {
        return emvProcessInterface.importAidSelectRes(index);
    }

    @Override
    public boolean importPin(String pin) {
        return emvProcessInterface.importPin(pin);
    }

    @Override
    public boolean importUserAuthRes(boolean res) {
        return emvProcessInterface.importUserAuthRes(res);
    }

    @Override
    public boolean importMsgConfirmRes(boolean confirm) {
        return emvProcessInterface.importMsgConfirmRes(confirm);
    }

    @Override
    public boolean importECashTipConfirmRes(boolean confirm) {
        return emvProcessInterface.importECashTipConfirmRes(confirm);
    }

    @Override
    public boolean importOnlineResp(boolean onlineRes, String respCode, String icc55) {
        return emvProcessInterface.importOnlineResp(onlineRes, respCode, icc55);
    }

    @Override
    public boolean importConfirmCardInfoRes(boolean res) {
        return emvProcessInterface.importConfirmCardInfoRes(res);
    }

}

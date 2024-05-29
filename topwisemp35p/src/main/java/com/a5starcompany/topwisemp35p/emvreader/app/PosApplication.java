package com.a5starcompany.topwisemp35p.emvreader.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.a5starcompany.topwisemp35p.charackterEncoder.BCDASCII;
import com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvManager;
import com.a5starcompany.topwisemp35p.emvreader.emv.Processor;
import com.a5starcompany.topwisemp35p.emvreader.DeviceTopUsdkServiceManager;
import com.a5starcompany.topwisemp35p.emvreader.database.table.AidDaoImpl;
import com.a5starcompany.topwisemp35p.emvreader.database.table.DBManager;
import com.topwise.cloudpos.aidl.card.AidlCheckCard;
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad;
import com.topwise.cloudpos.data.PinpadConstant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class PosApplication {
    private static final String TAG = "jeremy " + PosApplication.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    @SuppressLint("StaticFieldLeak")
    private static PosApplication mPosApplication;
    public ConsumeData mConsumeData;

    public static void init(Context activity) {
        Log.i(TAG,"onCreate");
        mContext = activity;

        DBManager.getInstance().init(activity);
        DeviceTopUsdkServiceManager.Companion.getInstance();
        initApp();
    }

    public static int CONSUME = 1;
    public static int CARD_SCHEME = 0;

    public static PosApplication getApp() {
        mPosApplication = mPosApplication == null ? new PosApplication() : mPosApplication;
        return mPosApplication;
    }

    public void setTransactionType(int transactionType) {
        this.transactionType = transactionType;
    }


    private int transactionType = 0;

    public int getTransactionType() {
        return transactionType;
    }

    private Processor processor;

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public Processor getProcessor() {
        return processor;

    }

    public void setConsumeData() {
        this.mConsumeData = new ConsumeData();
    }

    public Context getContext() {
        return mContext;
    }


    public static void initApp() {
        //downLoadKeys();
        downLoadParam();
    }

    /**
     * 获取秘钥的状态，如果状态为false说明没有秘钥，需要重新去下载秘钥。
     * 如果返回true，说明有秘钥。可以直接交易
     */


    private static int getAIDSize() {
        int size = 0;

        size = new AidDaoImpl(mContext).findAllAid().size();

        return size;
    }

    private static void downLoadParam() {

        EmvManager mPbocManager = EmvManager.getInstance();

        //读取assert下的IC卡参数配置文件，将相关参数加载到EMV内核
        try {
//            if (getAIDSize() == 0) {
            boolean updateResult = false;
            boolean flag = true;
            int i = 0;
            String success = "";
            String fail = "";
            // 获取IC卡参数信息
            mPbocManager.updateAID(0x03, null);
            mPbocManager.updateCAPK(0x03, null);

            InputStream ins = mContext.getAssets().open("icparam/ic_param_20211101.txt");
            if (ins != null && ins.available() != 0x00) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(ins));
                String line;
                while ((line = br.readLine()) != null) {
                    // 未到达文件末尾
                    if (line.startsWith("AID")) {
                        Log.d(TAG,"downLoadParam: AID= %s"+ line.split("=")[1]);

                        // 更新AID
                        updateResult = mPbocManager.updateAID(0x01, line.split("=")[1]);

                    } else { // 更新RID
                        updateResult = mPbocManager.updateCAPK(0x01, line.split("=")[1]);
                        Log.d(TAG,"downLoadParam: CAPK= " + line.split("=")[1]);

                    }
                }
                Log.d(TAG,"downLoadParam: completed");
            }
            //}
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static int downLoadKeys() {
        int result = -1;
        final AidlPinpad pinpadManager = DeviceTopUsdkServiceManager.Companion.getInstance().getPinpadManager(0);

        try {
            boolean rootFlag = pinpadManager.getKeyState(PinpadConstant.KeyType.KEYTYPE_ROOT, 0);

            if (!rootFlag) {
                result = -1;
            }
            boolean pekFlag = pinpadManager.getKeyState(PinpadConstant.KeyType.KEYTYPE_PEK, 0);
            boolean makFlag = pinpadManager.getKeyState(PinpadConstant.KeyType.KEYTYPE_MAK, 0);
            if (pekFlag && makFlag) {
                Log.v("MainPageActivity","已经有秘钥了");
                result = 0;

            } else {
                Log.v("MainPageActivity", "没有秘钥");
                final byte[] tmk = BCDASCII.hexStringToBytes("89F8B0FDA2F2896B9801F131D32F986D89F8B0FDA2F2896B");
                final byte[] tak = BCDASCII.hexStringToBytes("92B1754D6634EB22");
                final byte[] tpk = BCDASCII.hexStringToBytes("B5E175AC5FD8DD8A03AD23A35C5BAB6B");
                final byte[] trk = BCDASCII.hexStringToBytes("744185122EEC284830694CAD383B4F7A");
                boolean mIsSuccess = false;

                try {
                    mIsSuccess = pinpadManager.loadMainkey(0, tmk, null);

                    mIsSuccess = pinpadManager.loadWorkKey(PinpadConstant.WKeyType.WKEY_TYPE_MAK, 0, 0, tak, null);

                    mIsSuccess = pinpadManager.loadWorkKey(PinpadConstant.WKeyType.WKEY_TYPE_PIK, 0, 0, tpk, null);

                    mIsSuccess = pinpadManager.loadWorkKey(PinpadConstant.WKeyType.WKEY_TYPE_TDK, 0, 0, trk, null);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                result = 1;
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return result;
    }
}

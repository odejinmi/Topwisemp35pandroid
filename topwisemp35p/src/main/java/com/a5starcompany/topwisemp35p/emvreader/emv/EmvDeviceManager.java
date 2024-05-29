package com.a5starcompany.topwisemp35p.emvreader.emv;

import android.os.IBinder;
import android.os.RemoteException;

import com.topwise.cloudpos.aidl.AidlDeviceService;
import com.topwise.cloudpos.aidl.card.AidlCheckCard;
import com.topwise.cloudpos.aidl.emv.level2.AidlAmex;
import com.topwise.cloudpos.aidl.emv.level2.AidlEmvL2;
import com.topwise.cloudpos.aidl.emv.level2.AidlEntry;
import com.topwise.cloudpos.aidl.emv.level2.AidlPaypass;
import com.topwise.cloudpos.aidl.emv.level2.AidlPaywave;
import com.topwise.cloudpos.aidl.emv.level2.AidlPure;
import com.topwise.cloudpos.aidl.emv.level2.AidlQpboc;
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad;

/**
 * @author xukun
 * @version 1.0.0
 * @date 18-6-8
 * <p>
 * All Device mode manager ,include Printer ,Pinpad ,
 * IC RF Magnetic card ,Beep.before get the mode handle ,should
 * bind usdk service first .
 */

public class EmvDeviceManager {


    private static EmvDeviceManager instance;
    private static AidlDeviceService mDeviceService;

    private EmvDeviceManager() {
    }

    public void init(AidlDeviceService deviceService) {
        mDeviceService = deviceService;
    }

    public static EmvDeviceManager getInstance() {
        if (null == instance) {
            instance = new EmvDeviceManager();
        }
        return instance;
    }

    public IBinder getPinPad() {
        try {
            if (mDeviceService != null) {
                return mDeviceService.getPinPad(0);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlPinpad getPinpadManager() {

        AidlPinpad aidlPinpad = AidlPinpad.Stub.asInterface(getPinPad());
        return aidlPinpad;
    }


    public IBinder getCheckCard() {
        try {
            if (mDeviceService != null) {
                return mDeviceService.getCheckCard();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlCheckCard getDetectCardManager() {

        AidlCheckCard aidlCheckCard = AidlCheckCard.Stub.asInterface(getCheckCard());
        return aidlCheckCard;
    }

    public AidlEmvL2 getEmvL2() {
        try {
            if (mDeviceService != null) {
                return AidlEmvL2.Stub.asInterface(mDeviceService.getL2Emv());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlPure getL2Pure() {
        try {
            if (mDeviceService != null) {
                return AidlPure.Stub.asInterface(mDeviceService.getL2Pure());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlPaypass getL2Paypass() {
        try {
            if (mDeviceService != null) {
                return AidlPaypass.Stub.asInterface(mDeviceService.getL2Paypass());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlPaywave getL2Paywave() {
        try {
            if (mDeviceService != null) {
                return AidlPaywave.Stub.asInterface(mDeviceService.getL2Paywave());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlEntry getL2Entry() {
        try {
            if (mDeviceService != null) {
                return AidlEntry.Stub.asInterface(mDeviceService.getL2Entry());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlAmex getL2Amex() {
        try {
            if (mDeviceService != null) {
                return AidlAmex.Stub.asInterface(mDeviceService.getL2Amex());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AidlQpboc getL2Qpboc() {
        try {
            if (mDeviceService != null) {
                return AidlQpboc.Stub.asInterface(mDeviceService.getL2Qpboc());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}

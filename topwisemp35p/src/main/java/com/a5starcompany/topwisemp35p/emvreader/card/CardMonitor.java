package com.a5starcompany.topwisemp35p.emvreader.card;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.fragment.app.FragmentTransaction;

import com.a5starcompany.topwisemp35p.emvreader.DeviceTopUsdkServiceManager;
import com.topwise.cloudpos.aidl.card.AidlCheckCard;


public class CardMonitor {
    private static final String TAG = "CardMonitor";
    private static final int SEARCH_CARD_TIME = 30000;
    private final AidlCheckCard mCheckCard;
    private final Context mContext;
    private  FragmentTransaction msupportFragmentManager;

    public CardMonitor(Context context) {
        mCheckCard = DeviceTopUsdkServiceManager.getInstance().getCheckCard();
        mContext = context;
    }

    public CardMonitor(Context context, FragmentTransaction supportFragmentManager) {
        mCheckCard = DeviceTopUsdkServiceManager.getInstance().getCheckCard();
        mContext = context;
        msupportFragmentManager = supportFragmentManager;
    }

    public void startMonitoring(boolean isMag, boolean isIc, boolean isRf) {

        Log.d("TAG", "onCreate:  i am waint here monitoring") ;
        Log.i(TAG,"startMonitoring");
        synchronized (this) {
            try {
                if (mCheckCard != null)
                    mCheckCard.checkCard(isMag, isIc, isRf, SEARCH_CARD_TIME, new CheckCardListenerSub(mContext,msupportFragmentManager));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopMonitoring() {
        Log.i(TAG, "stopMonitoring");
        synchronized (this) {
            try {
                if (mCheckCard != null) {
                    mCheckCard.cancelCheckCard();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}

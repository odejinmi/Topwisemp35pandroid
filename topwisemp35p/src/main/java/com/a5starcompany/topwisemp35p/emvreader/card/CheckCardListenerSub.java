package com.a5starcompany.topwisemp35p.emvreader.card;


import static com.a5starcompany.topwisemp35p.emvreader.util.CardSearchErrorUtil.CARD_SEARCH_ERROR_REASON_MAG_EMV;
import static com.a5starcompany.topwisemp35p.emvreader.util.CardSearchErrorUtil.CARD_SEARCH_ERROR_REASON_MAG_READ;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.fragment.app.FragmentTransaction;

import com.a5starcompany.topwisemp35p.emvreader.cache.ConsumeData;
import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil;
import com.a5starcompany.topwisemp35p.emvreader.DeviceTopUsdkServiceManager;
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvManager;
import com.topwise.cloudpos.aidl.card.AidlCheckCard;
import com.topwise.cloudpos.aidl.card.AidlCheckCardListener;
import com.topwise.cloudpos.aidl.magcard.TrackData;
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvTransData;



public class CheckCardListenerSub extends AidlCheckCardListener.Stub {
    private static final String TAG = StringUtil.TAGPUBLIC + CheckCardListenerSub.class.getSimpleName();

    private final Context mContext;
    private FragmentTransaction msupportFragmentManager;
    private final EmvManager mPbocManager;
    private EmvTransData mEmvTransData;
    private final AidlCheckCard mCheckCard = DeviceTopUsdkServiceManager.Companion.getInstance().getCheckCard();

    public CheckCardListenerSub(Context context) {
        mPbocManager = EmvManager.getInstance();
        mContext = context;
    }

    public CheckCardListenerSub(Context context, FragmentTransaction supportFragmentManager) {
        mPbocManager = EmvManager.getInstance();
        mContext = context;
        msupportFragmentManager = supportFragmentManager;
    }

    @Override
    public void onFindMagCard(TrackData data) {
        Log.i(TAG,"onFindMagCard()");

        String cardNo = data.getCardno();
        String track2 = data.getSecondTrackData();
        String track3 = data.getThirdTrackData();

        Log.d(TAG,"onFindMagCard cardNo : " + cardNo + " track2 : " + track2);
        if (cardNo == null || isTrack2Error(track2)) {
            cancelCheckCard();
            CardManager.Companion.getInstance().callBackError(CARD_SEARCH_ERROR_REASON_MAG_READ);
        } else if (isEmvCard(track2)) {
            cancelCheckCard();
            CardManager.Companion.getInstance().callBackError(CARD_SEARCH_ERROR_REASON_MAG_EMV);
        } else {
            PosApplication.getApp().mConsumeData.setCardType(ConsumeData.CARD_TYPE_MAG);
            PosApplication.getApp().mConsumeData.setCardno(cardNo);
            PosApplication.getApp().mConsumeData.setExpiryData(data.getExpiryDate());
            track2 = track2.replace("=", "D");
            PosApplication.getApp().mConsumeData.setSecondTrackData(track2);
            if (track3 != null) {
                track3 = track3.replace("=", "D");
                PosApplication.getApp().mConsumeData.setThirdTrackData(track3);
            }


            //CardManager.getInstance().startActivity(mContext, null, CardConfirmActivity.class);
        }
    }

    @Override
    public void onSwipeCardFail() {
        Log.i(TAG,"onSwipeCardFail()");
        cancelCheckCard();
        CardManager.Companion.getInstance().callBackError(CARD_SEARCH_ERROR_REASON_MAG_READ);
    }

    @Override
    public void onFindICCard() {
        Log.i(TAG,"onFindICCard()");

        EmvTransDataSub emvTransDataSub = new EmvTransDataSub();
        mEmvTransData = emvTransDataSub.getEmvTransData(true);
        mPbocManager.startEmvProcess(mEmvTransData, new ICPbocStartListenerSub(mContext,msupportFragmentManager));
    }

    @Override
    public void onFindRFCard() {
        Log.i(TAG,"onFindRFCard()");

        PosApplication.getApp().mConsumeData.setCardType(ConsumeData.CARD_TYPE_RF);
        EmvTransDataSub emvTransDataSub = new EmvTransDataSub();
        mEmvTransData = emvTransDataSub.getEmvTransData(false);
        //mPbocManager.startEmvProcess(mEmvTransData, new RFPbocStartListenerSub(mContext));
    }

    @Override
    public void onError(int errorCode) {
        Log.i(TAG,"onError(), errorCode: " + errorCode);
        cancelCheckCard();
        CardManager.Companion.getInstance().callBackError(errorCode);
    }

    @Override
    public void onTimeout() {
        Log.i(TAG,"onTimeout()");
        CardManager.Companion.getInstance().callBackTimeOut();

    }

    @Override
    public void onCanceled() {
        Log.i(TAG,"onCanceled()");
        CardManager.Companion.getInstance().callBackCanceled();
    }

    private boolean isTrack2Error(String track2) {
        Log.i(TAG,"isTrack2Error = " + track2);
        //Log.i(TAG, "isTrack2Error length = " + track2.length());
        return track2 == null ||
                track2.length() < 21 ||
                track2.length() > 37 ||
                track2.indexOf("=") < 12;
    }

    private boolean isEmvCard(String track2) {
        Log.i(TAG,"isEmvCard: track2: " + track2);
        if ((track2 != null) && (track2.length() > 0)) {
            int index = track2.indexOf("=");
            String subTrack2 = track2.substring(index);

            if (subTrack2.charAt(5) == '2' || subTrack2.charAt(5) == '6') {
                Log.i(TAG,"isEmvCard: true");
                return true;
            }
        }
        Log.i(TAG,"isEmvCard: false");
        return false;
    }

    private void cancelCheckCard() {
        Log.i(TAG,"cancelCheckCard()");
        try {
            assert mCheckCard != null;
            mCheckCard.cancelCheckCard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
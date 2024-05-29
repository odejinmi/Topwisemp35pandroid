package com.a5starcompany.topwisemp35p.emvreader.card;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.a5starcompany.topwisemp35p.emvreader.util.StringUtil;


public class CardMoniterService extends Service {
    private final String TAG = StringUtil.TAGPUBLIC + CardMoniterService.class.getSimpleName();

    private CardMonitor cardMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate()");
        cardMonitor = new CardMonitor(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        cardMonitor.startMonitoring(true, true, true);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        cardMonitor.stopMonitoring();
    }
}



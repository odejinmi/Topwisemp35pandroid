package com.a5starcompany.topwisemp35p.emvreader.util;

import android.util.Log;

public class SDKLog {


    private static final boolean DEBUG = true;


    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

}

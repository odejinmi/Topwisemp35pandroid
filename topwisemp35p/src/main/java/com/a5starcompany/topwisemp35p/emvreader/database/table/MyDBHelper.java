package com.a5starcompany.topwisemp35p.emvreader.database.table;

import android.content.Context;

import com.a5starcompany.topwisemp35p.emvreader.database.DBHelper;


public class MyDBHelper extends DBHelper {
    public static final String DBNAME = "EmvParam.db";
    public static final int DBVERSION = 1;

    private static final Class<?>[] clazz = {Aid.class, Capk.class};

    public MyDBHelper(Context context) {
        super(context, DBNAME, null, DBVERSION, clazz);
    }

}

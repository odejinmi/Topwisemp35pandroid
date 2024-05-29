package com.a5starcompany.topwisemp35p.emvreader

import android.app.Application
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication

class PosApp : Application(){

    override fun onCreate() {
        super.onCreate()
        PosApplication.init(this@PosApp)
        PosApplication.initApp()
        PosApplication.getApp().setConsumeData()
    }

    override fun onTerminate() {
        super.onTerminate()
//        PosApplication.cancelCheckCard()
    }
}
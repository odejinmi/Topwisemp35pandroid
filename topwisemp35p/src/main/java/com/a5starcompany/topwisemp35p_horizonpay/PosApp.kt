package com.a5starcompany.topwisemp35p_horizonpay

import android.app.Application
import android.os.IBinder.DeathRecipient
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.horizonpay.DeviceHelper
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.horizonpay.smartpossdk.PosAidlDeviceServiceUtil
import com.horizonpay.smartpossdk.PosAidlDeviceServiceUtil.DeviceServiceListen
import com.horizonpay.smartpossdk.aidl.IAidlDevice
import com.horizonpay.utils.BaseUtils

open class PosApp : Application(){

    var INSTANCE: PosApp? = null

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        BaseUtils.init(this)
        bindDriverService()
        PosApplication.init(this)
        PosApplication.initApp()
        PosApplication.getApp().setConsumeData()
    }

    override fun onTerminate() {
        super.onTerminate()
        PosApplication.cancelCheckCard()
    }

    val TAG: String = "MyApplication"
    private var device: IAidlDevice? = null


    fun getDevice(): IAidlDevice? {
        return device
    }


    fun bindDriverService() {
        PosAidlDeviceServiceUtil.connectDeviceService(this, object : DeviceServiceListen {
            override fun onConnected(device: IAidlDevice?) {
                println("how are we doing here on connected")
                this@PosApp.device = device
                try {
                    DeviceHelper.reset()
                    DeviceHelper.initDevices(this@PosApp)
                    this@PosApp.device?.asBinder()?.linkToDeath(object : DeathRecipient {
                        override fun binderDied() {
                            Log.d(TAG, "binderDied deviceing")

                            if (this@PosApp.device == null) {
                                Log.d(TAG, "binderDied device is null")
                                return
                            }

                            this@PosApp.device!!.asBinder().unlinkToDeath(this, 0)
                            this@PosApp.device = null

                            //reBind driver Service
                            bindDriverService()
                        }
                    }, 0)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun error(errorcode: Int) {

                println("how are we doing here error "+errorcode)
            }

            override fun onDisconnected() {
                println("how are we doing here ondisconnected")
            }

            override fun onUnCompatibleDevice() {
                println("how are we doing here on uncompatible Device")
            }
        })
    }
}
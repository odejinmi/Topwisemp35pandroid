package com.a5starcompany.horizonpay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.horizonpay.smartpossdk.PosAidlDeviceServiceUtil
import com.horizonpay.smartpossdk.aidl.IAidlDevice
import com.horizonpay.smartpossdk.aidl.sys.IAidlSys
import java.lang.Exception

class DeviceHorizonsdkServiceManager {

    private var mContext: Context? = null
    var deviceService:  IAidlDevice? = null
        private set
    private var mBindResult = false

    private fun bindDeviceService(): Boolean {
        Log.i("topwise", "")
        val intent = Intent()
        intent.action = ACTION_DEVICE_SERVICE
        intent.setClassName(DEVICE_SERVICE_PACKAGE_NAME, DEVICE_SERVICE_CLASS_NAME)
        try {
            val bindResult = mContext!!.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            Log.i("topwise", "bindResult = $bindResult")
            return bindResult
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            deviceService = PosAidlDeviceServiceUtil.getAidlDevice(mContext)
            Log.i("topwise", "onServiceConnected  :  " + deviceService)
//            EmvDeviceManager.getInstance().init(deviceService)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i("topwise", "onServiceDisconnected  :  " + deviceService)
            deviceService = null
        }
    }
    val systemService: IBinder?
        get() {
            try {
                if (deviceService != null) {
                    return deviceService!!.asBinder()
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }
    val systemManager: IAidlSys
        get() = IAidlDevice.Stub.asInterface(systemService).getSysHandler()

    companion object {
        private const val DEVICE_SERVICE_PACKAGE_NAME = "com.android.topwise.topusdkservice"
        private const val DEVICE_SERVICE_CLASS_NAME =
            "com.android.topwise.topusdkservice.service.DeviceService"
        private const val ACTION_DEVICE_SERVICE = "topwise_cloudpos_device_service"
        private var mDeviceServiceManager: DeviceHorizonsdkServiceManager? = null

        fun getmDeviceServiceManager() {
            synchronized(DeviceHorizonsdkServiceManager::class.java) {
                mDeviceServiceManager = DeviceHorizonsdkServiceManager()
                Log.d("topwise", "gz mDeviceServiceManager: " + mDeviceServiceManager)
                mDeviceServiceManager!!.mContext = PosApplication.getApp().context
                mDeviceServiceManager!!.mBindResult = mDeviceServiceManager!!.bindDeviceService()
            }
        }

        @JvmStatic
        val instance: DeviceHorizonsdkServiceManager?
            get() {
                Log.d("topwise", "mDeviceServiceManager: " + mDeviceServiceManager)
                if (null == mDeviceServiceManager) {
                    synchronized(DeviceHorizonsdkServiceManager::class.java) {
                        if (null == mDeviceServiceManager) {
                            getmDeviceServiceManager()
                        }
                    }
                }
                return mDeviceServiceManager
            }
    }
}
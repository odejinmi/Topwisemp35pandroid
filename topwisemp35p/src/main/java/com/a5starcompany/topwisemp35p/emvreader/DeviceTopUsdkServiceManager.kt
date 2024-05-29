package com.a5starcompany.topwisemp35p.emvreader

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.a5starcompany.topwisemp35p.emvreader.app.PosApplication
import com.topwise.cloudpos.aidl.AidlDeviceService
import com.topwise.cloudpos.aidl.camera.AidlCameraScanCode
import com.topwise.cloudpos.aidl.card.AidlCheckCard
import com.topwise.cloudpos.aidl.emv.level2.*
import com.topwise.cloudpos.aidl.iccard.AidlICCard
import com.topwise.cloudpos.aidl.led.AidlLed
import com.topwise.cloudpos.aidl.pinpad.AidlPinpad
import com.topwise.cloudpos.aidl.printer.AidlPrinter
import com.topwise.cloudpos.aidl.printer.*
import com.topwise.cloudpos.aidl.rfcard.AidlRFCard
import com.topwise.cloudpos.aidl.shellmonitor.AidlShellMonitor
import com.topwise.cloudpos.aidl.system.AidlSystem
import com.a5starcompany.topwisemp35p.emvreader.emv.EmvDeviceManager
import java.lang.Exception

/**
 * @author xukun
 * @version 1.0.0
 * @date 18-6-8
 */
class DeviceTopUsdkServiceManager {
    private var mContext: Context? = null
    var deviceService: AidlDeviceService? = null
        private set
    private var mBindResult = false
    private fun bindDeviceService(): Boolean {
        Log.i("topwise","")
        val intent = Intent()
        intent.action = ACTION_DEVICE_SERVICE
        intent.setClassName(DEVICE_SERVICE_PACKAGE_NAME, DEVICE_SERVICE_CLASS_NAME)
        try {
            val bindResult = mContext!!.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            Log.i("topwise","bindResult = "+ bindResult)
            return bindResult
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun unBindDeviceService() {
        Log.i("topwise","")
        try {
            mContext!!.unbindService(mConnection)
        } catch (e: Exception) {
            Log.i("topwise","unbind DeviceService service failed : " + e)
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            deviceService = AidlDeviceService.Stub.asInterface(service)
            Log.i("topwise","onServiceConnected  : "+ deviceService)
            EmvDeviceManager.getInstance().init(deviceService)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i("topwise","onServiceDisconnected  :  "+ deviceService)
            deviceService = null
        }
    }
    val systemService: IBinder?
        get() {
            try {
                if (deviceService != null) {
                    return deviceService!!.systemService
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }
    val systemManager: AidlSystem
        get() = AidlSystem.Stub.asInterface(systemService)

    fun getPinPad(devid: Int): IBinder? {
        try {
            if (deviceService != null) {
                return deviceService!!.getPinPad(devid)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getPinpadManager(type: Int): AidlPinpad {
        return AidlPinpad.Stub.asInterface(getPinPad(type))
    }

    val ledManager: AidlLed
        get() = AidlLed.Stub.asInterface(led)
    val led: IBinder?
        get() {
            try {
                if (deviceService != null) {
                    return deviceService!!.led
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }
    val printer: IBinder?
        get() {
            try {
                if (deviceService != null) {
                    return deviceService!!.printer
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }
    val printManager: AidlPrinter?
        get() = AidlPrinter.Stub.asInterface(printer)

    //    public IBinder getEMVL2() {
    //        try {
    //            if (mDeviceService != null) {
    //                return mDeviceService.getEMVL2();
    //            }
    //        } catch (RemoteException e) {
    //            e.printStackTrace();
    //        }
    //        return null;
    //    }
    //
    //    public AidlPboc getPbocManager() {
    //
    //        AidlPboc aidlPboc = AidlPboc.Stub.asInterface(getEMVL2());
    //        return aidlPboc;
    //    }
    val rFCard: AidlRFCard
        get() = AidlRFCard.Stub.asInterface(getRFIDReader())

    fun getRFIDReader(): IBinder? {
        try {
            if (deviceService != null) {
                return deviceService!!.rfidReader
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getPSAMReader(devid: Int): IBinder? {
        try {
            if (deviceService != null) {
                return deviceService!!.getPSAMReader(devid)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getSerialPort(port: Int): IBinder? {
        try {
            if (deviceService != null) {
                return deviceService!!.getSerialPort(port)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getShellMonitor(): AidlShellMonitor? {
        try {
            if (deviceService != null) {
                return AidlShellMonitor.Stub.asInterface(deviceService!!.shellMonitor)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getCPUCard(): IBinder? {
        try {
            if (deviceService != null) {
                return deviceService!!.cpuCard
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getPedestal(): IBinder? {
        try {
            if (deviceService != null) {
                return deviceService!!.pedestal
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getICCardMoniter(): AidlICCard? {
        try {
            if (deviceService != null) {
                return AidlICCard.Stub.asInterface(deviceService!!.insertCardReader)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getRFCardMoniter(): AidlRFCard? {
        try {
            if (deviceService != null) {
                return AidlRFCard.Stub.asInterface(deviceService!!.rfidReader)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getCheckCard(): AidlCheckCard? {
        try {
            if (deviceService != null) {
                return AidlCheckCard.Stub.asInterface(deviceService!!.checkCard)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getScanAidl(): AidlCameraScanCode? {
        try {
            if (deviceService != null) {
                return AidlCameraScanCode.Stub.asInterface(deviceService!!.cameraManager)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    //AidlEmvL2
    fun getEmvL2(): AidlEmvL2? {
        try {
            if (deviceService != null) {
                return AidlEmvL2.Stub.asInterface(deviceService!!.l2Emv)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getL2Pure(): AidlPure? {
        try {
            if (deviceService != null) {
                return AidlPure.Stub.asInterface(deviceService!!.l2Pure)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getL2Paypass(): AidlPaypass? {
        try {
            if (deviceService != null) {
                return AidlPaypass.Stub.asInterface(deviceService!!.l2Paypass)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getL2Paywave(): AidlPaywave? {
        try {
            if (deviceService != null) {
                return AidlPaywave.Stub.asInterface(deviceService!!.l2Paywave)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getL2Entry(): AidlEntry? {
        try {
            if (deviceService != null) {
                return AidlEntry.Stub.asInterface(deviceService!!.l2Entry)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getL2Amex(): AidlAmex? {
        try {
            if (deviceService != null) {
                return AidlAmex.Stub.asInterface(deviceService!!.l2Amex)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    fun getL2Qpboc(): AidlQpboc? {
        try {
            if (deviceService != null) {
                return AidlQpboc.Stub.asInterface(deviceService!!.l2Qpboc)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        private const val DEVICE_SERVICE_PACKAGE_NAME = "com.android.topwise.topusdkservice"
        private const val DEVICE_SERVICE_CLASS_NAME =
            "com.android.topwise.topusdkservice.service.DeviceService"
        private const val ACTION_DEVICE_SERVICE = "topwise_cloudpos_device_service"
        @SuppressLint("StaticFieldLeak")
        private var mDeviceServiceManager: DeviceTopUsdkServiceManager? = null
        fun getmDeviceServiceManager() {
            synchronized(DeviceTopUsdkServiceManager::class.java) {
                mDeviceServiceManager = DeviceTopUsdkServiceManager()
                Log.d("topwise","gz mDeviceServiceManager: "+ mDeviceServiceManager)
                mDeviceServiceManager!!.mContext = PosApplication.getApp().context
                mDeviceServiceManager!!.mBindResult = mDeviceServiceManager!!.bindDeviceService()
            }
        }

        @JvmStatic
        val instance: DeviceTopUsdkServiceManager?
            get() {
                Log.d("topwise","mDeviceServiceManager: "+ mDeviceServiceManager)
                if (null == mDeviceServiceManager) {
                    synchronized(DeviceTopUsdkServiceManager::class.java) {
                        if (null == mDeviceServiceManager) {
                            getmDeviceServiceManager()
                        }
                    }
                }
                return mDeviceServiceManager
            }
    }
}
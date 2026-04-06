package com.a5starcompany.horizonpay

import android.annotation.SuppressLint
import android.os.RemoteException
import com.a5starcompany.topwisemp35p_horizonpay.PosApp
import com.horizonpay.smartpossdk.aidl.IAidlDevice
import com.horizonpay.smartpossdk.aidl.beeper.IAidlBeeper
import com.horizonpay.smartpossdk.aidl.camera.IAidlCamera
import com.horizonpay.smartpossdk.aidl.cardreader.IAidlCardReader
import com.horizonpay.smartpossdk.aidl.cpucard.IAidlCpuCard
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2
import com.horizonpay.smartpossdk.aidl.felica.IAidlFelicaCard
import com.horizonpay.smartpossdk.aidl.led.IAidlLed
import com.horizonpay.smartpossdk.aidl.m0ccard.IAidlM0CCard
import com.horizonpay.smartpossdk.aidl.m0ev1card.IAidlM0Ev1Card
import com.horizonpay.smartpossdk.aidl.m1card.IAidlM1Card
import com.horizonpay.smartpossdk.aidl.pinpad.IAidlPinpad
import com.horizonpay.smartpossdk.aidl.printer.IAidlPrinter
import com.horizonpay.smartpossdk.aidl.psamcard.IAidlPsamCard
import com.horizonpay.smartpossdk.aidl.serialport.IAidlSerialPort
import com.horizonpay.smartpossdk.aidl.serialport.IAidlUsbSerialPort
import com.horizonpay.smartpossdk.aidl.subLcd.IAidSubLcd
import com.horizonpay.smartpossdk.aidl.sys.IAidlSys

object DeviceHelper {
    private val TAG: String = DeviceHelper::class.java.getName()

    private var pinpad: IAidlPinpad? = null
    private var sysHandle: IAidlSys? = null
    private var printer: IAidlPrinter? = null
    private var emvHandler: IAidlEmvL2? = null
    private var cardReader: IAidlCardReader? = null
    private var device: IAidlDevice? = null
    private var led: IAidlLed? = null
    private var beeper: IAidlBeeper? = null
    private var felicaCard: IAidlFelicaCard? = null
    private var camera: IAidlCamera? = null
    private var m1Card: IAidlM1Card? = null
    private var cpuCard: IAidlCpuCard? = null
    private var m0Ev1Card: IAidlM0Ev1Card? = null
    private var m0CCard: IAidlM0CCard? = null
    private var serialPort: IAidlSerialPort? = null

    private var application: PosApp? = null

    private var iAidSubLcd: IAidSubLcd? = null
    private var iAidlPsamCard: IAidlPsamCard? = null

    private var iAidlUsbSerialPort: IAidlUsbSerialPort? = null

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun initDevices(app: PosApp?) {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("android.intent.action.PACKAGE_ADDED");
//        filter.addDataScheme("package");

        application = app
        if (application == null) {
            return
        }
        device = application!!.getDevice()

        if (device != null) {
            try {
//                emvHandler = device.getNewEmvL2("com.horizonpay.sample");
                pinpad = device!!.getPinpad(false)
                sysHandle = device!!.sysHandler
                printer = device!!.printer
                emvHandler = device!!.emvL2

                cardReader = device!!.cardReader
                led = device!!.led
                beeper = device!!.beeper
                m1Card = device!!.m1Card
                camera = device!!.camera
                felicaCard = device!!.felicaCard
                cpuCard = device!!.cpuCard
                m0CCard = device!!.m0CCard
                m0Ev1Card = device!!.m0Ev1Card
                serialPort = device!!.serialPort
                iAidSubLcd = device!!.subLcd
                iAidlPsamCard = device!!.psamCard
                iAidlUsbSerialPort = device!!.usbSerialPort
            }catch (e: RemoteException) {
                e.printStackTrace()
                throw e
            }
        } else {
            application!!.bindDriverService()
            reset()
        }
    }

    fun getDevice(): IAidlDevice? {
        return application?.getDevice()
    }

    @Throws(RemoteException::class) fun checkState() {
        if (application == null) {
            throw RemoteException("Please restart the application.")
        }

        if (application!!.getDevice() == null) {
            application!!.bindDriverService()
            reset()
            throw RemoteException("Device service connection failed, please try again later.")
        }
    }


    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getPinpad(): IAidlPinpad? {
        if (pinpad == null) {
            checkState()
            try {
                return application?.getDevice()?.getPinpad(false)
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return pinpad
        }
    }


    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getCardReader(): IAidlCardReader? {
        if (cardReader == null) {
            checkState()
            try {
                return application?.getDevice()?.cardReader
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return cardReader
        }
    }


    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val cpuCardHandler: IAidlCpuCard?
        get() {
            if (cpuCard == null) {
                checkState()
                try {
                    return application?.getDevice()?.cpuCard
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return cpuCard
            }
        }

    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val m1CardHandler: IAidlM1Card?
        get() {
            if (m1Card == null) {
                checkState()
                try {
                    return application?.getDevice()?.m1Card
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return m1Card
            }
        }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getSysHandle(): IAidlSys? {
        if (sysHandle == null) {
            checkState()
            try {
                return application?.getDevice()?.getSysHandler()
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return sysHandle
        }
    }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getLed(): IAidlLed? {
        if (led == null) {
            checkState()
            try {
                return application?.getDevice()?.led
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return led
        }
    }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getPrinter(): IAidlPrinter? {
        if (printer == null) {
            checkState()
            try {
                return application?.getDevice()?.printer
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return printer
        }
    }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getBeeper(): IAidlBeeper? {
        if (beeper == null) {
            checkState()
            try {
                return application?.getDevice()?.beeper
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return beeper
        }
    }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getEmvHandler(): IAidlEmvL2? {
        if (emvHandler == null) {
            checkState()
            try {
                return application?.getDevice()?.emvL2
                //                return application.getDevice().getNewEmvL2("com.horizonpay.sample");
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return emvHandler
        }
    }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getCamera(): IAidlCamera? {
        if (camera == null) {
            checkState()
            try {
                return application?.getDevice()?.camera
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return camera
        }
    }

    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val m0CCardHandler: IAidlM0CCard?
        get() {
            if (m0CCard == null) {
                checkState()
                try {
                    return application?.getDevice()?.m0CCard
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return m0CCard
            }
        }

    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val m0Ev1CardHandler: IAidlM0Ev1Card?
        get() {
            if (m0Ev1Card == null) {
                checkState()
                try {
                    return application?.getDevice()?.m0Ev1Card
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return m0Ev1Card
            }
        }

    @SuppressLint("NewApi") @Throws(RemoteException::class) fun getSerialPort(): IAidlSerialPort? {
        if (serialPort == null) {
            checkState()
            try {
                return application?.getDevice()?.serialPort
            }catch (e: RemoteException) {
                throw RemoteException("PinPad service acquisition failed, please try again later.")
            }
        } else {
            return serialPort
        }
    }

    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val subLcd: IAidSubLcd?
        get() {
            if (iAidSubLcd == null) {
                checkState()
                try {
                    return application?.getDevice()?.subLcd
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return iAidSubLcd
            }
        }

    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val psamCard: IAidlPsamCard?
        get() {
            if (iAidSubLcd == null) {
                checkState()
                try {
                    return application?.getDevice()?.psamCard
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return iAidlPsamCard
            }
        }

    @get:Throws(RemoteException::class)
    @get:SuppressLint("NewApi")
    val usbSerialPort: IAidlUsbSerialPort?
        get() {
            if (iAidlUsbSerialPort == null) {
                checkState()
                try {
                    return application?.getDevice()?.usbSerialPort
                }catch (e: RemoteException) {
                    throw RemoteException("PinPad service acquisition failed, please try again later.")
                }
            } else {
                return iAidlUsbSerialPort
            }
        }

    fun beepPrompt() {
        Thread(object : Runnable {

            override fun run() {
                try {
                    getBeeper()?.beep(0, 1)
                }catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

    fun reset() {
        pinpad = null
        sysHandle = null
        printer = null
        emvHandler = null
        cardReader = null
        led = null
        beeper = null
        m1Card = null
        camera = null
        felicaCard = null
        cpuCard = null
        m0Ev1Card = null
        m0CCard = null
        iAidSubLcd = null
        iAidlPsamCard = null

    }


}

package com.a5starcompany.topwisemp35p_horizonpay

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.util.Log
import com.a5starcompany.horizonpay.HorizonPayDevice
import com.a5starcompany.topwisemp35p.emvreader.TopWiseDevice
import com.a5starcompany.topwisemp35p.emvreader.emv.TransactionMonitor
import com.a5starcompany.topwisemp35p.emvreader.printer.PrintTemplate
import java.text.NumberFormat
import java.util.Locale

/** Topwisemp35pHorizonpayPlugin */
class Topwisemp35pHorizonpay(val context: Context, callback: (TransactionMonitor) -> Unit) {

  var ishorizondevice: Boolean = false
  private val callback: (TransactionMonitor) -> Unit
  init {
    this.callback = callback
    ishorizondevice = Build.MANUFACTURER == "HorizonPay" && Build.BRAND =="SPRD" && Build.MODEL == "S60" && Build.DEVICE == "sl8541e_1h10_32b"

  }


  val serialnumber: String
    get() = if (ishorizondevice) horizonPayDevice.serialnumber else topWiseDevice.serialnumber


  val topWiseDevice by lazy {
    TopWiseDevice(context) {
      this.callback.invoke(it)
    }
  }


  val horizonPayDevice by lazy {
    HorizonPayDevice(context) {
      this.callback.invoke(it)
    }
  }


  /**
   * It is invoked when making transaction
   * @param arg is the data that was passed in from the flutter side to make payment
   */
  fun makePayment(amount: String) {
    if (ishorizondevice) {
      horizonPayDevice.readCard(amount)
    }else{
      topWiseDevice.readCard(amount)
    }
  }

  fun enterpin(directpin: String) {
    if (ishorizondevice) {
      horizonPayDevice.enterpin(directpin)
    }else {
      topWiseDevice.enterpin(directpin)
    }
  }

  fun cancelcardprocess() {

//        if (!(call.arguments is Map<*,*>)) {
//            result.error(ERROR_CODE_PAYMENT_INITIALIZATION, "Invalid input(s)", null)
//            return
//        }
//
//        val amount = call.argument<String>("amount")!!
    if (ishorizondevice) {
      horizonPayDevice.closeCardReader()

    }else {
      topWiseDevice.closeCardReader()
    }
  }

  fun getcardsheme(amount: String) {

    if (ishorizondevice){

    }else {
      topWiseDevice.getCardScheme(amount)
    }

  }

  fun printDoc(template: PrintTemplate) {
    if (ishorizondevice){
      horizonPayDevice.printDoc(template)
    }else {
      topWiseDevice.printDoc(template) //perform print operation
    }
  }


}
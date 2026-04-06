package com.a5starcompany.horizonpay.utils

import android.os.RemoteException
import android.util.Log
import com.a5starcompany.horizonpay.DeviceHelper
import com.google.gson.Gson
import com.horizonpay.smartpossdk.aidl.emv.AidEntity
import com.horizonpay.smartpossdk.aidl.emv.AidNewEntity
import com.horizonpay.smartpossdk.aidl.emv.CapkEntity
import com.horizonpay.smartpossdk.aidl.emv.EmvTags
import com.horizonpay.smartpossdk.aidl.emv.EmvTermConfig
import com.horizonpay.utils.BaseUtils
import com.horizonpay.utils.FormatUtils
import java.io.FileInputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmvUtil {
    private const val TAG = "EmvUtil"

    val arqcTLVTags = arrayOf(
        "9F26", "9F27", "9F10", "9F37", "9F36",
        "95", "9A", "9C", "9F02", "5F2A",
        "82", "9F1A", "9F33", "9F34", "9F35",
        "9F1E", "84", "9F09", "9F63"
    )

    val tags = arrayOf(
        "5F20", "5F30", "9F03", "9F26", "9F27",
        "9F10", "9F37", "9F36", "95", "9A",
        "9C", "9F02", "5F2A", "82", "9F1A",
        "9F03", "9F33", "9F34", "9F35", "9F1E",
        "84", "9F09", "9F41", "9F63", "5A",
        "4F", "5F24", "5F34", "5F28", "9F12",
        "50", "56", "57", "9F20", "9F6B"
    )

    fun getExampleARPCData(): ByteArray? {
        return HexUtil.hexStringToByte("910AF98D4B51B47634743030")
    }

    fun getInitTermConfig(): EmvTermConfig {
        return EmvTermConfig().apply {
            merchId = "123456789012345"
            termId = "12345678"
            merchName = "horizonpay"
            capability = "E0F8C8"
            extCapability = "E000F0A001"
            termType = 0x22
            countryCode = "0566"
            transCurrCode = "0566"
            transCurrExp = 2
            merchCateCode = "0000"
        }
    }

    fun getCurrentTime(format: String): String {
        val df = SimpleDateFormat(format, Locale.getDefault())
        return df.format(Date())
    }

    fun readPan(): String? {
        val pan = try {
            DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_IC_PAN)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }

        return when {
            pan.isNullOrEmpty() -> getPanFromTrack2()
            pan.endsWith("F") -> pan.substring(0, pan.length - 1)
            else -> pan
        }
    }

    fun readTrack2(): String? {
        val track2 = try {
            DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_IC_TRACK2DATA)
                ?: DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_IC_TRACK2DD)
                ?: (DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.M_TAG_IC_9F6B) )
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }

        return if (!track2.isNullOrEmpty() && track2.endsWith("F")) {
            track2.substring(0, track2.length - 1)
        } else {
            track2
        }
    }

    fun readCardExpiryDate(): String? {
        return try {
            val temp = DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_IC_APPEXPIREDATE)
            if (!temp.isNullOrEmpty() && temp.length == 6) {
                val format = SimpleDateFormat("yyMMdd", Locale.getDefault())
                val date = format.parse(temp)
                SimpleDateFormat("yyyy/MM/dd", Locale.US).format(date)
            } else {
                temp
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }

    fun readCardHolder(): String? {
        return try {
            var cardHolderName = DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_IC_CHNAME)
            if (cardHolderName.isNullOrEmpty()) {
                val track1 = DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_IC_TRACK1DATA)
                cardHolderName = getCardHolderFromTrack1(track1)
            }
            cardHolderName
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    private fun getCardHolderFromTrack1(track1: String?): String? {
        return track1?.takeIf { it.length > 20 }?.let {
            val idx = it.indexOf('^')
            val temp = it.substring(idx + 1)
            temp.substring(0, temp.indexOf('^'))
        }
    }

    fun getPanFromTrack2(): String? {
        return readTrack2()?.let { track2 ->
            track2.indexOfFirst { it == '=' || it == 'D' }
                .takeIf { it != -1 }
                ?.let { endIndex -> track2.substring(0, minOf(endIndex, 19)) }
        }
    }

    fun showEmvTransResult(): StringBuilder {
        val builder = StringBuilder()
        try {
            val tlv = DeviceHelper.getEmvHandler()?.getTlvByTags(tags)
            val tlvDataList = TlvDataList.fromBinary(tlv)
            Log.d(TAG, "ICC Data: \n$tlv")

            builder.append("---------------------------------------------------\n")
            builder.append("Trans Amount: ${CardUtil.getCurrencyName(
                DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_TM_CURCODE)?.substring(1)
            )} ${
                FormatUtils.formatAmount(DeviceHelper.getEmvHandler()?.getTagValue(EmvTags.EMV_TAG_TM_AUTHAMNTN), 3, ",", 2)}\n")
            builder.append("Card No: ${readPan()}\n")
            builder.append("Card Org: ${CardUtil.getCardTypeFromAid(tlvDataList.getTLV(EmvTags.EMV_TAG_IC_AID)
                ?.getValue())}\n")

            builder.append("Card ExpiryDate: ${readCardExpiryDate()}\n")
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_CHNAME)?.let {
                builder.append("Card Holder Name: ${it.gBKValue}\n")
            }
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_PANSN)?.let {
                builder.append("Card Sequence Number: ${it.getValue()}\n")
            }
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_SERVICECODE)?.let {
                builder.append("Card Service Code: ${it.getValue()}\n")
            }
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_ISSCOUNTRYCODE)?.let {
                builder.append("Card Issuer Country Code: ${it.getValue()}\n")
            }
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_APNAME)?.let {
                builder.append("App name: ${it.gBKValue}\n")
            }
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_APPLABEL)?.let {
                builder.append("App label : ${it.gBKValue}\n")
            }
            tlvDataList.getTLV(EmvTags.EMV_TAG_IC_TRACK1DATA)?.let {
                builder.append("Card Track 1: ${it.getValue()}\n")
            }

            builder.append("Card Track 2: ${readTrack2()}\n")
            builder.append("----------------------------\n")
            tags.forEach { tag ->
                builder.append("$tag=${tlvDataList.getTLV(tag)}\n")
            }
            builder.append("---------------------------------------------------\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return builder
    }

    fun getAidList(): List<AidEntity>? {
        Log.d(TAG, "getAidList: ")
        return readAssetsTxt("aid.json")?.let { jsonString ->
            Gson().fromJson(jsonString, Array<AidEntity>::class.java).toList().also { list ->
                list.forEach { Log.d(TAG, "getAidList: ${it.aid}") }
            }
        }
    }

    fun getNewAidList(): List<AidNewEntity>? {
        Log.d(TAG, "getAidList: ")
        return readAssetsTxt("new_aid.json")?.let { jsonString ->
            Gson().fromJson(jsonString, Array<AidNewEntity>::class.java).toList().also { list ->
                list.forEach { Log.d(TAG, "getAidList: ${it.aid}") }
            }
        }
    }

    fun getCapkList(): List<CapkEntity>? {
        return readAssetsTxt("capk.json")?.let { jsonString ->
            Gson().fromJson(jsonString, Array<CapkEntity>::class.java).toList()
        }
    }

    fun readAssetsTxt(fileName: String): String? {
        return try {
            BaseUtils.getApp().assets.open(fileName).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                String(buffer, Charsets.UTF_8)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun readFile(filePath: String): String? {
        return try {
            FileInputStream(filePath).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                String(buffer, Charsets.UTF_8)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

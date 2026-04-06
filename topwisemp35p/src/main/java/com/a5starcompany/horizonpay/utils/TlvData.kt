package com.a5starcompany.horizonpay.utils

import android.util.Log
import com.a5starcompany.horizonpay.utils.HexUtil.bytesToHexString
import com.a5starcompany.horizonpay.utils.HexUtil.hexStringToByte
import com.a5starcompany.horizonpay.utils.HexUtil.merge
import com.a5starcompany.horizonpay.utils.HexUtil.subByte
import com.horizonpay.utils.ConvertUtils
import java.io.UnsupportedEncodingException

class TlvData private constructor() {
    var rawData: ByteArray? = null
    private var tag: String? = null
    private var length = -1
    private var value: ByteArray? = null
    fun getTag(): String? {
        if (tag != null) {
            return tag
        }
        val tLen: Int = getTLength(this.rawData, 0)
        return bytesToHexString(
            subByte(
                this.rawData, 0, tLen
            )
        ).also { tag = it }
    }

    fun getLength(): Int {
        if (length > -1) {
            return length
        }
        val offset: Int = getTLength(this.rawData, 0)
        val l: Int = Companion.getLLength(this.rawData!!, offset)
        if (l == 1) {
            return this.rawData!![offset].toInt()
        }

        var afterLen = 0
        for (i in 1 until l) {
            afterLen = afterLen shl 8
            afterLen = afterLen or ((this.rawData!![offset + i]).toInt() and 0xff)
        }
        return afterLen.also { length = it }
    }

    val tLLength: Int
        get() {
            if (this.rawData == null) {
                return -1
            }
            return rawData!!.size - (this.bytesValue?.size ?: 0)
        }

    fun getValue(): String? {
        val result = this.bytesValue
        if (result == null) {
            return null
        }
        return bytesToHexString(result)
    }

    val byteValue: ByteArray?
        get() = this.bytesValue

    val gBKValue: String?
        get() {
            try {
                val result = this.bytesValue

                if (result == null) {
                    return null
                }
                return String(result, charset("GBK")).replace("\\u0000".toRegex(), "")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return null
        }

    val numberValue: String?
        get() {
            val num = getValue()
            if (num == null) {
                return null
            }
            return num.toLong().toString()
        }

    val gBKNumberValue: ByteArray?
        get() {
            try {
                val result = this.numberValue
                if (result == null) {
                    return null
                }
                return result.toByteArray(charset("GBK"))
            } catch (e: UnsupportedEncodingException) {
            }
            return null
        }

    val bCDValue: ByteArray?
        get() {
            val result: String? = this.gBKValue
            if (result == null) {
                return null
            }
            return hexStringToByte(result)
        }

    val bytesValue: ByteArray?
        get() {
            if (value != null) {
                return value
            }
            val l = getLength()
            return HexUtil.subBytes(this.rawData!!, rawData!!.size - l, l).also { value = it }
        }

    val isValid: Boolean
        get() = this.rawData != null

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }

        if (obj !is TlvData) {
            return false
        }

        if (this.rawData == null || obj.rawData == null) {
            return false
        }

        return rawData.contentEquals(obj.rawData)
    }

    override fun toString(): String {
        if (this.rawData == null) {
            return super.toString()
        }
        return bytesToHexString(this.rawData)!!
    }

    companion object {
        private const val TAG = "TlvData"

        fun fromRawData(
            tlData: ByteArray?,
            tlOffset: Int,
            vData: ByteArray?,
            vOffset: Int
        ): TlvData? {
            if (tlData == null || tlData.size == 0 || vData == null || vData.size == 0) {
                return null
            }
            val tLen: Int = getTLength(tlData, tlOffset)
            val lLen: Int = getLLength(tlData, tlOffset + tLen)
            val vLen: Int = calcValueLength(tlData, tlOffset + tLen, lLen)

            val d = TlvData()
            d.rawData = merge(subByte(tlData, tlOffset, tLen + lLen), subByte(vData, vOffset, vLen))
            d.getTag()
            d.getLength()
            d.bytesValue

            return d
        }

        fun fromData(tagName: String?, value: ByteArray?): TlvData? {
            if (tagName == null || tagName.length == 0 || value == null || value.size == 0) {
                return null
            }
            val tag = hexStringToByte(tagName)
            val d = TlvData()
            d.rawData = merge(tag, makeLengthData(value.size), value)
            d.tag = tagName
            d.length = value.size
            d.value = value
            return d
        }

        fun fromRawData(data: ByteArray?, offset: Int): TlvData? {
            if (data == null || data.size == 0) {
                return null
            }
            val len: Int = getDataLength(data, offset)
            val d = TlvData()
            d.rawData = subByte(data, offset, len)
            d.getTag()
            d.getLength()
            d.bytesValue
            return d
        }

        private fun getTLength(data: ByteArray?, offset: Int): Int {
            if (data == null || data.size == 0) {
                return -1
            }

            if ((data[offset].toInt() and 0x1F) == 0x1F) {
                return 2
            }
            return 1
        }

        private fun getLLength(data: ByteArray, offset: Int): Int {
            if ((data[offset].toInt() and 0x80) == 0) {
                return 1
            }
            return (data[offset].toInt() and 0x7F) + 1
        }

        private fun getDataLength(data: ByteArray, offset: Int): Int {
            val tLen: Int = getTLength(data, offset)
            val lLen: Int = getLLength(data, offset + tLen)
            val vLen: Int = calcValueLength(data, offset + tLen, lLen)
            return tLen + lLen + vLen
        }

        private fun calcValueLength(l: ByteArray, offset: Int, lLen: Int): Int {
            if (lLen == 1) {
                return l[offset].toInt()
            }
            Log.d(TAG, "calcValueLength: " + l.size)
            Log.d(TAG, "calcValueLength: " + offset)
            Log.d(TAG, "calcValueLength: " + lLen)
            Log.d(TAG, "calcValueLength: " + ConvertUtils.bytes2HexString(l))
            var vLen = 0
            for (i in 1 until lLen) {
                vLen = vLen shl 8
                vLen = vLen or ((l[offset + i]).toInt() and 0xff)
            }
            return vLen
        }

        private fun makeLengthData(len: Int): ByteArray? {
            if (len > 127) {
                var lenData: ByteArray? = ByteArray(4)
                var validIndex = -1
                for (i in lenData!!.indices) {
                    lenData[i] = ((len shr (8 * (3 - i))) and 0xFF).toByte()
                    if (lenData[i].toInt() != 0 && validIndex < 0) {
                        validIndex = i
                    }
                }

                lenData = HexUtil.subBytes(lenData, validIndex, -1)
                lenData = merge(byteArrayOf((0x80 or lenData.size).toByte()), lenData)
                return lenData
            } else {
                return byteArrayOf(len.toByte())
            }
        }
    }
}
package com.a5starcompany.horizonpay.utils

import com.a5starcompany.horizonpay.utils.HexUtil.bytesToHexString
import com.a5starcompany.horizonpay.utils.HexUtil.hexStringToByte
import com.a5starcompany.horizonpay.utils.HexUtil.merge
import java.util.Arrays

class TlvDataList {
    private val data: MutableList<TlvData> = ArrayList<TlvData>()
    fun size(): Int {
        return data.size
    }

    fun toBinary(): ByteArray? {
        val allData = arrayOfNulls<ByteArray>(data.size)
        for (i in data.indices) {
            allData[i] = data.get(i).rawData
        }
        return merge(*allData)
    }

    fun contains(tag: String?): Boolean {
        return null != getTLV(tag)
    }

    fun getTLV(tag: String?): TlvData? {
        for (d in data) {
            if (d.getTag() == tag) {
                return d
            }
        }
        return null
    }

    fun getTLV(tag: String?, defalutTlvData: TlvData?): TlvData? {
        for (d in data) {
            if (d.getTag() == tag) {
                return d
            }
        }
        return defalutTlvData
    }

    fun getTLVs(vararg tags: String?): TlvDataList? {
        val list = TlvDataList()
        for (tag in tags) {
            val data = getTLV(tag)
            if (data != null) {
                list.addTLV(data)
            }
        }
        if (list.size() == 0) {
            return null
        }
        return list
    }

    fun getTLV(index: Int): TlvData? {
        return data.get(index)
    }

    fun addTLV(tlv: TlvData) {
        if (tlv.isValid) {
            data.add(tlv)
        } else {
            throw IllegalArgumentException("tlv is not valid!")
        }
    }

    fun retainAll(vararg tags: String?) {
        val tagList = Arrays.asList<String?>(*tags)
        var i = 0
        while (i < data.size) {
            if (!tagList.contains(data.get(i).getTag())) {
                data.removeAt(i)
            } else {
                i++
            }
        }
    }

    fun remove(tag: String) {
        var i = 0
        while (i < data.size) {
            if (tag == data.get(i).getTag()) {
                data.removeAt(i)
            } else {
                i++
            }
        }
    }

    fun removeAll(vararg tags: String?) {
        val tagList = Arrays.asList<String?>(*tags)
        var i = 0
        while (i < data.size) {
            if (tagList.contains(data.get(i).getTag())) {
                data.removeAt(i)
            } else {
                i++
            }
        }
    }

    override fun toString(): String {
        if (data.isEmpty()) {
            return super.toString()
        }
        return bytesToHexString(toBinary())!!
    }

    companion object {
        private const val TAG = "TlvDataList"

        fun fromBinary(data: ByteArray): TlvDataList {
            val l = TlvDataList()
            var offset = 0
            while (offset < data.size) {
                val d: TlvData = TlvData.fromRawData(data, offset)!!
                l.addTLV(d)
                offset += d.rawData!!.size
            }
            return l
        }

        fun fromBinary(data: String?): TlvDataList {
            return fromBinary(hexStringToByte(data)!!)
        }
    }
}
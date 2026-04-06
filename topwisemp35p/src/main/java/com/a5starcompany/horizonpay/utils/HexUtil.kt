package com.a5starcompany.horizonpay.utils

import java.util.Locale

object HexUtil {
    private val DIGITS = charArrayOf(
        '0', '1', '2', '3', '4',
        '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    fun byteToHex(b: Byte): String {
        return ("" + "0123456789ABCDEF".get(0xf and (b.toInt() shr 4)) + "0123456789ABCDEF".get(b.toInt() and 0xf))
    }

    fun hexStringToByte(hex: String?): ByteArray? {
        var hex = hex
        if (hex == null || hex.length == 0) {
            return null
        }
        hex = hex.uppercase(Locale.getDefault())
        val len = (hex.length / 2)
        val result = ByteArray(len)
        val achar = hex.toCharArray()
        for (i in 0 until len) {
            val pos = i * 2
            result[i] =
                (toByte(achar[pos]).toInt() shl 4 or toByte(achar[pos + 1]).toInt()).toByte()
        }
        return result
    }

    fun asc_to_bcd(asc: Byte): Byte {
        val bcd: Byte

        if ((asc >= '0'.code.toByte()) && (asc <= '9'.code.toByte())) bcd =
            (asc - '0'.code.toByte()).toByte()
        else if ((asc >= 'A'.code.toByte()) && (asc <= 'F'.code.toByte())) bcd =
            (asc - 'A'.code.toByte() + 10).toByte()
        else if ((asc >= 'a'.code.toByte()) && (asc <= 'f'.code.toByte())) bcd =
            (asc - 'a'.code.toByte() + 10).toByte()
        else bcd = (asc - 48).toByte()
        return bcd
    }

    fun ASCII_To_BCD(ascii: ByteArray, asc_len: Int): ByteArray {
        val bcd = ByteArray(asc_len / 2)
        var j = 0
        for (i in 0 until (asc_len + 1) / 2) {
            bcd[i] = asc_to_bcd(ascii[j++])
            bcd[i] =
                ((if (j >= asc_len) 0x00 else asc_to_bcd(ascii[j++])) + (bcd[i].toInt() shl 4)).toByte()
        }
        return bcd
    }


    fun bcd2Str(bytes: ByteArray): String {
        val temp = StringBuffer(bytes.size * 2)
        for (i in bytes.indices) {
            temp.append(((bytes[i].toInt() and 0xf0) ushr 4).toByte().toInt())
            temp.append((bytes[i].toInt() and 0x0f).toByte().toInt())
        }
        return if (temp.toString().substring(0, 1).equals("0", ignoreCase = true)) temp.toString()
            .substring(1) else temp.toString()
    }

    fun bcd2str(bcds: ByteArray?): String? {
        if (null == bcds) {
            return null
        }
        val ascii = "0123456789abcdef".toCharArray()
        val temp = ByteArray(bcds.size * 2)
        for (i in bcds.indices) {
            temp[i * 2] = ((bcds[i].toInt() shr 4) and 0x0f).toByte()
            temp[i * 2 + 1] = (bcds[i].toInt() and 0x0f).toByte()
        }
        val res = StringBuffer()

        for (i in temp.indices) {
            res.append(ascii[temp[i].toInt()])
        }
        return res.toString().uppercase(Locale.getDefault())
    }


    fun hex2Byte(hex: String): Byte {
        val achar = hex.uppercase(Locale.getDefault()).toCharArray()
        val b = (toByte(achar[0]).toInt() shl 4 or toByte(achar[1]).toInt()).toByte()
        return b
    }

    private fun toByte(c: Char): Byte {
        val b = "0123456789ABCDEF".indexOf(c).toByte()
        return b
    }

    fun int2bytes(num: Int): ByteArray {
        val b = ByteArray(4)
        val mask = 0xff
        for (i in 0..3) {
            b[i] = (num ushr (24 - i * 8)).toByte()
        }
        return b
    }

    fun int2bytes(d: Int, outdata: ByteArray, offset: Int): Int {
        outdata[offset + 3] = ((d shr 24) and 0xff).toByte()
        outdata[offset + 2] = ((d shr 16) and 0xff).toByte()
        outdata[offset + 1] = ((d shr 8) and 0xff).toByte()
        outdata[offset + 0] = ((d shr 0) and 0xff).toByte()
        return offset + 4
    }

    fun bytes2int(b: ByteArray): Int {
        val mask = 0xff
        var temp = 0
        var res = 0
        for (i in 0..3) {
            res = res shl 8
            temp = b[i].toInt() and mask
            res = res or temp
        }
        return res
    }


    fun bytes2short(b: ByteArray): Int {
        val mask = 0xff
        var temp = 0
        var res = 0
        for (i in 0..1) {
            res = res shl 8
            temp = b[i].toInt() and mask
            res = res or temp
        }
        return res
    }

    fun getBinaryStrFromByteArr(bArr: ByteArray): String {
        var result = ""
        for (b in bArr) {
            result += getBinaryStrFromByte(b)
        }
        return result
    }


    fun getBinaryStrFromByte(b: Byte): String {
        var result = ""
        var a = b

        for (i in 0..7) {
            val c = a
            a = (a.toInt() shr 1).toByte()
            a = (a.toInt() shl 1).toByte()
            if (a == c) {
                result = "0" + result
            } else {
                result = "1" + result
            }
            a = (a.toInt() shr 1).toByte()
        }
        return result
    }

    fun getBinaryStrFromByte2(b: Byte): String {
        var result = ""
        var a = b

        for (i in 0..7) {
            result = (a % 2).toString() + result
            a = (a.toInt() shr 1).toByte()
        }
        return result
    }

    fun getBinaryStrFromByte3(b: Byte): String {
        var result = ""
        var a = b

        for (i in 0..7) {
            result = (a % 2).toString() + result
            a = (a / 2).toByte()
        }
        return result
    }


    fun toByteArray(iSource: Int, iArrayLen: Int): ByteArray {
        val bLocalArr = ByteArray(iArrayLen)
        var i = 0
        while ((i < 4) && (i < iArrayLen)) {
            bLocalArr[i] = (iSource shr 8 * i and 0xFF).toByte()

            i++
        }
        return bLocalArr
    }

    fun xor(op1: ByteArray, op2: ByteArray): ByteArray {
        require(op1.size == op2.size) { "Parameter error, parameter length is different" }
        val result = ByteArray(op1.size)
        for (i in op1.indices) {
            result[i] = (op1[i].toInt() xor op2[i].toInt()).toByte()
        }
        return result
    }

    fun xorBytes(op: ByteArray, start: Int, end: Int): Byte {
        var xorResult: Byte = 0x00
        var xorResultStr = ""
        if ((start >= 0) && (start < end) && (end > 1)) {
            for (i in start until end) {
                xorResult = (xorResult.toInt() xor op[i].toInt()).toByte()
                xorResultStr = xorResultStr + String.format("%02x ", xorResult)
            }
        }
        return xorResult
    }

    fun bytesToHexString(bArray: ByteArray?): String? {
        if (bArray == null || bArray.size == 0) {
            return null
        }
        val sb = StringBuffer(bArray.size)

        var sTemp: String?
        var j = 0
        for (i in bArray.indices) {
            sTemp = Integer.toHexString(0xFF and bArray[i].toInt())
            if (sTemp.length < 2) sb.append(0)

            sb.append(sTemp.uppercase(Locale.getDefault()))
            j++
        }
        return sb.toString()
    }

    fun str2HexStr(str: String?): String? {
        if (str == null || str.length == 0) {
            return null
        }
        val chars = "0123456789ABCDEF".toCharArray()
        val sb = StringBuilder("")
        val bs = str.toByteArray()
        var bit: Int

        for (i in bs.indices) {
            bit = (bs[i].toInt() and 0x0f0) shr 4
            sb.append(chars[bit])
            bit = bs[i].toInt() and 0x0f
            sb.append(chars[bit])
        }
        return sb.toString().trim { it <= ' ' }
    }

    fun subBytes(src: ByteArray, begin: Int, count: Int): ByteArray {
        val bs = ByteArray(count)
        System.arraycopy(src, begin, bs, 0, count)
        return bs
    }

    fun mergeBytes(bytesA: ByteArray?, bytesB: ByteArray?): ByteArray? {
        if (bytesA != null && bytesA.size != 0) {
            if (bytesB != null && bytesB.size != 0) {
                val bytes = ByteArray(bytesA.size + bytesB.size)
                System.arraycopy(bytesA, 0, bytes, 0, bytesA.size)
                System.arraycopy(bytesB, 0, bytes, bytesA.size, bytesB.size)
                return bytes
            } else {
                return bytesA
            }
        } else {
            return bytesB
        }
    }

    fun merge(vararg data: ByteArray?): ByteArray? {
        if (data == null) {
            return null
        } else {
            var bytes: ByteArray? = null

            for (i in data.indices) {
                bytes = mergeBytes(bytes, data[i])
            }

            return bytes
        }
    }

    fun subByte(srcBytes: ByteArray?, offset: Int, len: Int): ByteArray? {
        if (srcBytes == null) {
            return null
        } else if (len <= srcBytes.size && offset + len <= srcBytes.size && offset < srcBytes.size) {
            val bytes: ByteArray?
            if (len == -1) {
                bytes = ByteArray(srcBytes.size - offset)
                System.arraycopy(srcBytes, offset, bytes, 0, srcBytes.size - offset)
            } else {
                bytes = ByteArray(len)
                System.arraycopy(srcBytes, offset, bytes, 0, len)
            }

            return bytes
        } else {
            return null
        }
    }

    fun encode(i: Int): String {
        var i = i
        val cbuf = CharArray(8)
        var charPos = cbuf.size - 1
        do {
            cbuf[charPos--] = DIGITS[i and 0xF]
            i = i ushr 4
            cbuf[charPos--] = DIGITS[i and 0xF]
            i = i ushr 4
        } while (i != 0)
        return String(cbuf, charPos + 1, cbuf.size - charPos - 1)
    }

    fun encode(b: Byte): String {
        return encode(byteArrayOf(b))
    }

    fun encode(b: ByteArray): String {
        val hex = StringBuilder(b.size * 2)
        for (i in b.indices) {
            val hiNibble = b[i].toInt() shr 4 and 0xF
            val loNibble = b[i].toInt() and 0xF
            hex.append(DIGITS[hiNibble]).append(DIGITS[loNibble])
        }
        return hex.toString()
    }

    fun decode(hex: String): ByteArray {
        var hex = hex
        val len = hex.length
        if (len > 0) {
            hex = hex.uppercase(Locale.getDefault())
        }
        val r = ByteArray(len / 2)
        for (i in r.indices) {
            var digit1 = hex.get(i * 2).code
            var digit2 = hex.get(i * 2 + 1).code
            if (digit1 >= '0'.code && digit1 <= '9'.code) digit1 -= '0'.code
            else if (digit1 >= 'A'.code && digit1 <= 'F'.code) digit1 -= 'A'.code - 10
            if (digit2 >= '0'.code && digit2 <= '9'.code) digit2 -= '0'.code
            else if (digit2 >= 'A'.code && digit2 <= 'F'.code) digit2 -= 'A'.code - 10

            r[i] = ((digit1 shl 4) + digit2).toByte()
        }
        return r
    }

    /**
     * Move first 2 bytes to the last
     * e.g.
     * before:1122334455667788
     * after:2233445566778811
     *
     * @param rbuf
     * @return
     */
    fun insertFirst2Last(rbuf: ByteArray): ByteArray? {
        var temp: Byte
        val tmp: ByteArray? = ByteArray(8)

        for (i in 0..7) {
            temp = rbuf[0]
            for (j in 0..6) {
                tmp!![j] = rbuf[j + 1]
            }
            tmp!![7] = temp
        }
        return tmp
    }

    /**
     * Move last 2 bytes to the first.
     * e.g.
     * before:1122334455667788
     * after:8811223344556677
     *
     * @param rbuf
     * @return
     */
    fun insertLast2First(rbuf: ByteArray): ByteArray? {
        var temp: Char
        val tmp: ByteArray? = ByteArray(8)
        tmp!![0] = rbuf[7]
        for (j in 1..7) {
            tmp[j] = rbuf[j - 1]
        }
        return tmp
    }
}

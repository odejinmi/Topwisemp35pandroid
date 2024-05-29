package  com.a5starcompany.topwisemp35p.emvreader.util


import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.experimental.xor
import java.io.ByteArrayOutputStream
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

const val IPEK_LIVE = "3F2216D8297BCE9C"
const val KSN_LIVE = "0000000002DDDDE00001"
const val IPEK_TEST = "9F8011E7E71E483B"
const val KSN_TEST = "0000000006DDDDE01500"

object DukptHelper {
    fun getSessionKey(
        IPEK: String = IPEK_LIVE,
        KSN: String = KSN_LIVE
    ): String {
        var initialIPEK: String = IPEK
        //println("The expected value of the initial IPEK $initialIPEK");
        val ksn = KSN.padStart(20, '0')
        println("The expected value of the ksn $ksn")
        var sessionkey = ""
        //Get ksn with a zero counter by ANDing it with 0000FFFFFFFFFFE00000
        val newKSN = XORorANDorORfunction(ksn, "0000FFFFFFFFFFE00000", "&")
        println("The expected value of the new KSN is $newKSN");
        val counterKSN: String = ksn.substring(ksn.length - 5).padStart(16, '0')

        //println("The expected value of the counter KSN is $counterKSN");
        //get the number of binary associated with the counterKSN number
        var newKSNtoleft16 = newKSN.substring(newKSN.length - 16)
        //println("The expected value of the new KSN to left 16 $newKSNtoleft16");
        val counterKSNbin = Integer.toBinaryString(counterKSN.toInt())
        println("The expected value of the counter KSN Bin $counterKSNbin");
        var binarycount = counterKSNbin
        for (i in 0 until counterKSNbin.length) {
            val len: Int = binarycount.length
            var result = ""
            if (binarycount.substring(0, 1) == "1") {
                result = "1".padEnd(len, '0')
                println("The expected value of the result is $result")
                binarycount = binarycount.substring(1)
                println("The expected value of the new binary count is $binarycount")
            } else {
                binarycount = binarycount.substring(1)
                println("The expected value of the new binary count is $binarycount")
                continue
            }
            val counterKSN2 = Integer.toHexString(Integer.parseInt(result, 2))
                .toUpperCase().padStart(16, '0')
            //println("The expected value of the counter ksn 2 is $counterKSN2")
            val newKSN2 = XORorANDorORfunction(newKSNtoleft16, counterKSN2, "|")
            //println("The expected value of the new ksn 2 is $newKSN2")
            sessionkey = BlackBoxLogic(newKSN2, initialIPEK) //Call the Black Box from here
            //println("The expected value of the session key here is $sessionkey")
            newKSNtoleft16 = newKSN2
            initialIPEK = sessionkey
        }
        val checkWorkingKey = XORorANDorORfunction(
            sessionkey,
            "00000000000000FF00000000000000FF",
            "^"
        )
        println("*************************The expected value of the working key is $checkWorkingKey");
        return XORorANDorORfunction(sessionkey, "00000000000000FF00000000000000FF", "^")
    }

    fun XORorANDorORfunction(valueA: String, valueB: String, symbol: String = "|"): String {
        val a = valueA.toCharArray()
        val b = valueB.toCharArray()
        var result = ""

        for (i in 0 until a.lastIndex + 1) {
            if (symbol === "|") {
                result += (Integer.parseInt(a[i].toString(), 16).or
                    (Integer.parseInt(b[i].toString(), 16)).toString(16).toUpperCase())
            } else if (symbol === "^") {
                result += (Integer.parseInt(a[i].toString(), 16).xor
                    (Integer.parseInt(b[i].toString(), 16)).toString(16).toUpperCase())
            } else {
                result += (Integer.parseInt(a[i].toString(), 16).and
                    (Integer.parseInt(b[i].toString(), 16))).toString(16).toUpperCase()
            }
        }
        return result
    }

    fun BlackBoxLogic(ksn: String, iPek: String): String {
        if (iPek.length < 32) {
            //println("The expected value IPEK $iPek and IKSN is $ksn")
            val msg = XORorANDorORfunction(iPek, ksn, "^")
            //println("The expected value of the msg is $msg")
            val desreslt = desEncrypt(msg, iPek)
            // println("The expected value of the desresult is $desreslt")
            val rsesskey = XORorANDorORfunction(desreslt, iPek, "^")
            //println("The expected value of the session key during BBL is $rsesskey")
            return rsesskey
        }
        val current_sk = iPek
        val ksn_mod = ksn
        val leftIpek =
            XORorANDorORfunction(
                current_sk,
                "FFFFFFFFFFFFFFFF0000000000000000",
                "&"
            ).substring(16)
        val rightIpek =
            XORorANDorORfunction(current_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16)
        val message = XORorANDorORfunction(rightIpek, ksn_mod, "^")
        val desresult = desEncrypt(message, leftIpek)
        val rightSessionKey = XORorANDorORfunction(desresult, rightIpek, "^")
        val resultCurrent_sk =
            XORorANDorORfunction(current_sk, "C0C0C0C000000000C0C0C0C000000000", "^")
        val leftIpek2 = XORorANDorORfunction(
            resultCurrent_sk,
            "FFFFFFFFFFFFFFFF0000000000000000",
            "&"
        ).substring(0, 16)
        val rightIpek2 = XORorANDorORfunction(
            resultCurrent_sk,
            "0000000000000000FFFFFFFFFFFFFFFF",
            "&"
        ).substring(16)
        val message2 = XORorANDorORfunction(rightIpek2, ksn_mod, "^")
        val desresult2 = desEncrypt(message2, leftIpek2)
        val leftSessionKey = XORorANDorORfunction(desresult2, rightIpek2, "^")
        return leftSessionKey + rightSessionKey
    }

    fun encryptPinBlock(pan: String, pin: String): String {
        val pan = pan.substring(pan.length - 13).take(12).padStart(16, '0')
        println("The expected value of the encrypted pan is $pan")
        val pin = '0' + pin.length.toString(16) + pin.padEnd(16, 'F')
        //println("The expected value of the clear pin is $pin")
        return XORorANDorORfunction(pan, pin, "^")
    }

    fun hexStringToByteArray(key: String): ByteArray {
        var result: ByteArray = ByteArray(0)
        for (i in 0 until key.length step 2) {
            result += Integer.parseInt(key.substring(i, (i + 2)), 16).toByte()
        }
        return result
    }

    fun byteArrayToHexString(key: ByteArray): String {
        var st = ""
        for (b in key) {
            st += String.format("%02X", b)
        }
        return st
    }

    private fun desEncrypt(desData: String, key: String): String {
        val keyData = hexStringToByteArray(key)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            bout.write(cipher.doFinal(hexStringToByteArray(desData)))
        } catch (e: Exception) {
            print("Exception DES Encryption.. " + e.printStackTrace())
        }
        return byteArrayToHexString(bout.toByteArray()).substring(0, 16)
    }

    fun DesEncryptDukpt(workingKey: String, pan: String, clearPin: String): String {
        val pinBlock = XORorANDorORfunction(workingKey, encryptPinBlock(pan, clearPin), "^")
        val keyData = hexStringToByteArray(workingKey)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            bout.write(cipher.doFinal(hexStringToByteArray(pinBlock)))
        } catch (e: Exception) {
            println("Exception .. " + e.message)
        }
        return XORorANDorORfunction(
            workingKey, byteArrayToHexString(bout.toByteArray()).substring(
                0,
                16
            ), "^"
        )
    }


    /*fun DesEncryptDukpt(workingKey: String, clearPinBlock: String): String {
        val pinBlock = XORorANDorORfunction(workingKey, clearPinBlock, "^")
        val keyData = hexStringToByteArray(workingKey)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            bout.write(cipher.doFinal(hexStringToByteArray(pinBlock)))
        } catch (e: Exception) {
            println("Exception .. " + e.message)
        }
        return XORorANDorORfunction(
            workingKey, byteArrayToHexString(bout.toByteArray()).substring(
                0,
                16
            ), "^"
        )
    }*/


}

/**
 * @author Derek
 */
object TripleDES {
    /**
     * get correct length key for triple DES operation
     * @param key
     * @return
     */
    @JvmStatic
    private fun GetKey(key: ByteArray): ByteArray {
        val bKey = ByteArray(24)
        var i: Int
        if (key.size == 8) {
            i = 0
            while (i < 8) {
                bKey[i] = key[i]
                bKey[i + 8] = key[i]
                bKey[i + 16] = key[i]
                i++
            }
        } else if (key.size == 16) {
            i = 0
            while (i < 8) {
                bKey[i] = key[i]
                bKey[i + 8] = key[i + 8]
                bKey[i + 16] = key[i]
                i++
            }
        } else if (key.size == 24) {
            i = 0
            while (i < 24) {
                bKey[i] = key[i]
                i++
            }
        }
        return bKey
    }

    /**
     * encrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt(data: ByteArray?, key: ByteArray): ByteArray? {
//		Log.d(TripleDES.class.getSimpleName(), "Data: " +  Hex2String(data));
//		Log.d(TripleDES.class.getSimpleName(), "Key: " +  Hex2String(key));
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            return cipher.doFinal(data)
        } catch (e: NoSuchPaddingException) {

        } catch (e: NoSuchAlgorithmException) {

        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * decrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt(data: ByteArray?, key: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            return cipher.doFinal(data)
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * encrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt_CBC(data: ByteArray, key: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            val enc = ByteArray(data.size)
            val dataTemp1 = ByteArray(8)
            var dataTemp2 = ByteArray(8)
            var i = 0
            while (i < data.size) {
                for (j in 0..7) dataTemp1[j] = (data[i + j] xor dataTemp2[j])
                dataTemp2 = cipher.doFinal(dataTemp1)
                for (j in 0..7) enc[i + j] = dataTemp2[j]
                i += 8
            }
            return enc
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    @JvmStatic
    fun encrypt_CBC(data: ByteArray, key: ByteArray, IV: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            val enc = ByteArray(data.size)
            val dataTemp1 = ByteArray(8)
            var dataTemp2 = ByteArray(8)
            for (i in 0..7) dataTemp2[i] = IV[i]
            var i = 0
            while (i < data.size) {
                for (j in 0..7) dataTemp1[j] = (data[i + j] xor dataTemp2[j])
                dataTemp2 = cipher.doFinal(dataTemp1)
                for (j in 0..7) enc[i + j] = dataTemp2[j]
                i += 8
            }
            return enc
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * decrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt_CBC(data: ByteArray, key: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            val enc = cipher.doFinal(data)
            for (i in 8 until enc.size) enc[i] = enc[i] xor data[i - 8]
            return enc
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        } catch (e: NullPointerException) {
        }
        return null
    }

    /**
     * encrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt(data: String, key: String): String {
        val result: String
        val bData: ByteArray = String2Hex(data)
        val bKey: ByteArray = String2Hex(key)
        val bOutput: ByteArray? = encrypt(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * decrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = decrypt(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * encrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt_CBC(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = encrypt_CBC(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * decrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt_CBC(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = decrypt_CBC(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * Convert Byte Array to Hex String
     * @param data
     * @return
     */
    @JvmStatic
    fun Hex2String(data: ByteArray?): String {
        if (data == null) {
            return ""
        }
        var result = ""
        for (i in data.indices) {
            var tmp: Int = data[i].toInt() shr 4
            result += Integer.toString(tmp and 0x0F, 16)
            tmp = (data[i] and 0x0F).toInt()
            result += Integer.toString(tmp and 0x0F, 16)
        }
        return result
    }

    /**
     * Convert Hex String to byte array
     * @param data
     * @return
     */
    @JvmStatic
    fun String2Hex(data: String): ByteArray {
        val result: ByteArray
        result = ByteArray(data.length / 2)
        var i = 0
        while (i < data.length) {
            result[i / 2] = data.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return result
    }
}
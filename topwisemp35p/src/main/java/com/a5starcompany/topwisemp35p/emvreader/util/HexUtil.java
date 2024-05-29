package com.a5starcompany.topwisemp35p.emvreader.util;

public class HexUtil {
    public HexUtil() {
    }

    public static byte[] hexStringToByte(String data) {
        String hex = data.toUpperCase();
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();

        for (int i = 0; i < len; ++i) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }

        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    public static String bcd2str(byte[] bcds) {
        char[] ascii = "0123456789abcdef".toCharArray();
        byte[] temp = new byte[bcds.length * 2];

        for (int i = 0; i < bcds.length; ++i) {
            temp[i * 2] = (byte) (bcds[i] >> 4 & 15);
            temp[i * 2 + 1] = (byte) (bcds[i] & 15);
        }

        StringBuffer res = new StringBuffer();

        for (int i = 0; i < temp.length; ++i) {
            res.append(ascii[temp[i]]);
        }

        return res.toString().toUpperCase();
    }

    public static byte uniteBytes(byte src1, byte src2) {
        byte b1 = Byte.decode("0x" + new String(new byte[]{src1}));
        b1 = (byte) (b1 << 4);
        byte b2 = Byte.decode("0x" + new String(new byte[]{src2}));
        return (byte) (b2 ^ b1);
    }
}

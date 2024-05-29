package com.a5starcompany.topwisemp35p.emvreader.util;

import com.topwise.cloudpos.struct.BytesUtil;

import java.util.Arrays;

public class AmountUtil {

    private static final String TAG = AmountUtil.class.getSimpleName();

    /**
     * eg:1.2 -> 000000000012
     *
     * @param amt Amount
     * @return Filled amount data
     */
    public static String getFixedAmount(String amt) {
        StringBuilder buffer = new StringBuilder();
        if (amt.contains(".")) {
            String[] buf = amt.split("\\.");
            SDKLog.d(TAG, "buf len: " + buf.length);
            if (buf.length == 2) {
                if (buf[1].length() >= 2) {
                    buffer.append(buf[0]).append(buf[1].substring(0, 2));
                } else if (buf[1].length() == 1) {
                    buffer.append(buf[0]).append(buf[1]).append("0");
                } else {
                    buffer.append(buf[0]).append("00");
                }
            } else {
                return null;
            }
        } else {
            buffer.append(amt).append("00");
        }
        SDKLog.d(TAG, "amount buffer: " + buffer);
        String bufStr = buffer.toString().length() % 2 != 0 ? 0 + buffer.toString() : buffer.toString();
        byte[] amount = new byte[6];
        Arrays.fill(amount, (byte) 0x00);
        System.arraycopy(BytesUtil.hexString2Bytes(bufStr), 0, amount, 6 - bufStr.length() / 2, bufStr.length() / 2);
        return BytesUtil.bytes2HexString(amount);
    }
}

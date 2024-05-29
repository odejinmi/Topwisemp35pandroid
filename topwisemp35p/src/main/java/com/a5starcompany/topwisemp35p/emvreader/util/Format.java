package com.a5starcompany.topwisemp35p.emvreader.util;

public class Format {
    public Format() {
    }

    public static byte[] pinblock(String pan, String pin) {
        byte[] arrPan = getPan(pan);
        byte[] arrPin = getPin(pin);
        byte[] arrRet = new byte[8];

        for (int i = 0; i < 8; ++i) {
            arrRet[i] = (byte) (arrPan[i] ^ arrPin[i]);
        }

        return arrRet;
    }

    private static byte[] getPan(String pan) {
        pan = StringUtil.formatPan(pan);
        byte[] arrPan = pan.getBytes();
        byte[] pancode = new byte[]{HexUtil.uniteBytes(arrPan[0], arrPan[1]), HexUtil.uniteBytes(arrPan[2], arrPan[3]), HexUtil.uniteBytes(arrPan[4], arrPan[5]), HexUtil.uniteBytes(arrPan[6], arrPan[7]), HexUtil.uniteBytes(arrPan[8], arrPan[9]), HexUtil.uniteBytes(arrPan[10], arrPan[11]), HexUtil.uniteBytes(arrPan[12], arrPan[13]), HexUtil.uniteBytes(arrPan[14], arrPan[15])};
        return pancode;
    }

    private static byte[] getPin(String pin) {
        pin = StringUtil.formatPin(pin);
        byte[] arrPin = pin.getBytes();
        byte[] pincode = new byte[]{HexUtil.uniteBytes(arrPin[0], arrPin[1]), HexUtil.uniteBytes(arrPin[2], arrPin[3]), HexUtil.uniteBytes(arrPin[4], arrPin[5]), HexUtil.uniteBytes(arrPin[6], arrPin[7]), HexUtil.uniteBytes(arrPin[8], arrPin[9]), HexUtil.uniteBytes(arrPin[10], arrPin[11]), HexUtil.uniteBytes(arrPin[12], arrPin[13]), HexUtil.uniteBytes(arrPin[14], arrPin[15])};
        return pincode;
    }
}
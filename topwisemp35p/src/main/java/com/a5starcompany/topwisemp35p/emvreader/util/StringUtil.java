package com.a5starcompany.topwisemp35p.emvreader.util;

public class StringUtil {
    public StringUtil() {
    }

    public static String formatPan(String pan) {
        int length = pan.length();
        String encryptPan = pan.substring(length < 13 ? 0 : length - 13, length - 1);
        encryptPan = "0000" + encryptPan;
        return encryptPan;
    }

    public static String formatPin(String pin) {
        String f = "";
        String len = "";
        int length = pin.length();
        String pinBlock;
        if (10 > length) {
            switch (length) {
                case 4:
                    f = "FFFFFFFFFF";
                    break;
                case 5:
                    f = "FFFFFFFFF";
                    break;
                case 6:
                    f = "FFFFFFFF";
                    break;
                case 7:
                    f = "FFFFFFF";
                    break;
                case 8:
                    f = "FFFFFF";
                    break;
                case 9:
                    f = "FFFFF";
            }

            pinBlock = "0" + length + pin + f;
        } else {
            switch (length) {
                case 10:
                    f = "FFFF";
                    len = "A";
                    break;
                case 11:
                    f = "FFF";
                    len = "B";
                    break;
                case 12:
                    f = "FF";
                    len = "C";
            }

            pinBlock = "0" + len + pin + f;
        }

        return pinBlock;
    }

    public static final String TAGPUBLIC = "Mubby ";
}
package com.a5starcompany.topwisemp35p.emvreader.emv.impl;

public interface EmvProcessInterface {
    boolean importAmount(String amt);

    boolean importFinalAidSelectRes(boolean res);

    boolean importAidSelectRes(int index);

    boolean importPin(String pin);

    boolean importUserAuthRes(boolean res);

    boolean importMsgConfirmRes(boolean confirm);

    boolean importECashTipConfirmRes(boolean confirm);

    boolean importOnlineResp(boolean onlineRes, String respCode, String icc55);

    boolean importConfirmCardInfoRes(boolean res);

    byte[] getTlv(String tag);

    void setTlv(String tag, byte[] value);

    void endEmv();

}

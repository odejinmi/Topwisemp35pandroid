package com.a5starcompany.topwisemp35p.emvreader.emv;

import com.a5starcompany.topwisemp35p.emvreader.card.CardType;
import com.topwise.cloudpos.struct.BaseStruct;

public class EmvTransData extends BaseStruct {
    private int cardType = CardType.NONE;
    private byte transType  = 0x00;
    private boolean isForceOnline = false;
    private int amount = 0;
    private int otherAmount = 0;

    public EmvTransData() {

    }

    public EmvTransData(int cardType, byte transType, int amount) {
        setCardType(cardType);
        setTransType(transType);
        setAmount(amount);
    }

    public EmvTransData(int cardType, byte transType, int amount, boolean isForceOnline) {
        setCardType(cardType);
        setTransType(transType);
        setAmount(amount);
        setForceOnline(isForceOnline);
    }

    public EmvTransData(int cardType, byte transType, int amount, boolean isForceOnline, int otherAmount) {
        setCardType(cardType);
        setTransType(transType);
        setAmount(amount);
        setForceOnline(isForceOnline);
        setOtherAmount(otherAmount);
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public byte getTransType() {
        return transType;
    }

    public void setTransType(byte transType) {
        this.transType = transType;
    }

    public boolean isForceOnline() {
        return isForceOnline;
    }

    public void setForceOnline(boolean forceOnline) {
        isForceOnline = forceOnline;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        if (amount >= 0)
            this.amount = amount;
    }

    public int getOtherAmount() {
        return otherAmount;
    }

    public void setOtherAmount(int otherAmount) {
        if (otherAmount >= 0)
            this.otherAmount = otherAmount;
    }
}

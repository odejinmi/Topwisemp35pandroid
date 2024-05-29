package com.a5starcompany.topwisemp35p.emvreader.emv;

import androidx.annotation.Keep;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.jvm.internal.Intrinsics;


@Keep
public final class TransactionMonitor {
    @NotNull
    private final CardReadState state;
    @NotNull
    private final String message;
    private final boolean status;
    @Nullable
    private final CardReadResult transactionData;

    @NotNull
    public final CardReadState getState() {
        return this.state;
    }

    @NotNull
    public final String getMessage() {
        return this.message;
    }

    public final boolean getStatus() {
        return this.status;
    }

    @Nullable
    public final CardReadResult getTransactionData() {
        return this.transactionData;
    }

    public TransactionMonitor(@NotNull CardReadState state, @NotNull String message, boolean status, @Nullable CardReadResult transactionData) {
//        Intrinsics.checkParameterIsNotNull(state, "state");
//        Intrinsics.checkParameterIsNotNull(message, "message");
        super();
        this.state = state;
        this.message = message;
        this.status = status;
        this.transactionData = transactionData;
    }

    @NotNull
    public final CardReadState component1() {
        return this.state;
    }

    @NotNull
    public final String component2() {
        return this.message;
    }

    public final boolean component3() {
        return this.status;
    }

    @Nullable
    public final CardReadResult component4() {
        return this.transactionData;
    }

    @NotNull
    public final TransactionMonitor copy(@NotNull CardReadState state, @NotNull String message, boolean status, @Nullable CardReadResult transactionData) {
        Intrinsics.checkParameterIsNotNull(state, "state");
        Intrinsics.checkParameterIsNotNull(message, "message");
        return new TransactionMonitor(state, message, status, transactionData);
    }

    // $FF: synthetic method
    public static TransactionMonitor copy$default(TransactionMonitor var0, CardReadState var1, String var2, boolean var3, CardReadResult var4, int var5, Object var6) {
        if ((var5 & 1) != 0) {
            var1 = var0.state;
        }

        if ((var5 & 2) != 0) {
            var2 = var0.message;
        }

        if ((var5 & 4) != 0) {
            var3 = var0.status;
        }

        if ((var5 & 8) != 0) {
            var4 = var0.transactionData;
        }

        return var0.copy(var1, var2, var3, var4);
    }

    @NotNull
    public String toString() {
        return "TransactionMonitor(state=" + this.state + ", message=" + this.message + ", status=" + this.status + ", transactionData=" + this.transactionData + ")";
    }

    public int hashCode() {
        CardReadState var10000 = this.state;
        int var1 = (var10000 != null ? var10000.hashCode() : 0) * 31;
        String var10001 = this.message;
        var1 = (var1 + (var10001 != null ? var10001.hashCode() : 0)) * 31;
        byte var2 = this.status ? (byte) 1 : (byte) 0;
        if (var2 != 0) {
            var2 = 1;
        }

        var1 = (var1 + var2) * 31;
        CardReadResult var3 = this.transactionData;
        return var1 + (var3 != null ? var3.hashCode() : 0);
    }

    public boolean equals(@Nullable Object var1) {
        if (this != var1) {
            if (var1 instanceof TransactionMonitor) {
                TransactionMonitor var2 = (TransactionMonitor)var1;
                if (Intrinsics.areEqual(this.state, var2.state) && Intrinsics.areEqual(this.message, var2.message) && this.status == var2.status && Intrinsics.areEqual(this.transactionData, var2.transactionData)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }
}

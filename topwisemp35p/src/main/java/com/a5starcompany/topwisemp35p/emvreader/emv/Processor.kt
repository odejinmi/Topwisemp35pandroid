package com.a5starcompany.topwisemp35p.emvreader.emv

enum class Processor(var message: String) {
    ELKANAH("elkanah"), INTERSWITCH("interswitch"), NOT_SUPPORTED("this transaction is not supported!"), THREELINE(
        "3line"
    ),
    FLUTTERWAVE("flutterwave")
}

enum class AccountType(var accountType: String, var code: String) {
    DEFAULT("default", "00"), SAVINGS("savings", "10"), CURRENT("current", "20"), CREDIT(
        "credit",
        "30"
    )
}
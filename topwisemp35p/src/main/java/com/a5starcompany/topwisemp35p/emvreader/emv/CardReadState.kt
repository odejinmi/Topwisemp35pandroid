package com.a5starcompany.topwisemp35p.emvreader.emv

enum class CardReadState {
    Loading,
    CardData,
    CardReadTimeOut,
    CallBackError,
    CallBackCanceled,
    CallBackTransResult,
    CardDetected,
}





package com.lura.moneymanager.model

class MessageDataResponse(
    var messageList: MutableList<MessageData>,
    var creditedAmount: Double,
    var debittedAmount: Double
)
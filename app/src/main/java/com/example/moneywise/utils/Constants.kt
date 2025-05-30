package com.example.moneywise.utils

object Constants {
    const val SMS_PERMISSION_REQUEST_CODE = 1001

    // Opérateurs Mobile Money
    val MOBILE_MONEY_SENDERS = listOf(
        "MVola", "AirtelMoney", "OrangeMoney",
        "Telma", "Airtel", "Orange"
    )

    // Types de transactions
    const val TRANSACTION_TYPE_DEPOT = "depot"
    const val TRANSACTION_TYPE_RETRAIT = "retrait"
    const val TRANSACTION_TYPE_TRANSFERT = "transfert"

    // Formats de date
    val DATE_FORMATS = listOf(
        "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy"
    )
}
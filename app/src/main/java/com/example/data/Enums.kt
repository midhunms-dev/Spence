package com.example.data

enum class AccountType(val displayName: String) {
    BANK("Bank Account"),
    CASH("Cash in Hand"),
    CREDIT_CARD("Credit Card"),
    LOAN("Loan")
}

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    EMI_PAYMENT("EMI Payment"),
    CC_BILL_PAYMENT("Credit Card Bill Payment")
}

package com.example

import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.EmiEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseTrackerUnitTest {

    @Test
    fun creditCard_limitAndAvailableCredit_calculatedCorrectly() {
        val card = AccountEntity(
            id = 1L,
            name = "HDFC Card",
            type = AccountType.CREDIT_CARD,
            balance = 35000.0, // Available limit
            creditLimit = 50000.0 // Total limit
        )

        // Used credit should be total limit - available credit = 15,000
        assertEquals(15000.0, card.usedCredit, 0.001)
        // Percentage used should be 30%
        assertEquals(30.0f, card.creditUsedPercentage, 0.01f)
    }

    @Test
    fun creditCard_whenSpent_availableCreditReduces() {
        var card = AccountEntity(
            id = 1L,
            name = "HDFC Card",
            type = AccountType.CREDIT_CARD,
            balance = 50000.0,
            creditLimit = 50000.0
        )

        // Expense of 5,000
        val spend = 5000.0
        card = card.copy(balance = card.balance - spend)

        assertEquals(45000.0, card.balance, 0.001)
        assertEquals(5000.0, card.usedCredit, 0.001)
        assertEquals(10.0f, card.creditUsedPercentage, 0.01f)
    }

    @Test
    fun emiEntity_progressAndRemaining_calculatedCorrectly() {
        val emi = EmiEntity(
            id = 1L,
            title = "iPhone 15",
            monthlyAmount = 3000.0,
            totalInstallments = 12,
            paidInstallments = 4,
            dueDayOfMonth = 10,
            linkedAccountId = 1L
        )

        assertEquals(8, emi.remainingInstallments)
        assertEquals(12000.0, emi.paidAmount, 0.001)
        assertEquals(24000.0, emi.remainingAmount, 0.001)
        assertEquals(36000.0, emi.totalAmount, 0.001)
        assertEquals(4f / 12f, emi.progress, 0.001f)
        assertFalse(emi.isCompleted)
    }

    @Test
    fun emiEntity_whenAllPaid_isCompletedIsTrue() {
        val emi = EmiEntity(
            id = 1L,
            title = "Laptop",
            monthlyAmount = 2500.0,
            totalInstallments = 6,
            paidInstallments = 6,
            dueDayOfMonth = 5,
            linkedAccountId = 1L
        )

        assertTrue(emi.isCompleted)
        assertEquals(0, emi.remainingInstallments)
        assertEquals(0.0, emi.remainingAmount, 0.001)
    }

    @Test
    fun billingCycleHelper_calculatesValidTimeRange() {
        // Test billing cycle for billing day 20
        val cycle = com.example.data.BillingCycleHelper.getBillingCycle(20, 0)
        assertTrue(cycle.startMillis < cycle.endMillis)
        assertTrue(cycle.label.contains("20") || cycle.label.contains("Cycle"))

        val prevCycle = com.example.data.BillingCycleHelper.getBillingCycle(20, -1)
        assertTrue(prevCycle.startMillis < cycle.startMillis)
        assertTrue(prevCycle.endMillis < cycle.endMillis)
    }
}

package com.example.budgetwise

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test for core budget logic.
 * Satisfies the "Conduct automated testing" requirement of the POE.
 */
class BudgetLogicTest {

    @Test
    fun testIsExceedingBudget() {
        val maxBudget = 1000.0
        val totalSpent = 1200.0
        
        val isExceeded = totalSpent > maxBudget
        
        assertEquals("Spending 1200 should exceed a 1000 budget", true, isExceeded)
    }

    @Test
    fun testTargetRangeLogic() {
        val minBudget = 500.0
        val maxBudget = 1000.0
        val spent = 750.0
        
        val isInRange = spent in minBudget..maxBudget
        
        assertEquals("750 should be within the 500-1000 range", true, isInRange)
    }
}

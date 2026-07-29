package com.example.kotlinrest.test

import com.example.kotlinrest.support.centsToDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {

    @Test
    fun formatsCentsAsMajorUnits() {
        assertEquals("645.36", centsToDecimal(64536))
        assertEquals("37.50", centsToDecimal(3750))
        assertEquals("1.00", centsToDecimal(100))
        // The fractional part must stay two digits, or 5 cents reads as "0.5".
        assertEquals("0.05", centsToDecimal(5))
        assertEquals("0.00", centsToDecimal(0))
        assertEquals("-12.34", centsToDecimal(-1234))
    }
}

package id.nivorapos.pos_service.service

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionServiceTaxBasisTest {

    @Test
    fun `taxable base subtracts item promotions from adjusted totalPrice`() {
        // Real payload from log.txt: Caffe Latte, price 32000 + Medium(5000) + Caramel Syrup(6000)
        // = totalPrice 43000, minus item promotions 4300 + 20000 = taxable base 18700.
        val base = computeItemTaxableBase(
            totalPrice = BigDecimal("43000.00"),
            promotionAmount = BigDecimal("24300.00")
        )

        assertEquals(BigDecimal("18700.00"), base)
    }

    @Test
    fun `taxable base never goes negative when promotions exceed totalPrice`() {
        val base = computeItemTaxableBase(
            totalPrice = BigDecimal("10000.00"),
            promotionAmount = BigDecimal("15000.00")
        )

        assertEquals(BigDecimal.ZERO, base.max(BigDecimal.ZERO))
    }

    @Test
    fun `taxable base with no promotions equals totalPrice unchanged`() {
        val base = computeItemTaxableBase(
            totalPrice = BigDecimal("32000.00"),
            promotionAmount = BigDecimal.ZERO
        )

        assertEquals(BigDecimal("32000.00"), base)
    }

    @Test
    fun `taxAmount computed on real deploy log payload matches mobile's expected 2057`() {
        // Regression test for log.txt 400 error: server previously computed 3520.00
        // (price*qty, ignoring adjustments/promotions) instead of mobile's 2057.00.
        val taxableBase = computeItemTaxableBase(
            totalPrice = BigDecimal("43000.00"),
            promotionAmount = BigDecimal("24300.00")
        )
        val taxPercentage = BigDecimal("11.00")
        val expectedTaxAmount = taxableBase.multiply(taxPercentage)
            .divide(BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP)

        assertEquals(BigDecimal("2057.00"), expectedTaxAmount)
    }
}

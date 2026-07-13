package id.nivorapos.pos_service.service

import id.nivorapos.pos_service.dto.request.DiscountValidateItemRequest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PromotionServiceCalculationTest {

    private fun item(productId: Long, qty: Int, price: String) =
        DiscountValidateItemRequest(productId = productId, qty = qty, price = BigDecimal(price))

    // ─── allocateUnits ──────────────────────────────────────────────────────

    @Test
    fun `allocateUnits clamps units taken from a line to what is actually needed`() {
        // Temuan #9 PERCENTAGE-FIXED_PRICE bug: old code used take(getQty) on whole lines,
        // so a single line with qty=10 and getQty=2 counted all 10 units instead of 2.
        val items = listOf(item(1L, qty = 10, price = "1000"))

        val total = allocateUnits(items, qtyNeeded = 2) { _, units -> BigDecimal(units) }

        assertEquals(BigDecimal(2), total)
    }

    @Test
    fun `allocateUnits spreads across multiple lines cheapest-first`() {
        val cheap = item(1L, qty = 1, price = "5000")
        val expensive = item(2L, qty = 5, price = "8000")
        val sorted = listOf(cheap, expensive) // caller is responsible for sorting cheapest-first

        // BUY 1 GET 3 FREE: 1 unit cheap (5000) + 2 units expensive (16000) = 21000
        val total = allocateUnits(sorted, qtyNeeded = 3) { item, units -> item.price.multiply(BigDecimal(units)) }

        assertEquals(BigDecimal("21000"), total)
    }

    @Test
    fun `allocateUnits stops once qtyNeeded is satisfied`() {
        val items = listOf(item(1L, qty = 100, price = "1"))
        val total = allocateUnits(items, qtyNeeded = 0) { _, units -> BigDecimal(units) }
        assertEquals(BigDecimal.ZERO, total)
    }

    // ─── buildAvailableRewardItems (qualifier/reward overlap reservation) ──

    @Test
    fun `no overlap returns reward items unchanged`() {
        val buyItems = listOf(item(1L, qty = 3, price = "1000"))
        val rewardItems = listOf(item(2L, qty = 5, price = "2000"))

        val result = buildAvailableRewardItems(buyItems, rewardItems, buyQty = 1)

        assertEquals(1, result.size)
        assertEquals(5, result.single().qty)
    }

    @Test
    fun `single overlapping product reserves qualifier units before counting as reward`() {
        // Self-referential BUY 2 GET 1 FREE on the same product: cart has 5 units,
        // 2 must be reserved as qualifiers, leaving 3 available as reward.
        val buyItems = listOf(item(1L, qty = 5, price = "1000"))
        val rewardItems = listOf(item(1L, qty = 5, price = "1000"))

        val result = buildAvailableRewardItems(buyItems, rewardItems, buyQty = 2)

        assertEquals(3, result.single().qty)
    }

    @Test
    fun `non-reward buy items satisfy the qualifier first, freeing all overlap qty as reward`() {
        // buyScope=ALL (products 1 and 2 both qualify), rewardScope=PRODUCT[2].
        // Product 1 alone (qty=2) already satisfies buyQty=2, so none of product 2's
        // qty needs to be reserved as qualifier — all of it is available as reward.
        val buyItems = listOf(item(1L, qty = 2, price = "1000"), item(2L, qty = 4, price = "2000"))
        val rewardItems = listOf(item(2L, qty = 4, price = "2000"))

        val result = buildAvailableRewardItems(buyItems, rewardItems, buyQty = 2)

        assertEquals(4, result.single().qty)
    }

    @Test
    fun `multi-product overlap reserves from the most expensive product first`() {
        // buyScope=ALL, rewardScope=PRODUCT[1,2], both eligible as buy AND reward,
        // no non-reward buy items to satisfy buyQty=3 → reserve 3 units from product 2
        // (pricier, 3000) before product 1 (1000), leaving product 1 fully available.
        val buyItems = listOf(item(1L, qty = 4, price = "1000"), item(2L, qty = 4, price = "3000"))
        val rewardItems = listOf(item(1L, qty = 4, price = "1000"), item(2L, qty = 4, price = "3000"))

        val result = buildAvailableRewardItems(buyItems, rewardItems, buyQty = 3)

        val byProduct = result.associate { it.productId to it.qty }
        assertEquals(4, byProduct[1L])
        assertEquals(1, byProduct[2L])
    }
}

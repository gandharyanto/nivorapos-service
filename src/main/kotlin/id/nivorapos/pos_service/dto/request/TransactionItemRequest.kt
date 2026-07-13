package id.nivorapos.pos_service.dto.request

data class TransactionItemRequest(
    val productId: Long,
    val qty: Int,
    val price: String,
    val taxId: Long? = null,
    val taxAmount: String? = null,
    val variantId: Long? = null,
    val modifierIds: List<Long> = emptyList(),
    /** Mobile's variant/modifier breakdown format — entries with detailType="MODIFIER" carry the modifier id in referenceId. */
    val details: List<TransactionItemDetailRequest> = emptyList(),
    /** Mobile's tax breakdown format — mobile only ever populates this, never the legacy flat [taxId]/[taxAmount]. */
    val taxes: List<TransactionItemTaxRequest> = emptyList(),
    /** Mobile's adjusted line total: price + variant/modifier priceAdjustment, before this item's own promotions. */
    val totalPrice: String? = null,
    /** Item-level promotions (BuyXGetY reward allocations etc.) applied to this line before tax. */
    val promotions: List<TransactionItemPromotionRequest> = emptyList()
) {
    /** Union of the legacy flat [modifierIds] and modifier ids resolved from [details], since mobile only ever populates the latter. */
    val effectiveModifierIds: List<Long>
        get() = (modifierIds + details.filter { it.detailType == "MODIFIER" }.map { it.referenceId }).distinct()

    /** Prefer the [taxes] breakdown mobile sends; fall back to the legacy flat field. */
    val effectiveTaxId: Long?
        get() = taxes.firstOrNull()?.id ?: taxId

    val effectiveTaxAmount: String?
        get() = taxes.firstOrNull()?.amt ?: taxAmount
}

data class TransactionItemDetailRequest(
    val detailType: String,
    val referenceId: Long
)

data class TransactionItemTaxRequest(
    val id: Long? = null,
    val amt: String? = null,
    val type: String? = null,
    val value: java.math.BigDecimal? = null
)

data class TransactionItemPromotionRequest(
    val id: Long? = null,
    val amt: String? = null,
    val type: String? = null
)

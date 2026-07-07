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
    val details: List<TransactionItemDetailRequest> = emptyList()
) {
    /** Union of the legacy flat [modifierIds] and modifier ids resolved from [details], since mobile only ever populates the latter. */
    val effectiveModifierIds: List<Long>
        get() = (modifierIds + details.filter { it.detailType == "MODIFIER" }.map { it.referenceId }).distinct()
}

data class TransactionItemDetailRequest(
    val detailType: String,
    val referenceId: Long
)

package id.nivorapos.pos_service.dto.request

data class TransactionItemRequest(
    val productId: Long,
    val qty: Int,
    val price: String,
    val taxId: Long? = null
)

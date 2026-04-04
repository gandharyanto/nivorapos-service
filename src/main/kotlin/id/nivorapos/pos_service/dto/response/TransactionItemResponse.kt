package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal

data class TransactionItemResponse(
    val productId: Long,
    val productName: String,
    val price: BigDecimal,
    val qty: Int,
    val totalPrice: BigDecimal,
    val taxAmount: BigDecimal
)

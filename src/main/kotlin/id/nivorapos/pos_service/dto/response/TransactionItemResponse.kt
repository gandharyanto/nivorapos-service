package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionItemResponse(
    val id: Long,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val price: BigDecimal,
    val qty: Int,
    val totalPrice: BigDecimal,
    val taxId: Long?,
    val taxName: String?,
    val taxPercentage: BigDecimal,
    val taxAmount: BigDecimal,
    val createdDate: LocalDateTime?
)

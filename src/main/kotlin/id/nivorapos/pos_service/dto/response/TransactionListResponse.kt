package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionListResponse(
    val id: Long,
    val merchantId: Long,
    val trxId: String,
    val status: String,
    val paymentMethod: String?,
    val subTotal: BigDecimal,
    val totalAmount: BigDecimal,
    val totalTax: BigDecimal,
    val totalRounding: BigDecimal,
    val cashTendered: BigDecimal,
    val cashChange: BigDecimal,
    val username: String?,
    val createdDate: LocalDateTime?
)

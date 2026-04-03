package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentResponse(
    val id: Long,
    val transactionId: Long,
    val paymentTrxId: String?,
    val paymentMethod: String?,
    val paymentSource: String?,
    val amountPaid: BigDecimal,
    val status: String,
    val isEffective: Boolean,
    val paymentReference: String?,
    val paymentDate: LocalDateTime?,
    val createdDate: LocalDateTime?
)

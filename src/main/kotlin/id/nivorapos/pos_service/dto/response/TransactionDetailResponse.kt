package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionDetailResponse(
    val id: Long,
    val merchantId: Long,
    val outletId: Long?,
    val merchantUniqueCode: String?,
    val username: String?,
    val trxId: String,
    val transactionOrigin: String?,
    val status: String,
    val paymentMethod: String?,
    val priceIncludeTax: Boolean,
    val subTotal: BigDecimal,
    val totalAmount: BigDecimal,
    val serviceChargePercentage: BigDecimal,
    val serviceChargeAmount: BigDecimal,
    val totalServiceCharge: BigDecimal,
    val taxPercentage: BigDecimal,
    val totalTax: BigDecimal,
    val taxName: String?,
    val totalRounding: BigDecimal,
    val roundingType: String?,
    val roundingTarget: String?,
    val cashTendered: BigDecimal,
    val cashChange: BigDecimal,
    val queueId: Long?,
    val queueNumber: String?,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?,
    val items: List<TransactionItemResponse>,
    val payments: List<PaymentResponse>
)

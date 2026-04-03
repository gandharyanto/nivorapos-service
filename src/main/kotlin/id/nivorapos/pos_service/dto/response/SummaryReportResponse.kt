package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal

data class ProductSalesSummary(
    val productName: String,
    val totalSaleItems: Long,
    val totalAmount: BigDecimal
)

data class PaymentSummary(
    val paymentMethod: String,
    val totalAmount: BigDecimal,
    val totalTransactions: Long
)

data class SummaryReportResponse(
    val totalTransactions: Long,
    val totalRevenue: BigDecimal,
    val productSales: List<ProductSalesSummary>,
    val internalPayments: List<PaymentSummary>,
    val externalPayments: List<PaymentSummary>
)

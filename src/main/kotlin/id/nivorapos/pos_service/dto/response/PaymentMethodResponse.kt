package id.nivorapos.pos_service.dto.response

data class PaymentMethodResponse(
    val id: Long,
    val merchantPaymentMethodId: Long,
    val code: String,
    val name: String,
    val category: String?,
    val paymentType: String?,
    val provider: String?,
    val isEnabled: Boolean,
    val displayOrder: Int,
    val configJson: String?
)

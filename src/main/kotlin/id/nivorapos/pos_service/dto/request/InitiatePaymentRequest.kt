package id.nivorapos.pos_service.dto.request

data class InitiatePaymentAdditionalInfo(
    val qrContent: String? = null
)

data class InitiatePaymentRequest(
    val paymentMethod: String? = null,
    val totalAmount: String? = null,
    val paymentTrxId: String? = null,
    val additionalInfo: InitiatePaymentAdditionalInfo? = null
)

package id.nivorapos.pos_service.dto.response

data class PaymentMethodListResponse(
    val internalPayments: List<PaymentMethodResponse>,
    val externalPayments: List<PaymentMethodResponse>
)

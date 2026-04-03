package id.nivorapos.pos_service.dto.request

data class TransactionUpdateRequest(
    val status: String,
    val paymentStatus: String? = null,
    val paymentReference: String? = null,
    val paymentTrxId: String? = null
)

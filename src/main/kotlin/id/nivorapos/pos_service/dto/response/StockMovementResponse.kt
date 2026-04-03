package id.nivorapos.pos_service.dto.response

import java.time.LocalDateTime

data class StockMovementResponse(
    val id: Long,
    val productId: Long,
    val merchantId: Long,
    val outletId: Long?,
    val referenceId: Long?,
    val qty: Int,
    val movementType: String,
    val movementReason: String?,
    val note: String?,
    val createdBy: String?,
    val createdDate: LocalDateTime?
)

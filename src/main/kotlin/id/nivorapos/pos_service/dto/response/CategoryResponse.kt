package id.nivorapos.pos_service.dto.response

import java.time.LocalDateTime

data class CategoryResponse(
    val id: Long,
    val merchantId: Long,
    val name: String,
    val image: String?,
    val description: String?,
    val createdBy: String?,
    val createdDate: LocalDateTime?,
    val modifiedBy: String?,
    val modifiedDate: LocalDateTime?
)

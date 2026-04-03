package id.nivorapos.pos_service.dto.response

data class PagedResponse<T>(
    val status: String = "SUCCESS",
    val message: String,
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

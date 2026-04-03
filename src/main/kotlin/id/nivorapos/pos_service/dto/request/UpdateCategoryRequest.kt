package id.nivorapos.pos_service.dto.request

data class UpdateCategoryRequest(
    val id: Long,
    val name: String,
    val image: String? = null,
    val description: String? = null
)

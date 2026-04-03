package id.nivorapos.pos_service.dto.request

data class CategoryRequest(
    val name: String,
    val image: String? = null,
    val description: String? = null
)

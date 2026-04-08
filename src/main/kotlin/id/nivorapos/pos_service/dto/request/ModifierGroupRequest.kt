package id.nivorapos.pos_service.dto.request

data class ModifierGroupRequest(
    val name: String,
    val isRequired: Boolean,
    val minSelect: Int = 0,
    val maxSelect: Int = 1,
    val displayOrder: Int = 0
)

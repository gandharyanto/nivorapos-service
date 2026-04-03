package id.nivorapos.pos_service.dto.request

data class MerchantRolePermissionRequest(
    val roleId: Long,
    val permissionId: Long,
    val isGranted: Boolean = true
)

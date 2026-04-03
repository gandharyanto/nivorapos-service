package id.nivorapos.pos_service.dto.response

import java.time.LocalDateTime

data class MerchantRolePermissionResponse(
    val id: Long,
    val merchantId: Long,
    val roleId: Long,
    val roleCode: String?,
    val roleName: String?,
    val permissionId: Long,
    val permissionCode: String?,
    val permissionName: String?,
    val isGranted: Boolean,
    val createdDate: LocalDateTime?
)

data class EffectivePermissionResponse(
    val merchantId: Long,
    val roleId: Long,
    val roleCode: String?,
    val roleName: String?,
    val permissions: List<PermissionItem>
) {
    data class PermissionItem(
        val id: Long,
        val code: String,
        val name: String,
        val source: String  // "GLOBAL" or "MERCHANT_OVERRIDE"
    )
}

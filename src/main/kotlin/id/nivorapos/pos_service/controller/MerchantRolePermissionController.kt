package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.MerchantRolePermissionRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.EffectivePermissionResponse
import id.nivorapos.pos_service.dto.response.MerchantRolePermissionResponse
import id.nivorapos.pos_service.service.MerchantRolePermissionService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/merchant-role-permission")
class MerchantRolePermissionController(
    private val service: MerchantRolePermissionService
) {

    // List semua overrides untuk merchant tertentu
    @GetMapping("/merchant/{merchantId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun listByMerchant(
        @PathVariable merchantId: Long
    ): ResponseEntity<ApiResponse<List<MerchantRolePermissionResponse>>> {
        return ResponseEntity.ok(service.listByMerchant(merchantId))
    }

    // List overrides untuk merchant + role tertentu
    @GetMapping("/merchant/{merchantId}/role/{roleId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun listByMerchantAndRole(
        @PathVariable merchantId: Long,
        @PathVariable roleId: Long
    ): ResponseEntity<ApiResponse<List<MerchantRolePermissionResponse>>> {
        return ResponseEntity.ok(service.listByMerchantAndRole(merchantId, roleId))
    }

    // Lihat effective permissions (global + override) untuk merchant + role
    @GetMapping("/merchant/{merchantId}/role/{roleId}/effective")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun getEffective(
        @PathVariable merchantId: Long,
        @PathVariable roleId: Long
    ): ResponseEntity<ApiResponse<EffectivePermissionResponse>> {
        return ResponseEntity.ok(service.getEffectivePermissions(merchantId, roleId))
    }

    // Set override (upsert): is_granted=true untuk grant, false untuk deny
    @PostMapping("/merchant/{merchantId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun setOverride(
        @PathVariable merchantId: Long,
        @RequestBody request: MerchantRolePermissionRequest
    ): ResponseEntity<ApiResponse<MerchantRolePermissionResponse>> {
        return try {
            ResponseEntity.ok(service.setOverride(merchantId, request))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed"))
        }
    }

    // Hapus 1 override by id
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun deleteOverride(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            ResponseEntity.ok(service.deleteOverride(id))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Not found"))
        }
    }

    // Reset semua overrides untuk merchant + role (kembali ke global default)
    @DeleteMapping("/merchant/{merchantId}/role/{roleId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun resetByRole(
        @PathVariable merchantId: Long,
        @PathVariable roleId: Long
    ): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.ok(service.deleteAllOverridesByRole(merchantId, roleId))
    }
}

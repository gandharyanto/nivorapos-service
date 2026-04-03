package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun existsByCode(code: String): Boolean
}

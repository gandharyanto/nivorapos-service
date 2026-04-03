package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleRepository : JpaRepository<UserRole, Long> {
    fun findByUserId(userId: Long): List<UserRole>
    fun existsByUserIdAndRoleId(userId: Long, roleId: Long): Boolean
}

package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RoleRepository : JpaRepository<Role, Long> {
    fun existsByCode(code: String): Boolean
    fun findByCode(code: String): Optional<Role>
}

package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Area
import org.springframework.data.jpa.repository.JpaRepository

interface AreaRepository : JpaRepository<Area, Long> {
    fun existsByCode(code: String): Boolean
}

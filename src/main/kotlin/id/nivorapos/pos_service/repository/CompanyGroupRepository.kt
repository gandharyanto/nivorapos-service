package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.CompanyGroup
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyGroupRepository : JpaRepository<CompanyGroup, Long> {
    fun existsByCode(code: String): Boolean
}

package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun existsByCode(code: String): Boolean
}

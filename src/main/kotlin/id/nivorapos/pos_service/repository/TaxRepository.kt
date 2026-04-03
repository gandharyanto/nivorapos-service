package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Tax
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxRepository : JpaRepository<Tax, Long> {
    fun findByMerchantId(merchantId: Long): List<Tax>
}

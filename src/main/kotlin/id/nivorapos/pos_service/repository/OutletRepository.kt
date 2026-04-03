package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Outlet
import org.springframework.data.jpa.repository.JpaRepository

interface OutletRepository : JpaRepository<Outlet, Long> {
    fun findAllByMerchantId(merchantId: Long): List<Outlet>
    fun existsByMerchantIdAndCode(merchantId: Long, code: String): Boolean
}

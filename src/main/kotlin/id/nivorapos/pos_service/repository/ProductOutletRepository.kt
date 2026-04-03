package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.ProductOutlet
import org.springframework.data.jpa.repository.JpaRepository

interface ProductOutletRepository : JpaRepository<ProductOutlet, Long> {
    fun existsByProductIdAndOutletId(productId: Long, outletId: Long): Boolean
}

package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.ProductVariantGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductVariantGroupRepository : JpaRepository<ProductVariantGroup, Long> {
    fun findByProductId(productId: Long): List<ProductVariantGroup>
    fun findByProductIdAndId(productId: Long, id: Long): ProductVariantGroup?
    fun existsByProductId(productId: Long): Boolean
}

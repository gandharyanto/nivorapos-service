package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.ProductModifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductModifierRepository : JpaRepository<ProductModifier, Long> {
    fun findByProductId(productId: Long): List<ProductModifier>
    fun findByModifierGroupId(modifierGroupId: Long): List<ProductModifier>
    fun findByProductIdAndId(productId: Long, id: Long): ProductModifier?
    fun existsByModifierGroupIdAndIsActiveTrue(modifierGroupId: Long): Boolean
    fun findByModifierGroupIdAndIsDefaultTrue(modifierGroupId: Long): List<ProductModifier>
}

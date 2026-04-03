package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByMerchantIdAndId(merchantId: Long, id: Long): Optional<Category>
    fun findAllByMerchantId(merchantId: Long, pageable: Pageable): Page<Category>
}

package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.Product
import id.nivorapos.pos_service.entity.ProductCategory
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime

object ProductSpecification {

    fun withFilters(
        merchantId: Long,
        keyword: String?,
        sku: String?,
        upc: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        categoryId: Long?
    ): Specification<Product> = Specification { root, query, cb ->
        val predicates = mutableListOf(
            cb.equal(root.get<Long>("merchantId"), merchantId),
            cb.isNull(root.get<LocalDateTime>("deletedDate"))
        )

        if (!keyword.isNullOrBlank()) {
            predicates.add(
                cb.like(cb.lower(root.get("name")), "%${keyword.lowercase()}%")
            )
        }

        if (!sku.isNullOrBlank()) {
            predicates.add(cb.equal(root.get<String>("sku"), sku))
        }

        if (!upc.isNullOrBlank()) {
            predicates.add(cb.equal(root.get<String>("upc"), upc))
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), startDate))
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), endDate))
        }

        if (categoryId != null) {
            val subquery = query!!.subquery(Long::class.java)
            val pc = subquery.from(ProductCategory::class.java)
            subquery.select(pc.get("id"))
                .where(
                    cb.equal(pc.get<Long>("productId"), root.get<Long>("id")),
                    cb.equal(pc.get<Long>("categoryId"), categoryId)
                )
            predicates.add(cb.exists(subquery))
        }

        query?.distinct(true)
        cb.and(*predicates.toTypedArray())
    }
}

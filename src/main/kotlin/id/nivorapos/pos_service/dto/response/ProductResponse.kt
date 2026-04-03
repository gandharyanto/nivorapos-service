package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductImageResponse(
    val id: Long,
    val filename: String?,
    val ext: String?,
    val isMain: Boolean
)

data class ProductTaxResponse(
    val id: Long,
    val name: String,
    val percentage: BigDecimal,
    val isActive: Boolean,
    val isDefault: Boolean
)

data class ProductResponse(
    val id: Long,
    val merchantId: Long,
    val name: String,
    val price: BigDecimal,
    val sku: String?,
    val upc: String?,
    val imageUrl: String?,
    val imageThumbUrl: String?,
    val description: String?,
    val stockMode: String?,
    val basePrice: BigDecimal?,
    val isTaxable: Boolean,
    val taxId: Long?,
    val tax: ProductTaxResponse?,
    val qty: Int,
    val categories: List<CategoryResponse>,
    val images: List<ProductImageResponse>,
    val createdBy: String?,
    val createdDate: LocalDateTime?,
    val modifiedBy: String?,
    val modifiedDate: LocalDateTime?
)

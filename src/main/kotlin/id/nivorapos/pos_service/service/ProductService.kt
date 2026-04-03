package id.nivorapos.pos_service.service

import id.nivorapos.pos_service.dto.request.ProductRequest
import id.nivorapos.pos_service.dto.request.UpdateProductRequest
import id.nivorapos.pos_service.dto.response.*
import id.nivorapos.pos_service.entity.Product
import id.nivorapos.pos_service.entity.ProductCategory
import id.nivorapos.pos_service.entity.Stock
import id.nivorapos.pos_service.repository.*
import id.nivorapos.pos_service.repository.ProductSpecification
import id.nivorapos.pos_service.security.SecurityUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productImageRepository: ProductImageRepository,
    private val categoryRepository: CategoryRepository,
    private val stockRepository: StockRepository,
    private val taxRepository: TaxRepository
) {

    fun list(
        page: Int,
        size: Int,
        categoryId: Long?,
        keyword: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        upc: String?,
        sku: String?,
        sortBy: String?,
        sortDir: String?
    ): PagedResponse<ProductResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val direction = if (sortDir?.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC
        val sortField = sortBy ?: "createdDate"
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortField))

        val spec = ProductSpecification.withFilters(
            merchantId = merchantId,
            keyword = keyword,
            sku = sku,
            upc = upc,
            startDate = startDate,
            endDate = endDate,
            categoryId = categoryId
        )
        val result = productRepository.findAll(spec, pageable)

        return PagedResponse(
            message = "Product list retrieved",
            data = result.content.map { buildProductResponse(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun detail(id: Long): ApiResponse<ProductResponse> {
        val product = productRepository.findByIdAndDeletedDateIsNull(id)
            .orElseThrow { RuntimeException("Product not found") }
        return ApiResponse.success("Product found", buildProductResponse(product))
    }

    @Transactional
    fun add(request: ProductRequest): ApiResponse<ProductResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        val product = Product(
            merchantId = merchantId,
            name = request.name,
            price = request.price,
            sku = request.sku,
            upc = request.upc,
            imageUrl = request.imageUrl,
            imageThumbUrl = request.imageThumbUrl,
            description = request.description,
            stockMode = request.stockMode,
            basePrice = request.basePrice,
            isTaxable = request.isTaxable,
            taxId = request.taxId,
            createdBy = username,
            createdDate = now,
            modifiedBy = username,
            modifiedDate = now
        )
        val saved = productRepository.save(product)

        // Save stock
        val stock = Stock(
            productId = saved.id,
            qty = request.qty,
            createdBy = username,
            createdDate = now,
            modifiedBy = username,
            modifiedDate = now
        )
        stockRepository.save(stock)

        // Save categories
        request.categoryIds.forEach { catId ->
            val pc = ProductCategory(
                productId = saved.id,
                categoryId = catId,
                createdBy = username,
                createdDate = now,
                modifiedBy = username,
                modifiedDate = now
            )
            productCategoryRepository.save(pc)
        }

        return ApiResponse.success("Product created", buildProductResponse(saved))
    }

    @Transactional
    fun update(request: UpdateProductRequest): ApiResponse<ProductResponse> {
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        val product = productRepository.findByIdAndDeletedDateIsNull(request.id)
            .orElseThrow { RuntimeException("Product not found") }

        product.name = request.name
        product.price = request.price
        product.sku = request.sku
        product.upc = request.upc
        product.imageUrl = request.imageUrl
        product.imageThumbUrl = request.imageThumbUrl
        product.description = request.description
        product.stockMode = request.stockMode
        product.basePrice = request.basePrice
        product.isTaxable = request.isTaxable
        product.taxId = request.taxId
        product.modifiedBy = username
        product.modifiedDate = now

        val saved = productRepository.save(product)

        // Update categories
        productCategoryRepository.deleteByProductId(saved.id)
        request.categoryIds.forEach { catId ->
            val pc = ProductCategory(
                productId = saved.id,
                categoryId = catId,
                createdBy = username,
                createdDate = now,
                modifiedBy = username,
                modifiedDate = now
            )
            productCategoryRepository.save(pc)
        }

        return ApiResponse.success("Product updated", buildProductResponse(saved))
    }

    @Transactional
    fun delete(id: Long): ApiResponse<Nothing> {
        val username = SecurityUtils.getUsernameFromContext()
        val product = productRepository.findByIdAndDeletedDateIsNull(id)
            .orElseThrow { RuntimeException("Product not found") }

        product.deletedBy = username
        product.deletedDate = LocalDateTime.now()
        productRepository.save(product)

        return ApiResponse.success("Product deleted")
    }

    private fun buildProductResponse(product: Product): ProductResponse {
        val stock = stockRepository.findByProductId(product.id).orElse(null)
        val productCategories = productCategoryRepository.findByProductId(product.id)
        val tax = product.taxId?.let { taxRepository.findById(it).orElse(null) }
        val categories = productCategories.mapNotNull { pc ->
            categoryRepository.findById(pc.categoryId).orElse(null)?.let {
                CategoryResponse(
                    id = it.id,
                    merchantId = it.merchantId,
                    name = it.name,
                    image = it.image,
                    description = it.description,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate,
                    modifiedBy = it.modifiedBy,
                    modifiedDate = it.modifiedDate
                )
            }
        }
        val images = productImageRepository.findByProductId(product.id).map {
            ProductImageResponse(
                id = it.id,
                filename = it.filename,
                ext = it.ext,
                isMain = it.isMain
            )
        }

        return ProductResponse(
            id = product.id,
            merchantId = product.merchantId,
            name = product.name,
            price = product.price,
            sku = product.sku,
            upc = product.upc,
            imageUrl = product.imageUrl,
            imageThumbUrl = product.imageThumbUrl,
            description = product.description,
            stockMode = product.stockMode,
            basePrice = product.basePrice,
            isTaxable = product.isTaxable,
            taxId = product.taxId,
            tax = tax?.let {
                ProductTaxResponse(
                    id = it.id,
                    name = it.name,
                    percentage = it.percentage,
                    isActive = it.isActive,
                    isDefault = it.isDefault
                )
            },
            qty = stock?.qty ?: 0,
            categories = categories,
            images = images,
            createdBy = product.createdBy,
            createdDate = product.createdDate,
            modifiedBy = product.modifiedBy,
            modifiedDate = product.modifiedDate
        )
    }
}

package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.ProductRequest
import id.nivorapos.pos_service.dto.request.UpdateProductRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.PagedResponse
import id.nivorapos.pos_service.dto.response.ProductResponse
import id.nivorapos.pos_service.service.ProductService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/pos/product")
class ProductController(
    private val productService: ProductService
) {

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam size: Int,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false) upc: String?,
        @RequestParam(required = false) sku: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?
    ): ResponseEntity<PagedResponse<ProductResponse>> {
        return ResponseEntity.ok(
            productService.list(page, size, categoryId, keyword, startDate?.atStartOfDay(), endDate?.atTime(23, 59, 59), upc, sku, sortBy, sortDir)
        )
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    fun detail(@PathVariable id: Long): ResponseEntity<ApiResponse<ProductResponse>> {
        return try {
            ResponseEntity.ok(productService.detail(id))
        } catch (e: Exception) {
            ResponseEntity.status(404).body(ApiResponse.error(e.message ?: "Not found"))
        }
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    fun add(@RequestBody request: ProductRequest): ResponseEntity<ApiResponse<ProductResponse>> {
        return try {
            ResponseEntity.ok(productService.add(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to create product"))
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    fun update(@RequestBody request: UpdateProductRequest): ResponseEntity<ApiResponse<ProductResponse>> {
        return try {
            ResponseEntity.ok(productService.update(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to update product"))
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            ResponseEntity.ok(productService.delete(id))
        } catch (e: Exception) {
            ResponseEntity.status(404).body(ApiResponse.error(e.message ?: "Not found"))
        }
    }
}

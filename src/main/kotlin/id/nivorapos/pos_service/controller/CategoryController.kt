package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.CategoryRequest
import id.nivorapos.pos_service.dto.request.UpdateCategoryRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.CategoryResponse
import id.nivorapos.pos_service.dto.response.PagedResponse
import id.nivorapos.pos_service.service.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/category")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PagedResponse<CategoryResponse>> {
        return ResponseEntity.ok(categoryService.list(page, size))
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    fun detail(@PathVariable id: Long): ResponseEntity<ApiResponse<CategoryResponse>> {
        return try {
            ResponseEntity.ok(categoryService.detail(id))
        } catch (e: Exception) {
            ResponseEntity.status(404).body(ApiResponse.error(e.message ?: "Not found"))
        }
    }

    @PostMapping("/single/add")
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    fun add(@RequestBody request: CategoryRequest): ResponseEntity<ApiResponse<CategoryResponse>> {
        return try {
            ResponseEntity.ok(categoryService.add(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to create category"))
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('CATEGORY_EDIT')")
    fun update(@RequestBody request: UpdateCategoryRequest): ResponseEntity<ApiResponse<CategoryResponse>> {
        return try {
            ResponseEntity.ok(categoryService.update(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to update category"))
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            ResponseEntity.ok(categoryService.delete(id))
        } catch (e: Exception) {
            ResponseEntity.status(404).body(ApiResponse.error(e.message ?: "Not found"))
        }
    }
}

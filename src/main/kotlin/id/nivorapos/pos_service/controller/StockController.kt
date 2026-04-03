package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.StockUpdateRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.PagedResponse
import id.nivorapos.pos_service.dto.response.StockMovementResponse
import id.nivorapos.pos_service.service.StockService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/pos")
class StockController(
    private val stockService: StockService
) {

    @PutMapping("/stock/update")
    @PreAuthorize("hasAuthority('STOCK_UPDATE')")
    fun updateStock(@RequestBody request: StockUpdateRequest): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            ResponseEntity.ok(stockService.updateStock(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to update stock"))
        }
    }

    @GetMapping("/stock-movement/product/list")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    fun stockMovementList(
        @RequestParam productId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<PagedResponse<StockMovementResponse>> {
        return ResponseEntity.ok(stockService.stockMovementList(productId, startDate, endDate))
    }
}

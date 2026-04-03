package id.nivorapos.pos_service.service

import id.nivorapos.pos_service.dto.request.StockUpdateRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.PagedResponse
import id.nivorapos.pos_service.dto.response.StockMovementResponse
import id.nivorapos.pos_service.entity.StockMovement
import id.nivorapos.pos_service.repository.StockMovementRepository
import id.nivorapos.pos_service.repository.StockRepository
import id.nivorapos.pos_service.security.SecurityUtils
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class StockService(
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    @Transactional
    fun updateStock(request: StockUpdateRequest): ApiResponse<Map<String, Any>> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        val stock = stockRepository.findByProductId(request.productId)
            .orElseThrow { RuntimeException("Stock not found for product ${request.productId}") }

        val previousQty = stock.qty
        when (request.updateType.uppercase()) {
            "ADD" -> stock.qty += request.qty
            "REDUCE" -> {
                if (stock.qty < request.qty) {
                    throw RuntimeException("Insufficient stock")
                }
                stock.qty -= request.qty
            }
            else -> throw RuntimeException("Invalid updateType. Must be ADD or REDUCE")
        }
        stock.modifiedBy = username
        stock.modifiedDate = now
        stockRepository.save(stock)

        val movement = StockMovement(
            productId = request.productId,
            merchantId = merchantId,
            qty = request.qty,
            movementType = request.updateType.uppercase(),
            movementReason = "MANUAL_UPDATE",
            createdBy = username,
            createdDate = now,
            modifiedBy = username,
            modifiedDate = now
        )
        stockMovementRepository.save(movement)

        return ApiResponse.success(
            "Stock updated",
            mapOf("productId" to request.productId, "previousQty" to previousQty, "currentQty" to stock.qty)
        )
    }

    fun stockMovementList(
        productId: Long,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): PagedResponse<StockMovementResponse> {
        val start = startDate ?: LocalDateTime.of(2000, 1, 1, 0, 0)
        val end = endDate ?: LocalDateTime.now().plusDays(1)
        val pageable = PageRequest.of(0, 1000)

        val result = stockMovementRepository.findByProductIdAndCreatedDateBetween(
            productId, start, end, pageable
        )

        return PagedResponse(
            message = "Stock movement list retrieved",
            data = result.content.map { it.toResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    private fun StockMovement.toResponse() = StockMovementResponse(
        id = id,
        productId = productId,
        merchantId = merchantId,
        outletId = outletId,
        referenceId = referenceId,
        qty = qty,
        movementType = movementType,
        movementReason = movementReason,
        note = note,
        createdBy = createdBy,
        createdDate = createdDate
    )
}

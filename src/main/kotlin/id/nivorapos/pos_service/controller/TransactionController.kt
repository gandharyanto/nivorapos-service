package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.InitiatePaymentRequest
import id.nivorapos.pos_service.dto.request.TransactionRequest
import id.nivorapos.pos_service.dto.request.TransactionUpdateRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.PagedResponse
import id.nivorapos.pos_service.dto.response.TransactionDetailResponse
import id.nivorapos.pos_service.dto.response.TransactionListResponse
import id.nivorapos.pos_service.service.TransactionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/pos/transaction")
class TransactionController(
    private val transactionService: TransactionService
) {

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    ): ResponseEntity<PagedResponse<TransactionListResponse>> {
        return ResponseEntity.ok(transactionService.list(page, size, startDate?.atStartOfDay(), endDate?.atTime(23, 59, 59)))
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW')")
    fun detail(@PathVariable id: Long): ResponseEntity<ApiResponse<TransactionDetailResponse>> {
        return try {
            ResponseEntity.ok(transactionService.detail(id))
        } catch (e: Exception) {
            ResponseEntity.status(404).body(ApiResponse.error(e.message ?: "Not found"))
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('TRANSACTION_CREATE')")
    fun create(@RequestBody request: TransactionRequest): ResponseEntity<ApiResponse<TransactionDetailResponse>> {
        return try {
            ResponseEntity.ok(transactionService.create(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to create transaction"))
        }
    }

    @PutMapping("/update", "/update/")
    @PreAuthorize("hasAuthority('TRANSACTION_UPDATE')")
    fun update(
        @RequestBody request: TransactionUpdateRequest
    ): ResponseEntity<ApiResponse<TransactionDetailResponse>> {
        return try {
            ResponseEntity.ok(transactionService.update(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to update transaction"))
        }
    }

    @PutMapping("/initiate-payment/{merchantTrxId}")
    @PreAuthorize("hasAuthority('TRANSACTION_UPDATE')")
    fun initiatePayment(
        @PathVariable merchantTrxId: String,
        @RequestBody request: InitiatePaymentRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            ResponseEntity.ok(transactionService.initiatePayment(merchantTrxId, request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to initiate payment"))
        }
    }

    @PutMapping("/update/{merchantTrxId}")
    @PreAuthorize("hasAuthority('TRANSACTION_UPDATE')")
    fun updateByPath(
        @PathVariable merchantTrxId: String,
        @RequestBody request: TransactionUpdateRequest
    ): ResponseEntity<ApiResponse<TransactionDetailResponse>> {
        return try {
            val merged = request.copy(code = request.code ?: merchantTrxId)
            ResponseEntity.ok(transactionService.update(merged))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to update transaction"))
        }
    }
}

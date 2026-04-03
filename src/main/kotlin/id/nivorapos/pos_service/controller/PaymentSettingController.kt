package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.PaymentSettingRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.PaymentMethodListResponse
import id.nivorapos.pos_service.dto.response.PaymentSettingResponse
import id.nivorapos.pos_service.service.PaymentSettingService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos")
class PaymentSettingController(
    private val paymentSettingService: PaymentSettingService
) {

    @GetMapping("/payment-setting")
    @PreAuthorize("hasAuthority('PAYMENT_SETTING')")
    fun get(): ResponseEntity<ApiResponse<PaymentSettingResponse>> {
        return try {
            ResponseEntity.ok(paymentSettingService.get())
        } catch (e: Exception) {
            ResponseEntity.status(404).body(ApiResponse.error(e.message ?: "Not found"))
        }
    }

    @PostMapping("/payment-setting/create")
    @PreAuthorize("hasAuthority('PAYMENT_SETTING')")
    fun create(@RequestBody request: PaymentSettingRequest): ResponseEntity<ApiResponse<PaymentSettingResponse>> {
        return try {
            ResponseEntity.ok(paymentSettingService.create(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to create payment setting"))
        }
    }

    @PutMapping("/payment-setting/update")
    @PreAuthorize("hasAuthority('PAYMENT_SETTING')")
    fun update(@RequestBody request: PaymentSettingRequest): ResponseEntity<ApiResponse<PaymentSettingResponse>> {
        return try {
            ResponseEntity.ok(paymentSettingService.update(request))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to update payment setting"))
        }
    }

    @GetMapping("/payment-method/merchant/list")
    @PreAuthorize("hasAuthority('PAYMENT_SETTING')")
    fun paymentMethodList(): ResponseEntity<ApiResponse<PaymentMethodListResponse>> {
        return try {
            ResponseEntity.ok(paymentSettingService.getPaymentMethods())
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to retrieve payment methods"))
        }
    }
}

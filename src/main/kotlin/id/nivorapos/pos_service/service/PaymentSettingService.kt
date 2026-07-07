package id.nivorapos.pos_service.service

import id.nivorapos.pos_service.dto.request.PaymentSettingRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.PaymentMethodListResponse
import id.nivorapos.pos_service.dto.response.PaymentMethodResponse
import id.nivorapos.pos_service.dto.response.PaymentSettingResponse
import id.nivorapos.pos_service.entity.PaymentSetting
import id.nivorapos.pos_service.entity.Tax
import id.nivorapos.pos_service.repository.MerchantPaymentMethodRepository
import id.nivorapos.pos_service.repository.PaymentMethodRepository
import id.nivorapos.pos_service.repository.PaymentSettingRepository
import id.nivorapos.pos_service.repository.TaxRepository
import id.nivorapos.pos_service.security.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PaymentSettingService(
    private val paymentSettingRepository: PaymentSettingRepository,
    private val merchantPaymentMethodRepository: MerchantPaymentMethodRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val productService: ProductService,
    private val posMerchantDefaultsService: PosMerchantDefaultsService,
    private val taxRepository: TaxRepository
) {

    fun get(): ApiResponse<PaymentSettingResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        posMerchantDefaultsService.ensureForMerchant(merchantId, SecurityUtils.getUsernameFromContext())
        val setting = paymentSettingRepository.findByMerchantId(merchantId)
            .orElseThrow { RuntimeException("Payment setting not found for merchant $merchantId") }
        val defaultTax = requireDefaultTax(merchantId)
        return ApiResponse.success("Payment setting retrieved", setting.toResponse(defaultTax))
    }

    @Transactional
    fun create(request: PaymentSettingRequest): ApiResponse<PaymentSettingResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        posMerchantDefaultsService.ensureForMerchant(merchantId, username)
        val setting = paymentSettingRepository.findByMerchantId(merchantId).orElseGet {
            PaymentSetting(
                merchantId = merchantId,
                createdBy = username,
                createdDate = now
            )
        }
        val defaultTax = requireDefaultTax(merchantId)
        val merged = mergeRequest(setting, defaultTax, request)
        validateRequest(merged)
        applyRequest(setting, merged, username, now)
        applyTaxRequest(defaultTax, merged, username, now)
        val saved = paymentSettingRepository.save(setting)
        taxRepository.save(defaultTax)
        productService.recalculateMerchantPrices(merchantId)
        return ApiResponse.success("Payment setting saved", saved.toResponse(defaultTax))
    }

    @Transactional
    fun update(request: PaymentSettingRequest): ApiResponse<PaymentSettingResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        posMerchantDefaultsService.ensureForMerchant(merchantId, username)
        val setting = paymentSettingRepository.findByMerchantId(merchantId)
            .orElseThrow { RuntimeException("Payment setting not found") }

        val defaultTax = requireDefaultTax(merchantId)
        val merged = mergeRequest(setting, defaultTax, request)
        validateRequest(merged)
        applyRequest(setting, merged, username, now)
        applyTaxRequest(defaultTax, merged, username, now)

        val saved = paymentSettingRepository.save(setting)
        taxRepository.save(defaultTax)
        productService.recalculateMerchantPrices(merchantId)
        return ApiResponse.success("Payment setting updated", saved.toResponse(defaultTax))
    }

    /** posMerchantDefaultsService.ensureForMerchant() sudah menjamin ini ada sebelum dipanggil. */
    private fun requireDefaultTax(merchantId: Long): Tax =
        taxRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
            ?: throw RuntimeException("Default tax not found for merchant $merchantId")

    fun getPaymentMethods(): ApiResponse<PaymentMethodListResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        posMerchantDefaultsService.ensureForMerchant(merchantId, SecurityUtils.getUsernameFromContext())
        val merchantMethods = merchantPaymentMethodRepository.findByMerchantIdAndIsEnabledTrue(merchantId)

        val methodMap = paymentMethodRepository.findAll().associateBy { it.id }

        val allMethods = merchantMethods.mapNotNull { mpm ->
            val pm = methodMap[mpm.paymentMethodId] ?: return@mapNotNull null
            PaymentMethodResponse(
                id = pm.id,
                merchantPaymentMethodId = mpm.id,
                code = pm.code,
                name = pm.name,
                category = pm.category,
                paymentType = pm.paymentType,
                provider = pm.provider,
                isEnabled = mpm.isEnabled,
                displayOrder = mpm.displayOrder,
                configJson = mpm.configJson
            )
        }

        val internalPayments = allMethods.filter { pm ->
            pm.category == "INTERNAL" || pm.paymentType == "CASH"
        }
        val externalPayments = allMethods.filter { pm ->
            pm.category != "INTERNAL" && pm.paymentType != "CASH"
        }

        return ApiResponse.success(
            "Payment methods retrieved",
            PaymentMethodListResponse(
                internalPayments = internalPayments,
                externalPayments = externalPayments
            )
        )
    }

    private fun validateRequest(request: PaymentSettingRequest) {
        val isServiceCharge = request.isServiceCharge == true
        val serviceChargePercentage = request.serviceChargePercentage ?: java.math.BigDecimal.ZERO
        val serviceChargeAmount = request.serviceChargeAmount ?: java.math.BigDecimal.ZERO
        if (isServiceCharge) {
            val hasPct = serviceChargePercentage > java.math.BigDecimal.ZERO
            val hasAmt = serviceChargeAmount > java.math.BigDecimal.ZERO
            require(hasPct || hasAmt) {
                "serviceChargePercentage atau serviceChargeAmount wajib diisi jika isServiceCharge = true"
            }
            if (hasPct) {
                require(serviceChargePercentage >= java.math.BigDecimal("0.01") &&
                        serviceChargePercentage <= java.math.BigDecimal("100")) {
                    "serviceChargePercentage harus antara 0.01 dan 100"
                }
            }
            val validSources = listOf("BEFORE_TAX", "AFTER_TAX", "DPP", "AFTER_DISCOUNT")
            require(request.serviceChargeSource != null && request.serviceChargeSource.uppercase() in validSources) {
                "serviceChargeSource wajib diisi dengan BEFORE_TAX, AFTER_TAX, DPP, atau AFTER_DISCOUNT"
            }
        }
        val isRounding = request.isRounding == true
        val roundingTarget = request.roundingTarget ?: 0
        if (isRounding) {
            require(roundingTarget > 0) { "roundingTarget harus > 0 jika isRounding = true" }
            require(request.roundingType != null && request.roundingType.uppercase() in listOf("FLOOR", "CEIL", "ROUND")) {
                "roundingType harus FLOOR, CEIL, atau ROUND"
            }
        }
        if (request.isTax == true) {
            require(request.taxPercentage != null &&
                    request.taxPercentage >= java.math.BigDecimal("0.01") &&
                    request.taxPercentage <= java.math.BigDecimal("100")) {
                "taxPercentage harus antara 0.01 dan 100 jika isTax = true"
            }
            require(!request.taxName.isNullOrBlank()) { "taxName wajib diisi jika isTax = true" }
        }
    }

    private fun mergeRequest(setting: PaymentSetting, defaultTax: Tax, request: PaymentSettingRequest) = PaymentSettingRequest(
        isPriceIncludeTax = request.isPriceIncludeTax ?: setting.isPriceIncludeTax,
        isRounding = request.isRounding ?: setting.isRounding,
        roundingTarget = request.roundingTarget ?: setting.roundingTarget,
        roundingType = request.roundingType ?: setting.roundingType,
        isServiceCharge = request.isServiceCharge ?: setting.isServiceCharge,
        serviceChargePercentage = request.serviceChargePercentage ?: setting.serviceChargePercentage,
        serviceChargeAmount = request.serviceChargeAmount ?: setting.serviceChargeAmount,
        serviceChargeSource = request.serviceChargeSource ?: setting.serviceChargeSource,
        isTax = request.isTax ?: defaultTax.isActive,
        taxPercentage = request.taxPercentage ?: defaultTax.percentage,
        taxName = request.taxName ?: defaultTax.name
    )

    private fun applyRequest(
        setting: PaymentSetting,
        request: PaymentSettingRequest,
        username: String,
        now: LocalDateTime
    ) {
        setting.isPriceIncludeTax = request.isPriceIncludeTax ?: setting.isPriceIncludeTax
        setting.isRounding = request.isRounding ?: setting.isRounding
        setting.roundingTarget = request.roundingTarget ?: setting.roundingTarget
        setting.roundingType = request.roundingType ?: setting.roundingType
        setting.isServiceCharge = request.isServiceCharge ?: setting.isServiceCharge
        setting.serviceChargePercentage = request.serviceChargePercentage ?: setting.serviceChargePercentage
        setting.serviceChargeAmount = request.serviceChargeAmount ?: setting.serviceChargeAmount
        setting.serviceChargeSource = request.serviceChargeSource?.uppercase() ?: setting.serviceChargeSource
        setting.modifiedBy = username
        setting.modifiedDate = now
    }

    private fun applyTaxRequest(defaultTax: Tax, request: PaymentSettingRequest, username: String, now: LocalDateTime) {
        defaultTax.isActive = request.isTax ?: defaultTax.isActive
        defaultTax.percentage = request.taxPercentage ?: defaultTax.percentage
        defaultTax.name = request.taxName ?: defaultTax.name
        defaultTax.modifiedBy = username
        defaultTax.modifiedDate = now
    }

    private fun PaymentSetting.toResponse(defaultTax: Tax) = PaymentSettingResponse(
        id = id,
        merchantId = merchantId,
        isPriceIncludeTax = isPriceIncludeTax,
        isRounding = isRounding,
        roundingTarget = roundingTarget,
        roundingType = roundingType,
        isServiceCharge = isServiceCharge,
        serviceChargePercentage = serviceChargePercentage,
        serviceChargeAmount = serviceChargeAmount,
        serviceChargeSource = serviceChargeSource,
        isTax = defaultTax.isActive,
        taxPercentage = defaultTax.percentage,
        taxName = defaultTax.name,
        createdBy = createdBy,
        createdDate = createdDate,
        modifiedBy = modifiedBy,
        modifiedDate = modifiedDate
    )
}

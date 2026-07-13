package id.nivorapos.pos_service.dto.request

import java.math.BigDecimal

data class PaymentSettingRequest(
    val isPriceIncludeTax: Boolean? = null,
    val isRounding: Boolean? = null,
    val roundingTarget: Int? = null,
    val roundingType: String? = null,
    val isServiceCharge: Boolean? = null,
    val serviceChargePercentage: BigDecimal? = null,
    val serviceChargeAmount: BigDecimal? = null,
    /** BEFORE_DISCOUNT_BEFORE_TAX | AFTER_DISCOUNT_BEFORE_TAX | BEFORE_DISCOUNT_AFTER_TAX | AFTER_DISCOUNT_AFTER_TAX */
    val serviceChargeSource: String? = null,
    /** Mengedit tax default merchant (Tax.isDefault=true), bukan kolom di PaymentSetting sendiri. */
    val isTax: Boolean? = null,
    val taxPercentage: BigDecimal? = null,
    val taxName: String? = null
)

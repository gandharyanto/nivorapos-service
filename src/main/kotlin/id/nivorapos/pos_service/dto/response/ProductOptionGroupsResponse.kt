package id.nivorapos.pos_service.dto.response

import java.math.BigDecimal

data class OptionItemResponse(
    val optionId: Long,
    val variantId: Long?,
    val name: String,
    val priceAdjustment: BigDecimal,
    val qty: Int,
    val isUnlimitedStock: Boolean,
    val isOptional: Boolean,
    val displayOrder: Int
)

data class OptionGroupResponse(
    val groupId: Long,
    val name: String,
    val groupType: String,
    val isCombinationMember: Boolean,
    val selectionType: String,
    val isRequired: Boolean,
    val minSelection: Int,
    val maxSelection: Int,
    val options: List<OptionItemResponse>
)

data class ProductOptionGroupsData(
    val productId: Long,
    val productType: String,
    val isPriceAdjustable: Boolean,
    val variantGroups: List<OptionGroupResponse>,
    val modifierGroups: List<OptionGroupResponse>
)

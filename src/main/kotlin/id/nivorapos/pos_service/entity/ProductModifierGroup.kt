package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "product_modifier_group")
class ProductModifierGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_id")
    var productId: Long = 0,

    @Column(name = "name")
    var name: String = "",

    @Column(name = "is_required")
    var isRequired: Boolean = false,

    @Column(name = "min_select")
    var minSelect: Int = 0,

    @Column(name = "max_select")
    var maxSelect: Int = 1,

    @Column(name = "display_order")
    var displayOrder: Int = 0,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

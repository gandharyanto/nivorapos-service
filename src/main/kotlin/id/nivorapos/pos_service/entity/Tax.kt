package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "tax")
class Tax(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "merchant_id")
    var merchantId: Long = 0,

    @Column(name = "name")
    var name: String = "",

    @Column(name = "percentage", precision = 10, scale = 2)
    var percentage: BigDecimal = BigDecimal.ZERO,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "is_default")
    var isDefault: Boolean = false,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

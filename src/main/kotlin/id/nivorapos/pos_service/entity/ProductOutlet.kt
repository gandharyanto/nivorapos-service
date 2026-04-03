package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "product_outlet")
class ProductOutlet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_id")
    var productId: Long = 0,

    @Column(name = "outlet_id")
    var outletId: Long = 0,

    @Column(name = "outlet_price", precision = 19, scale = 2)
    var outletPrice: BigDecimal? = null,

    @Column(name = "stock_qty")
    var stockQty: Int = 0,

    @Column(name = "is_visible")
    var isVisible: Boolean = true,

    @Column(name = "can_standalone")
    var canStandalone: Boolean = true,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

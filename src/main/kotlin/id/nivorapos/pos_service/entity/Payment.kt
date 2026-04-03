package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "payment")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "transaction_id")
    var transactionId: Long = 0,

    @Column(name = "payment_trx_id")
    var paymentTrxId: String? = null,

    @Column(name = "payment_method")
    var paymentMethod: String? = null,

    @Column(name = "payment_source")
    var paymentSource: String? = null,

    @Column(name = "amount_paid", precision = 19, scale = 2)
    var amountPaid: BigDecimal = BigDecimal.ZERO,

    @Column(name = "status")
    var status: String = "PENDING",

    @Column(name = "is_effective")
    var isEffective: Boolean = false,

    @Column(name = "payment_reference")
    var paymentReference: String? = null,

    @Column(name = "payment_date")
    var paymentDate: LocalDateTime? = null,

    @Column(name = "payment_snapshot", columnDefinition = "text")
    var paymentSnapshot: String? = null,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

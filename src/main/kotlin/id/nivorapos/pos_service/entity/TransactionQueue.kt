package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "transaction_queue")
class TransactionQueue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "merchant_id")
    var merchantId: Long = 0,

    @Column(name = "outlet_id")
    var outletId: Long? = null,

    @Column(name = "queue_number")
    var queueNumber: String = "",

    @Column(name = "queue_date")
    var queueDate: LocalDate? = null,

    @Column(name = "status")
    var status: String = "ACTIVE",

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

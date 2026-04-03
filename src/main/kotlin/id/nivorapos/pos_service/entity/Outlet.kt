package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "outlet")
class Outlet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "merchant_id")
    var merchantId: Long? = null,

    @Column(name = "code")
    var code: String? = null,

    @Column(name = "name")
    var name: String? = null,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "is_default")
    var isDefault: Boolean = false,

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

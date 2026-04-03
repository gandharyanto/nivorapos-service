package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "merchant_role_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["merchant_id", "role_id", "permission_id"])]
)
class MerchantRolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "merchant_id")
    var merchantId: Long = 0,

    @Column(name = "role_id")
    var roleId: Long = 0,

    @Column(name = "permission_id")
    var permissionId: Long = 0,

    @Column(name = "is_granted")
    var isGranted: Boolean = true,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

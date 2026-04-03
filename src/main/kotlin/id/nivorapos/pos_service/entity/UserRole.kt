package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_roles")
class UserRole(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id")
    var userId: Long = 0,

    @Column(name = "role_id")
    var roleId: Long = 0,

    @Column(name = "scope_level")
    var scopeLevel: String? = null,

    @Column(name = "scope_id")
    var scopeId: Long? = null,

    @Column(name = "application_type")
    var applicationType: String? = null,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

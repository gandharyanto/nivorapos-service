package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "full_name")
    var fullName: String? = null,

    @Column(name = "username", unique = true)
    var username: String = "",

    @Column(name = "password")
    var password: String = "",

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "employee_code")
    var employeeCode: String? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "is_system")
    var isSystem: Boolean = false,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

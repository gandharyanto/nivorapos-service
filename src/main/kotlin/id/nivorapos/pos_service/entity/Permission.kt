package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "permissions")
class Permission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "code")
    var code: String = "",

    @Column(name = "name")
    var name: String = "",

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "menu_key")
    var menuKey: String? = null,

    @Column(name = "menu_label")
    var menuLabel: String? = null,

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_by")
    var modifiedBy: String? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

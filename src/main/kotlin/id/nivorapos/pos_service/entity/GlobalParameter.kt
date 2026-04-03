package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "global_parameter")
class GlobalParameter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "param_group")
    var paramGroup: String = "",

    @Column(name = "param_name")
    var paramName: String = "",

    @Column(name = "param_value", columnDefinition = "text")
    var paramValue: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null,

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
)

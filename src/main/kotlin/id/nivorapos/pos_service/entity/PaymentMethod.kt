package id.nivorapos.pos_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "payment_method")
class PaymentMethod(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "code")
    var code: String = "",

    @Column(name = "name")
    var name: String = "",

    @Column(name = "category")
    var category: String? = null,

    @Column(name = "payment_type")
    var paymentType: String? = null,

    @Column(name = "provider")
    var provider: String? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)

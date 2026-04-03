package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.PaymentSetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PaymentSettingRepository : JpaRepository<PaymentSetting, Long> {
    fun findByMerchantId(merchantId: Long): Optional<PaymentSetting>
}

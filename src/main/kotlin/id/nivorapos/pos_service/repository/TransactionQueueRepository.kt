package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.TransactionQueue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TransactionQueueRepository : JpaRepository<TransactionQueue, Long> {
    fun findByMerchantIdAndQueueDate(merchantId: Long, queueDate: LocalDate): List<TransactionQueue>
    fun countByMerchantIdAndQueueDate(merchantId: Long, queueDate: LocalDate): Long
}

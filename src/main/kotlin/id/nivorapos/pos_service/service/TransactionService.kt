package id.nivorapos.pos_service.service

import tools.jackson.databind.ObjectMapper
import id.nivorapos.pos_service.dto.request.TransactionRequest
import id.nivorapos.pos_service.dto.request.TransactionUpdateRequest
import id.nivorapos.pos_service.dto.response.*
import id.nivorapos.pos_service.entity.*
import id.nivorapos.pos_service.repository.*
import id.nivorapos.pos_service.security.SecurityUtils
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val transactionQueueRepository: TransactionQueueRepository,
    private val paymentRepository: PaymentRepository,
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val objectMapper: ObjectMapper,
    private val entityManager: EntityManager
) {

    fun list(
        page: Int,
        size: Int,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        sortBy: String?,
        sortType: String?
    ): PagedResponse<TransactionListResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val direction = if (sortType?.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC
        val sortField = sortBy ?: "createdDate"
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortField))

        val start = startDate ?: LocalDateTime.of(2000, 1, 1, 0, 0)
        val end = endDate ?: LocalDateTime.now().plusDays(1)

        val result = transactionRepository.findByMerchantIdAndCreatedDateBetween(
            merchantId, start, end, pageable
        )

        return PagedResponse(
            message = "Transaction list retrieved",
            data = result.content.map { it.toListResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun detail(id: Long): ApiResponse<TransactionDetailResponse> {
        val transaction = transactionRepository.findById(id)
            .orElseThrow { RuntimeException("Transaction not found") }
        return ApiResponse.success("Transaction found", buildDetail(transaction))
    }

    @Transactional
    fun create(request: TransactionRequest): ApiResponse<TransactionDetailResponse> {
        val merchantId = SecurityUtils.getMerchantIdFromContext()
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        // Generate trx_id
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val random = String.format("%04d", Random().nextInt(10000))
        val trxId = "TRX-${now.format(formatter)}-$random"

        // Handle queue
        var queueId: Long? = null
        if (!request.queueNumber.isNullOrBlank()) {
            val count = transactionQueueRepository.countByMerchantIdAndQueueDate(merchantId, LocalDate.now())
            val queueNumber = "A${String.format("%03d", count + 1)}"
            val queue = TransactionQueue(
                merchantId = merchantId,
                outletId = request.outletId,
                queueNumber = queueNumber,
                queueDate = LocalDate.now(),
                status = "ACTIVE",
                createdBy = username,
                createdDate = now,
                modifiedBy = username,
                modifiedDate = now
            )
            queueId = transactionQueueRepository.save(queue).id
        }

        val isCash = request.paymentMethod?.uppercase() == "CASH"

        val transaction = Transaction(
            merchantId = merchantId,
            outletId = request.outletId,
            username = username,
            trxId = trxId,
            transactionOrigin = request.transactionOrigin,
            status = if (isCash) "PAID" else "PENDING",
            paymentMethod = request.paymentMethod,
            priceIncludeTax = request.priceIncludeTax,
            subTotal = parseBD(request.subTotal),
            totalAmount = parseBD(request.totalAmount),
            serviceChargePercentage = parseBD(request.serviceChargePercentage),
            serviceChargeAmount = parseBD(request.serviceChargeAmount),
            totalServiceCharge = parseBD(request.totalServiceCharge),
            taxPercentage = parseBD(request.taxPercentage),
            totalTax = parseBD(request.totalTax),
            taxName = request.taxName,
            totalRounding = parseBD(request.totalRounding),
            roundingType = request.roundingType,
            roundingTarget = request.roundingTarget,
            cashTendered = parseBD(request.cashTendered),
            cashChange = parseBD(request.cashChange),
            queueId = queueId,
            createdBy = username,
            createdDate = now,
            modifiedBy = username,
            modifiedDate = now
        )
        val savedTrx = transactionRepository.save(transaction)

        // Save items
        request.items.forEach { itemReq ->
            val product = productRepository.findByIdAndDeletedDateIsNull(itemReq.productId).orElse(null)
            val itemPrice = parseBD(itemReq.price)
            val totalPrice = itemPrice.multiply(BigDecimal(itemReq.qty))
            val snapshot = if (product != null) objectMapper.writeValueAsString(product) else null

            val item = TransactionItem(
                transactionId = savedTrx.id,
                productId = itemReq.productId,
                productName = product?.name ?: "",
                price = itemPrice,
                qty = itemReq.qty,
                totalPrice = totalPrice,
                productSnapshot = snapshot,
                taxId = itemReq.taxId,
                createdBy = username,
                createdDate = now,
                modifiedBy = username,
                modifiedDate = now
            )
            transactionItemRepository.save(item)

            // Reduce stock
            stockRepository.findByProductId(itemReq.productId).ifPresent { stock ->
                if (stock.qty >= itemReq.qty) {
                    stock.qty -= itemReq.qty
                    stock.modifiedBy = username
                    stock.modifiedDate = now
                    stockRepository.save(stock)

                    val movement = StockMovement(
                        productId = itemReq.productId,
                        merchantId = merchantId,
                        outletId = request.outletId,
                        referenceId = savedTrx.id,
                        qty = itemReq.qty,
                        movementType = "REDUCE",
                        movementReason = "TRANSACTION",
                        createdBy = username,
                        createdDate = now,
                        modifiedBy = username,
                        modifiedDate = now
                    )
                    stockMovementRepository.save(movement)
                }
            }
        }

        // Save payment record
        val payment = Payment(
            transactionId = savedTrx.id,
            paymentMethod = request.paymentMethod,
            paymentSource = request.paymentSource,
            amountPaid = parseBD(request.totalAmount),
            status = if (isCash) "PAID" else "PENDING",
            isEffective = isCash,
            paymentDate = if (isCash) now else null,
            paymentReference = request.paymentReference,
            createdBy = username,
            createdDate = now,
            modifiedBy = username,
            modifiedDate = now
        )
        paymentRepository.save(payment)

        entityManager.flush()
        return ApiResponse.success("Transaction created", buildDetail(savedTrx))
    }

    @Transactional
    fun update(merchantTrxId: String, request: TransactionUpdateRequest): ApiResponse<TransactionDetailResponse> {
        val username = SecurityUtils.getUsernameFromContext()
        val now = LocalDateTime.now()

        val transaction = transactionRepository.findByTrxId(merchantTrxId)
            .orElseThrow { RuntimeException("Transaction not found: $merchantTrxId") }

        transaction.status = request.status
        transaction.modifiedBy = username
        transaction.modifiedDate = now
        transactionRepository.save(transaction)

        // Update payment
        val payments = paymentRepository.findByTransactionId(transaction.id)
        if (payments.isNotEmpty()) {
            val payment = payments.first()
            if (request.paymentStatus != null) payment.status = request.paymentStatus
            if (request.paymentReference != null) payment.paymentReference = request.paymentReference
            if (request.paymentTrxId != null) payment.paymentTrxId = request.paymentTrxId
            if (request.paymentStatus == "PAID" || request.paymentStatus == "SUCCESS") {
                payment.isEffective = true
                payment.paymentDate = now
            }
            payment.modifiedBy = username
            payment.modifiedDate = now
            paymentRepository.save(payment)
        }

        return ApiResponse.success("Transaction updated", buildDetail(transaction))
    }

    private fun buildDetail(transaction: Transaction): TransactionDetailResponse {
        val queueNumber = transaction.queueId?.let {
            transactionQueueRepository.findById(it).orElse(null)?.queueNumber
        }
        val items = transactionItemRepository.findByTransactionId(transaction.id).map {
            TransactionItemResponse(
                id = it.id,
                transactionId = it.transactionId,
                productId = it.productId,
                productName = it.productName,
                price = it.price,
                qty = it.qty,
                totalPrice = it.totalPrice,
                taxId = it.taxId,
                taxName = it.taxName,
                taxPercentage = it.taxPercentage,
                taxAmount = it.taxAmount,
                createdDate = it.createdDate
            )
        }
        val payments = paymentRepository.findByTransactionId(transaction.id).map {
            PaymentResponse(
                id = it.id,
                transactionId = it.transactionId,
                paymentTrxId = it.paymentTrxId,
                paymentMethod = it.paymentMethod,
                paymentSource = it.paymentSource,
                amountPaid = it.amountPaid,
                status = it.status,
                isEffective = it.isEffective,
                paymentReference = it.paymentReference,
                paymentDate = it.paymentDate,
                createdDate = it.createdDate
            )
        }
        return TransactionDetailResponse(
            id = transaction.id,
            merchantId = transaction.merchantId,
            outletId = transaction.outletId,
            merchantUniqueCode = transaction.merchantUniqueCode,
            username = transaction.username,
            trxId = transaction.trxId,
            transactionOrigin = transaction.transactionOrigin,
            status = transaction.status,
            paymentMethod = transaction.paymentMethod,
            priceIncludeTax = transaction.priceIncludeTax,
            subTotal = transaction.subTotal,
            totalAmount = transaction.totalAmount,
            serviceChargePercentage = transaction.serviceChargePercentage,
            serviceChargeAmount = transaction.serviceChargeAmount,
            totalServiceCharge = transaction.totalServiceCharge,
            taxPercentage = transaction.taxPercentage,
            totalTax = transaction.totalTax,
            taxName = transaction.taxName,
            totalRounding = transaction.totalRounding,
            roundingType = transaction.roundingType,
            roundingTarget = transaction.roundingTarget,
            cashTendered = transaction.cashTendered,
            cashChange = transaction.cashChange,
            queueId = transaction.queueId,
            queueNumber = queueNumber,
            createdDate = transaction.createdDate,
            modifiedDate = transaction.modifiedDate,
            items = items,
            payments = payments
        )
    }

    private fun Transaction.toListResponse() = TransactionListResponse(
        id = id,
        merchantId = merchantId,
        trxId = trxId,
        status = status,
        paymentMethod = paymentMethod,
        subTotal = subTotal,
        totalAmount = totalAmount,
        totalTax = totalTax,
        totalRounding = totalRounding,
        cashTendered = cashTendered,
        cashChange = cashChange,
        username = username,
        createdDate = createdDate
    )

    private fun parseBD(value: String?): BigDecimal {
        return try { BigDecimal(value ?: "0") } catch (e: Exception) { BigDecimal.ZERO }
    }
}

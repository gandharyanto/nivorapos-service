package id.nivorapos.pos_service.security

import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {

    fun getMerchantIdFromContext(): Long {
        val auth = SecurityContextHolder.getContext().authentication ?: return 0L
        val details = auth.details
        if (details is Map<*, *>) {
            val merchantId = details["merchantId"]
            return when (merchantId) {
                is Long -> merchantId
                is Int -> merchantId.toLong()
                is Number -> merchantId.toLong()
                else -> 0L
            }
        }
        return 0L
    }

    fun getUsernameFromContext(): String {
        return SecurityContextHolder.getContext().authentication?.name ?: ""
    }
}

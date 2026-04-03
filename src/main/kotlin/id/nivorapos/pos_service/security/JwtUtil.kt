package id.nivorapos.pos_service.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var expiration: Long = 86400000L

    private fun getSigningKey(): SecretKey {
        val keyBytes = secret.toByteArray(Charsets.UTF_8)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(username: String, merchantId: Long?): String {
        val claims = mutableMapOf<String, Any>()
        if (merchantId != null) {
            claims["merchantId"] = merchantId
        }
        return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact()
    }

    fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    fun extractMerchantId(token: String): Long? {
        val claims = extractAllClaims(token)
        val value = claims["merchantId"] ?: return null
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Number -> value.toLong()
            else -> null
        }
    }

    fun validateToken(token: String, username: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            claims.subject == username && !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }
}

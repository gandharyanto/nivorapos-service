package id.nivorapos.pos_service.dto.response

data class LoginResponse(
    val token: String,
    val username: String,
    val fullName: String?,
    val merchantId: Long?
)

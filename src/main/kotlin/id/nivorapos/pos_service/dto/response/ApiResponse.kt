package id.nivorapos.pos_service.dto.response

data class ApiResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(message: String, data: T? = null) = ApiResponse(
            status = "SUCCESS",
            message = message,
            data = data
        )

        fun <T> error(message: String): ApiResponse<T> = ApiResponse(
            status = "ERROR",
            message = message,
            data = null
        )
    }
}

package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.request.LoginRequest
import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.LoginResponse
import id.nivorapos.pos_service.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val result = authService.login(request)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(401).body(ApiResponse.error(e.message ?: "Login failed"))
        }
    }
}

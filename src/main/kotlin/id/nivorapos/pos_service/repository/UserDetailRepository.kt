package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.UserDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserDetailRepository : JpaRepository<UserDetail, Long> {
    fun findByUsername(username: String): Optional<UserDetail>
    fun existsByUsername(username: String): Boolean
}

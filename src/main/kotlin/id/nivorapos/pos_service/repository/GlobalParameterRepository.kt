package id.nivorapos.pos_service.repository

import id.nivorapos.pos_service.entity.GlobalParameter
import org.springframework.data.jpa.repository.JpaRepository

interface GlobalParameterRepository : JpaRepository<GlobalParameter, Long> {
    fun existsByParamGroupAndParamName(paramGroup: String, paramName: String): Boolean
}

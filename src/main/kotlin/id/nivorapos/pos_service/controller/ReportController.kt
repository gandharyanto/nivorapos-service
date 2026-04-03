package id.nivorapos.pos_service.controller

import id.nivorapos.pos_service.dto.response.ApiResponse
import id.nivorapos.pos_service.dto.response.SummaryReportResponse
import id.nivorapos.pos_service.service.ReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/pos/summary-report")
class ReportController(
    private val reportService: ReportService
) {

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    fun summaryReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<ApiResponse<SummaryReportResponse>> {
        return try {
            ResponseEntity.ok(reportService.summaryReport(startDate, endDate))
        } catch (e: Exception) {
            ResponseEntity.status(400).body(ApiResponse.error(e.message ?: "Failed to retrieve report"))
        }
    }
}

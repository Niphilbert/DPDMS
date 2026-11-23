package com.itzetech.dpdms.report;

import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.report.ReportDtos.Dashboard;
import com.itzetech.dpdms.report.ReportDtos.MonthRow;
import com.itzetech.dpdms.report.ReportDtos.Summary;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
class ReportController {

    private final ReportService reports;
    private final DashboardService dashboard;

    ReportController(ReportService reports, DashboardService dashboard) {
        this.reports = reports;
        this.dashboard = dashboard;
    }

    @GetMapping("/api/reports/summary")
    Summary summary(@AuthenticationPrincipal AuthUser me,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.summary(me, from, to);
    }

    @GetMapping("/api/reports/summary.csv")
    ResponseEntity<byte[]> summaryCsv(@AuthenticationPrincipal AuthUser me,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.summaryCsv(me, from, to);
    }

    @GetMapping("/api/reports/revenue-by-month")
    List<MonthRow> revenueByMonth(@AuthenticationPrincipal AuthUser me, @RequestParam(defaultValue = "12") int months) {
        if (months < 1 || months > 36) {
            throw ApiException.badRequest("BAD_REQUEST", "The value for \"months\" is not valid.");
        }
        return reports.revenueByMonth(me, months);
    }

    @GetMapping("/api/dashboard/stage3")
    Dashboard dashboard(@AuthenticationPrincipal AuthUser me) {
        return dashboard.forUser(me);
    }
}

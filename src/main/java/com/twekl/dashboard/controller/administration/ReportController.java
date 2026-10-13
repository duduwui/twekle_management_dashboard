package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.service.administration.ReportService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("@portalAccess.allows('REPORTS_INSIGHTS','READ')")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public String report(
            @RequestParam(defaultValue = "ALL") String period,
            @RequestParam(defaultValue = "milestones") String tab,
            @RequestParam(defaultValue = "ALL") String satisfaction,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("rows", reports.report(tab, period, satisfaction, page));
        model.addAttribute("activeTab", "reports");
        model.addAttribute("reportSubTab", tab);
        model.addAttribute("period", period);
        model.addAttribute("satisfaction", satisfaction);
        return "administration/report/reports";
    }

    /** A file download is a normal MVC response, not a JSON fallback. Each batch is bounded. */
    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "ALL") String period, HttpServletResponse response)
            throws IOException {
        var batch = reports.exportBatch(period, "");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"satisfaction.csv\"");
        var writer = response.getWriter();
        List<String> columns =
                List.of(
                        "id",
                        "customer",
                        "phone",
                        "orderNumber",
                        "milestone",
                        "satisfaction",
                        "notes",
                        "date");
        writer.println(String.join(",", columns));
        while (!batch.isEmpty()) {
            for (var row : batch) {
                writer.println(
                        columns.stream()
                                .map(key -> csv(row.get(key)))
                                .collect(java.util.stream.Collectors.joining(",")));
            }
            writer.flush();
            if (writer.checkError() || batch.size() < 500) break;
            batch = reports.exportBatch(period, batch.getLast().get("id").toString());
        }
    }

    static String csv(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.stripLeading().matches("^[=+@\\-].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}

package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.repository.administration.PortalQueryRepository;
import com.twekl.dashboard.util.administration.DateUtil;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final PortalQueryRepository queries;

    public ReportService(PortalQueryRepository queries) {
        this.queries = queries;
    }

    public List<Map<String, Object>> exportBatch(String period, String after) {
        return queries.exportRatings(DateUtil.getFilterCutoff(period), after);
    }

    public Page<Map<String, Object>> report(
            String tab, String period, String satisfaction, int page) {
        if (!Set.of("milestones", "followups", "products", "satisfaction").contains(tab))
            throw new IllegalArgumentException("Unknown report tab");
        if (!Set.of("ALL", "SATISFIED", "NEUTRAL", "UNSATISFIED").contains(satisfaction))
            throw new IllegalArgumentException("Unknown satisfaction filter");
        return queries.report(tab, DateUtil.getFilterCutoff(period), satisfaction, page);
    }
}

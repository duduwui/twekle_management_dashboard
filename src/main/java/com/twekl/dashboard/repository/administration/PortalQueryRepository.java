package com.twekl.dashboard.repository.administration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import javax.sql.DataSource;

/**
 * SQL projections for portal pages. Historical entities and image bodies never enter directory
 * results.
 */
@Repository
public class PortalQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PortalQueryRepository(DataSource source) {
        this.jdbc = new NamedParameterJdbcTemplate(source);
        this.jdbc.getJdbcTemplate().setQueryTimeout(10);
    }

    // Prefer the foreign key; name matching is only for records predating PRESET_ID.
    private static final String ACTIVE =
            """
            EXISTS (SELECT 1 FROM TIME_FILTER_PRESETS p WHERE p.ACTIVE = TRUE
              AND (p.ID = f.PRESET_ID OR (f.PRESET_ID IS NULL
                AND (LOWER(TRIM(f.MILESTONE_NAME)) = LOWER(p.LABEL_EN)
                  OR LOWER(TRIM(f.MILESTONE_NAME)) = LOWER(p.PRESET_KEY)))))
            """;
    private static final String CHECKS =
            """
            WITH checks AS (
              SELECT f.ID, f.ORDER_ID, o.CUSTOMER_ID, f.PRESET_ID, f.MILESTONE_NAME,
                f.COMPLETED, CASE f.SATISFACTION WHEN '1' THEN 'UNSATISFIED' WHEN '2' THEN 'UNSATISFIED'
                  WHEN '3' THEN 'NEUTRAL' WHEN '4' THEN 'SATISFIED' WHEN '5' THEN 'SATISFIED'
                  ELSE f.SATISFACTION END AS SATISFACTION, f.NOTES, f.CHECKED_BY,
                (f.IMAGE_URL IS NOT NULL AND f.IMAGE_URL <> '') AS hasImage,
                COALESCE(f.CHECKED_AT, f.UPDATED_AT, f.COMPLETED_DATE, o.ORDER_DATE) AS checkDate,
                TIMESTAMPADD(MINUTE, GREATEST(COALESCE(f.DURATION_VALUE, 1), 1) *
                  CASE f.DURATION_UNIT WHEN 'HOURS' THEN 60 WHEN 'WEEKS' THEN 10080
                  WHEN 'MONTHS' THEN 43200 ELSE 1440 END, o.ORDER_DATE) AS dueDate
              FROM ORDER_FOLLOWUP_CHECKS f JOIN CUSTOMER_ORDERS o ON o.ID = f.ORDER_ID
              WHERE %s
            )
            """
                    .formatted(ACTIVE);
    private static final String DIRECTORY =
            CHECKS
                    + """
                    , pending AS (
                      SELECT CUSTOMER_ID, COUNT(*) AS pendingCount, MIN(dueDate) AS nextDue
                      FROM checks WHERE COMPLETED = FALSE GROUP BY CUSTOMER_ID
                    ), orders AS (
                      SELECT CUSTOMER_ID, COUNT(*) AS orderCount, SUM(TOTAL_AMOUNT) AS spent,
                        MAX(ORDER_DATE) AS lastOrder FROM CUSTOMER_ORDERS GROUP BY CUSTOMER_ID
                    ), directory AS (
                      SELECT c.ID AS id, c.NAME AS name, c.PHONE_NUMBER AS phone, c.CITY AS city,
                        c.CREATED_AT AS registered, COALESCE(o.orderCount,0) AS orderCount,
                        COALESCE(o.spent,0) AS spent, o.lastOrder, COALESCE(p.pendingCount,0) AS pendingCount,
                        p.nextDue, CASE WHEN p.nextDue < :now THEN 'ALERT'
                          WHEN p.pendingCount > 0 OR o.orderCount IS NULL THEN 'IDLE' ELSE 'DONE' END AS status
                      FROM CUSTOMERS c LEFT JOIN orders o ON o.CUSTOMER_ID = c.ID
                      LEFT JOIN pending p ON p.CUSTOMER_ID = c.ID
                    )
                    """;

    private static final String RATINGS =
            """
            SELECT * FROM (
              SELECT CONCAT('check-',f.ID) AS id, c.NAME AS customer, c.PHONE_NUMBER AS phone,
                o.ORDER_NUMBER AS orderNumber, f.ORDER_ID AS orderId, f.MILESTONE_NAME AS milestone,
                f.SATISFACTION AS satisfaction, f.NOTES AS notes, f.checkDate AS date
              FROM checks f JOIN CUSTOMERS c ON c.ID = f.CUSTOMER_ID
                JOIN CUSTOMER_ORDERS o ON o.ID = f.ORDER_ID WHERE NULLIF(f.SATISFACTION,'') IS NOT NULL
              UNION ALL
              SELECT CONCAT('feedback-',f.ID), c.NAME, c.PHONE_NUMBER, o.ORDER_NUMBER, f.ORDER_ID,
                'Customer feedback', CASE WHEN f.RATING >= 4 THEN 'SATISFIED'
                WHEN f.RATING = 3 THEN 'NEUTRAL' ELSE 'UNSATISFIED' END, f.COMMENT, f.FEEDBACK_DATE
              FROM CUSTOMER_FEEDBACKS f JOIN CUSTOMERS c ON c.ID = f.CUSTOMER_ID
                LEFT JOIN CUSTOMER_ORDERS o ON o.ID = f.ORDER_ID
            ) ratings WHERE (:cutoff IS NULL OR date >= :cutoff)
              AND (:satisfaction = 'ALL' OR satisfaction = :satisfaction)
            """;

    public Page<Map<String, Object>> customers(
            String search, String status, Instant cutoff, int page) {
        Map<String, Object> args = parameters(cutoff);
        args.put(
                "search",
                "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        args.put("status", status);
        String select =
                """
                SELECT * FROM directory WHERE (name LIKE :search ESCAPE '!' OR phone LIKE :search ESCAPE '!')
                  AND (:status = 'ALL' OR status = :status) AND (:cutoff IS NULL OR registered >= :cutoff)
                """;
        return page(DIRECTORY, select, "ORDER BY nextDue IS NULL, nextDue, id", args, page);
    }

    public Page<Map<String, Object>> orders(long customerId, int page) {
        return page(
                "",
                "SELECT ID AS id, ORDER_NUMBER AS number, ITEMS_SUMMARY AS items, ORDER_DATE AS"
                        + " ordered, TOTAL_AMOUNT AS amount, ORDER_STATUS AS status FROM"
                        + " CUSTOMER_ORDERS WHERE CUSTOMER_ID = :id",
                "ORDER BY ordered DESC, id DESC",
                new HashMap<>(Map.of("id", customerId)),
                page);
    }

    public Page<Map<String, Object>> feedback(long customerId, int page) {
        return page(
                "",
                "SELECT ID AS id, RATING AS rating, COMMENT AS comment, FEEDBACK_DATE AS date FROM"
                        + " CUSTOMER_FEEDBACKS WHERE CUSTOMER_ID = :id",
                "ORDER BY date DESC, id DESC",
                new HashMap<>(Map.of("id", customerId)),
                page);
    }

    public List<Map<String, Object>> priorityCustomers() {
        return jdbc.queryForList(
                DIRECTORY
                        + "SELECT * FROM directory WHERE pendingCount > 0 ORDER BY nextDue IS NULL,"
                        + " nextDue, id LIMIT 10",
                parameters(null));
    }

    public Page<Map<String, Object>> checklist(long orderId, int page) {
        Map<String, Object> args = parameters(null);
        args.put("orderId", orderId);
        return page(
                "",
                """
                SELECT f.ID AS id, f.MILESTONE_NAME AS milestoneName, f.COMPLETED AS completed,
                    f.NOTES AS notes, CASE f.SATISFACTION WHEN '1' THEN 'UNSATISFIED' WHEN '2' THEN 'UNSATISFIED'
                        WHEN '3' THEN 'NEUTRAL' WHEN '4' THEN 'SATISFIED' WHEN '5' THEN 'SATISFIED'
                        ELSE f.SATISFACTION END AS satisfaction, f.CHECKED_BY AS checkedBy,
                    f.CHECKED_AT AS checkedAt, (NULLIF(f.IMAGE_URL,'') IS NOT NULL) AS hasImage,
                    TIMESTAMPDIFF(MINUTE,:now,TIMESTAMPADD(MINUTE,
                        GREATEST(COALESCE(f.DURATION_VALUE,1),1) * CASE f.DURATION_UNIT
                            WHEN 'HOURS' THEN 60 WHEN 'WEEKS' THEN 10080 WHEN 'MONTHS' THEN 43200 ELSE 1440 END,
                        o.ORDER_DATE)) AS minutesUntilDue
                FROM ORDER_FOLLOWUP_CHECKS f JOIN CUSTOMER_ORDERS o ON o.ID = f.ORDER_ID
                WHERE f.ORDER_ID = :orderId
                """,
                "ORDER BY minutesUntilDue, id",
                args,
                page);
    }

    public Map<String, Object> stats() {
        Map<String, Object> args = parameters(null);
        Map<String, Object> result =
                new HashMap<>(
                        jdbc.queryForMap(
                                """
                                SELECT COUNT(*) AS totalCustomers,
                                  COALESCE(SUM(CREATED_AT >= TIMESTAMPADD(DAY,-7,:now)),0) AS customersThisWeek,
                                  COALESCE(SUM(CREATED_AT >= TIMESTAMPADD(DAY,-30,:now)),0) AS customersThisMonth
                                FROM CUSTOMERS
                                """,
                                args));
        result.putAll(
                jdbc.queryForMap(
                        CHECKS
                                + """
                                SELECT COUNT(*) AS totalFollowups, COALESCE(SUM(COMPLETED),0) AS completedFollowups,
                                  COALESCE(SUM(NOT COMPLETED AND dueDate < :now),0) AS overdueFollowups,
                                  COALESCE(SUM(NOT COMPLETED AND (dueDate >= :now OR dueDate IS NULL)),0) AS inQueueFollowups
                                FROM checks
                                """,
                        args));
        result.put(
                "unsatisfiedCount",
                jdbc.queryForObject(
                        CHECKS
                                + """
                                SELECT (SELECT COUNT(*) FROM checks WHERE SATISFACTION IN ('UNSATISFIED','1','2'))
                                  + (SELECT COUNT(*) FROM CUSTOMER_FEEDBACKS WHERE RATING <= 2)
                                """,
                        args,
                        Long.class));
        double total = ((Number) result.get("totalFollowups")).doubleValue();
        for (String name : List.of("completed", "overdue", "inQueue")) {
            double count = ((Number) result.get(name + "Followups")).doubleValue();
            result.put(name + "Percentage", total == 0 ? 0 : count * 100 / total);
        }
        result.put(
                "completionPercentage",
                Math.round(((Number) result.get("completedPercentage")).doubleValue()));
        result.put(
                "pendingFollowups",
                ((Number) result.get("overdueFollowups")).longValue()
                        + ((Number) result.get("inQueueFollowups")).longValue());
        return result;
    }

    public Page<Map<String, Object>> report(
            String tab, Instant cutoff, String satisfaction, int page) {
        Map<String, Object> args = parameters(cutoff);
        args.put("satisfaction", satisfaction);
        String select =
                switch (tab) {
                    case "milestones" ->
                            """
                            SELECT MILESTONE_NAME AS milestone, COUNT(*) AS total, SUM(COMPLETED) AS completed,
                              SUM(NOT COMPLETED) AS pending, ROUND(100 * AVG(COMPLETED),1) AS completion
                            FROM checks WHERE (:cutoff IS NULL OR checkDate >= :cutoff) GROUP BY MILESTONE_NAME
                            """;
                    case "followups" ->
                            """
                            SELECT f.ID AS id, c.NAME AS customer, c.PHONE_NUMBER AS phone, o.ORDER_NUMBER AS orderNumber,
                              f.ORDER_ID AS orderId, f.MILESTONE_NAME AS milestone, f.NOTES AS notes,
                              f.SATISFACTION AS satisfaction, f.CHECKED_BY AS checkedBy, f.checkDate AS date, f.hasImage
                            FROM checks f JOIN CUSTOMERS c ON c.ID = f.CUSTOMER_ID
                            JOIN CUSTOMER_ORDERS o ON o.ID = f.ORDER_ID
                            WHERE (:cutoff IS NULL OR f.checkDate >= :cutoff)
                              AND (NULLIF(f.NOTES,'') IS NOT NULL OR f.hasImage OR NULLIF(f.SATISFACTION,'') IS NOT NULL)
                            """;
                    case "products" ->
                            """
                            SELECT items.product AS product, COUNT(DISTINCT o.ID) AS orders,
                              COALESCE(SUM(f.SATISFACTION = 'SATISFIED'),0) AS satisfied,
                              COALESCE(SUM(f.SATISFACTION = 'NEUTRAL'),0) AS neutral,
                              COALESCE(SUM(f.SATISFACTION = 'UNSATISFIED'),0) AS unsatisfied
                            FROM CUSTOMER_ORDERS o
                            JOIN JSON_TABLE(CONCAT('[', REPLACE(JSON_QUOTE(REPLACE(REPLACE(COALESCE(o.ITEMS_SUMMARY,''),CHAR(10),','),';',',')), ',', '","'), ']'),
                                '$[*]' COLUMNS (product VARCHAR(500) PATH '$')) items
                            LEFT JOIN checks f ON f.ORDER_ID = o.ID
                            WHERE (:cutoff IS NULL OR o.ORDER_DATE >= :cutoff) AND TRIM(items.product) <> '' GROUP BY items.product
                            """;
                    case "satisfaction" -> RATINGS;
                    default -> throw new IllegalArgumentException("Unknown report tab");
                };
        String order =
                switch (tab) {
                    case "milestones" -> "ORDER BY milestone";
                    case "products" -> "ORDER BY orders DESC, product";
                    default -> "ORDER BY date DESC, id DESC";
                };
        return page(CHECKS, select, order, args, page);
    }

    public List<Map<String, Object>> exportRatings(Instant cutoff, String after) {
        Map<String, Object> args = parameters(cutoff);
        args.put("satisfaction", "ALL");
        args.put("after", after);
        return jdbc.queryForList(
                CHECKS
                        + "SELECT * FROM ("
                        + RATINGS
                        + ") export WHERE id > :after ORDER BY id LIMIT 500",
                args);
    }

    private Map<String, Object> parameters(Instant cutoff) {
        Map<String, Object> args = new HashMap<>();
        args.put("now", Timestamp.from(Instant.now()));
        args.put("cutoff", cutoff == null ? null : Timestamp.from(cutoff));
        return args;
    }

    private Page<Map<String, Object>> page(
            String cte, String select, String order, Map<String, Object> args, int requestedPage) {
        long count =
                jdbc.queryForObject(
                        cte + " SELECT COUNT(*) FROM (" + select + ") result", args, Long.class);
        int page = Math.max(0, Math.min(requestedPage, (int) Math.max(0, (count - 1) / 25)));
        args.put("limit", 25);
        args.put("offset", (long) page * 25);
        List<Map<String, Object>> rows =
                count == 0
                        ? List.of()
                        : jdbc.queryForList(
                                cte + select + " " + order + " LIMIT :limit OFFSET :offset", args);
        return new PageImpl<>(rows, PageRequest.of(page, 25), count);
    }
}

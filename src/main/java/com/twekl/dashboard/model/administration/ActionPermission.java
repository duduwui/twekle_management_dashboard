package com.twekl.dashboard.model.administration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enterprise Action-Level Permission Catalog.
 * Defines granular, action-based business permissions across all system modules.
 */
public enum ActionPermission {

    // =========================================================
    // 1. CUSTOMER FOLLOW-UPS & ORDERS
    // =========================================================
    CUSTOMER_VIEW_LIST("CUSTOMER_VIEW_LIST", "CUSTOMER_FOLLOWUP", "View Customer List", "Access the customer directory, search and view customer records", "READ"),
    CUSTOMER_VIEW_LOG("CUSTOMER_VIEW_LOG", "CUSTOMER_FOLLOWUP", "View Order History & Logs", "Expand customer order logs and view checklist milestones", "READ"),
    CUSTOMER_CHECK_TOGGLE("CUSTOMER_CHECK_TOGGLE", "CUSTOMER_FOLLOWUP", "Toggle Checkpoint Status", "Check off follow-up checkpoints as Completed or Pending", "UPDATE"),
    CUSTOMER_ADD_FEEDBACK("CUSTOMER_ADD_FEEDBACK", "CUSTOMER_FOLLOWUP", "Add Notes & Satisfaction", "Enter operational feedback notes and customer ratings", "UPDATE"),
    CUSTOMER_UPLOAD_PHOTOS("CUSTOMER_UPLOAD_PHOTOS", "CUSTOMER_FOLLOWUP", "Upload Proof Photos", "Attach proof photos and receipts or remove existing attachments", "UPDATE"),
    CUSTOMER_EXPORT_EXCEL("CUSTOMER_EXPORT_EXCEL", "CUSTOMER_FOLLOWUP", "Export Customers to Excel", "Download complete customer lists to Excel/CSV", "READ"),

    // =========================================================
    // 2. REPORTS & ANALYTICS
    // =========================================================
    REPORT_VIEW_MILESTONES("REPORT_VIEW_MILESTONES", "REPORTS_INSIGHTS", "View Milestones Tab", "Access Follow-ups & Milestones performance analytics", "READ"),
    REPORT_VIEW_SATISFACTION("REPORT_VIEW_SATISFACTION", "REPORTS_INSIGHTS", "View Satisfaction Tab", "Access Customer Satisfaction ratings and sentiment feedback", "READ"),
    REPORT_VIEW_PRODUCTS("REPORT_VIEW_PRODUCTS", "REPORTS_INSIGHTS", "View Products Tab", "Access product distribution and quality defect statistics", "READ"),
    REPORT_VIEW_RETENTION("REPORT_VIEW_RETENTION", "REPORTS_INSIGHTS", "View Customer Details Tab", "Access customer retention cadence and order loyalty tiers", "READ"),
    REPORT_EXPORT_EXCEL("REPORT_EXPORT_EXCEL", "REPORTS_INSIGHTS", "Export Reports to Excel", "Export analytics and satisfaction notes to Excel/CSV", "READ"),

    // =========================================================
    // 3. ADMINISTRATION & STAFF
    // =========================================================
    STAFF_VIEW("STAFF_VIEW", "ADMIN_MANAGEMENT", "View Staff Members", "View list of administrative staff and contact details", "READ"),
    STAFF_MANAGE("STAFF_MANAGE", "ADMIN_MANAGEMENT", "Create & Edit Staff", "Add staff members, edit account details, reset passwords, and toggle active status", "CREATE"),
    STAFF_PERMISSIONS("STAFF_PERMISSIONS", "ADMIN_MANAGEMENT", "Configure Permissions Matrix", "Assign roles and customize action permissions for other users", "UPDATE"),

    // =========================================================
    // 4. TIME FILTER PRESETS & RULES
    // =========================================================
    PRESETS_VIEW("PRESETS_VIEW", "TIME_FILTERS", "View Time Presets & Rules", "View configured operational time filters and automated milestones", "READ"),
    PRESETS_MANAGE("PRESETS_MANAGE", "TIME_FILTERS", "Manage Time Presets & Rules", "Create, edit duration intervals, and toggle active/inactive milestone rules", "UPDATE");

    private final String key;
    private final String moduleKey;
    private final String title;
    private final String description;
    private final String fallbackCategory;

    ActionPermission(String key, String moduleKey, String title, String description, String fallbackCategory) {
        this.key = key;
        this.moduleKey = moduleKey;
        this.title = title;
        this.description = description;
        this.fallbackCategory = fallbackCategory;
    }

    public String getKey() { return key; }
    public String getModuleKey() { return moduleKey; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFallbackCategory() { return fallbackCategory; }

    public static Optional<ActionPermission> fromKey(String key) {
        if (key == null) return Optional.empty();
        String target = key.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(ap -> ap.getKey().equalsIgnoreCase(target))
                .findFirst();
    }

    public static List<ActionPermission> getByModule(String moduleKey) {
        if (moduleKey == null) return List.of();
        String target = moduleKey.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(ap -> ap.getModuleKey().equalsIgnoreCase(target))
                .collect(Collectors.toList());
    }

    public boolean matchesFallback(boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        return switch (fallbackCategory) {
            case "CREATE" -> canCreate || canUpdate;
            case "READ" -> canRead;
            case "UPDATE" -> canUpdate;
            case "DELETE" -> canDelete;
            default -> canRead;
        };
    }
}

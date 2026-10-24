package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Customer Controller Operations Contract (CustomerOperations.java)
 * ============================================================================
 * 
 * Defines all Web & REST API endpoint signatures for the Customer Directory,
 * customer profiles, order history, feedback reviews, and simulation generators.
 * 
 * Canonical Route: /admin/customers
 * 
 * Core Capabilities:
 *   1. Directory View: SSR rendering of customer directory with initial filters & metrics.
 *   2. REST Data Feeds: Quick JSON lists and paginated query search results.
 *   3. Customer Profiles: CRUD operations for customer entity records.
 *   4. Order & Feedback History: Customer-specific order and feedback sub-resources.
 *   5. Intake Simulator: Batch generator for realistic customer intake simulations.
 * ============================================================================
 */
public interface CustomerOperations {

    /**
     * Renders the customer directory SSR HTML page.
     *
     * @param days   Initial days intake filter.
     * @param filter Filter preset key (default 'ALL').
     * @param search Search query string.
     * @param model  Spring MVC Model container.
     * @return Thymeleaf view path: "administration/customer/customers".
     */
    String showCustomersPage(Integer days, String filter, String search, Model model);

    /**
     * Returns a JSON list of all customers, formatted with their latest follow-up status.
     *
     * @param timeFilter Preset time filter parameter.
     * @param filter     Alternative filter parameter.
     * @param limit      Maximum results to return.
     * @return ResponseEntity with list of customer map objects.
     */
    ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllCustomers(String timeFilter, String filter, Integer limit);

    /**
     * Returns a paginated JSON response for server-side tables and search filters.
     *
     * @param page       Current page number (1-based).
     * @param size       Page size limit (e.g. 40).
     * @param timeFilter Time boundary filter key.
     * @param status     Follow-up status filter (ALERT, IDLE, DONE).
     * @param search     Name, phone, city, or email search string.
     * @param sortField  Column sort property.
     * @param sortDir    Sort direction ('asc' or 'desc').
     * @return Paged payload with content list, total elements, and page indices.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> getPagedCustomers(
            int page,
            int size,
            String timeFilter,
            String status,
            String search,
            String sortField,
            String sortDir);

    /**
     * Retrieves aggregated system KPIs and customer directory statistics.
     *
     * @return Total customers, intake counts, revenue, and satisfaction metrics.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerStats();

    /**
     * Retrieves up to 200 customer follow-up summaries for quick UI feeds.
     *
     * @param timeFilter Time filter key.
     * @param filter     Alternative filter parameter.
     * @param limit      Max limit.
     * @return List of CustomerFollowupSummary DTOs.
     */
    ResponseEntity<ApiResponse<List<CustomerFollowupSummary>>> getCustomerFollowups(String timeFilter, String filter, Integer limit);

    /**
     * Retrieves a single customer profile by primary key ID.
     *
     * @param customerId Customer ID.
     * @return Customer entity or 404 Not Found.
     */
    ResponseEntity<ApiResponse<Customer>> getCustomerById(Long customerId);

    /**
     * Retrieves all orders placed by a specific customer.
     *
     * @param customerId Customer ID.
     * @return List of CustomerOrder entities.
     */
    ResponseEntity<ApiResponse<List<CustomerOrder>>> getCustomerOrders(Long customerId);

    /**
     * Retrieves all feedback reviews submitted by a specific customer.
     *
     * @param customerId Customer ID.
     * @return List of CustomerFeedback entities.
     */
    ResponseEntity<ApiResponse<List<CustomerFeedback>>> getCustomerFeedbacks(Long customerId);

    /**
     * Submits a customer feedback review and star rating.
     *
     * @param customerId Customer ID.
     * @param feedback   CustomerFeedback payload.
     * @return Saved CustomerFeedback entity.
     */
    ResponseEntity<ApiResponse<CustomerFeedback>> createFeedback(Long customerId, CustomerFeedback feedback);

    /**
     * Creates a new customer profile, optionally initiating an initial order and milestones.
     *
     * @param customer  Customer entity to persist.
     * @param autoOrder Whether to schedule initial order and follow-up milestones (default true).
     * @return Persisted Customer entity.
     */
    ResponseEntity<ApiResponse<Customer>> createCustomer(Customer customer, boolean autoOrder);

    /**
     * Simulates batch customer intake across specified number of days.
     *
     * @param days        Number of intake days to simulate.
     * @param count       Target customers per day (~40).
     * @param seedHistory Whether to seed tiered multi-order loyalty histories.
     * @return Summary response with created entities counts.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> generateCustomers(int days, int count, boolean seedHistory);

    /**
     * Runs single-day intake batch generator (~40 realistic customers).
     *
     * @param count Customers count (default 40).
     * @return Summary map.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> runDailyIntake(int count);

    /**
     * Resets database catalogs and freshly seeds with realistic staggered customer data.
     *
     * @param count Base batch count.
     * @return Summary map.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> resetAndSeed(int count);

    /**
     * Updates an existing customer's contact information.
     *
     * @param customerId Customer ID to update.
     * @param customer   Updated fields.
     * @return Updated Customer entity.
     */
    ResponseEntity<ApiResponse<Customer>> updateCustomer(Long customerId, Customer customer);

    /**
     * Deletes a customer profile and cascades related checkpoints.
     *
     * @param customerId Customer ID to delete.
     * @return Void success response.
     */
    ResponseEntity<ApiResponse<Void>> deleteCustomer(Long customerId);
}

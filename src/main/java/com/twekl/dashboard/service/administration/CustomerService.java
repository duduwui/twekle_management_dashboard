package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * Customer Service Contract (CustomerService.java)
 * ============================================================================
 * 
 * Defines all customer management capabilities, order creation, feedback tracking,
 * and high-level directory and retention metrics.
 * 
 * Core Capabilities:
 *   1. Customer Directory: Full CRUD operations for customer contact profiles and cities.
 *   2. Order Management: Order creation with automated follow-up milestone generation.
 *   3. Feedback Ingestion: Records post-sales customer reviews and 1-5 star ratings.
 *   4. System Statistics: Aggregates total customer counts, intake volumes (today, week, month),
 *      order revenues, and satisfaction scores with multi-layer caching.
 * ============================================================================
 */
public interface CustomerService {

    /**
     * Retrieves all customer records in the database.
     *
     * @return List of all customers.
     */
    List<Customer> findAll();

    /**
     * Looks up a customer profile by its primary key ID.
     *
     * @param customerId ID of the customer.
     * @return Optional containing the customer if found.
     */
    Optional<Customer> findById(Long customerId);

    /**
     * Looks up a customer profile by its primary key ID (alias for findById).
     *
     * @param customerId ID of the customer.
     * @return Optional containing the customer if found.
     */
    Optional<Customer> findByCustomerId(Long customerId);

    /**
     * Finds a single customer order by its primary key ID.
     *
     * @param orderId ID of the order.
     * @return Optional containing the CustomerOrder if found.
     */
    Optional<CustomerOrder> findOrderById(Long orderId);

    /**
     * Retrieves all orders placed by a specific customer, ordered by date descending.
     *
     * @param customerId Customer primary key ID.
     * @return List of CustomerOrder entities.
     */
    List<CustomerOrder> findOrdersByCustomerId(Long customerId);

    /**
     * Retrieves all satisfaction reviews and feedbacks submitted by a customer.
     *
     * @param customerId Customer primary key ID.
     * @return List of CustomerFeedback entities.
     */
    List<CustomerFeedback> findFeedbacksByCustomerId(Long customerId);

    /**
     * Returns the total count of registered customers in the database.
     *
     * @return Total customer count.
     */
    long count();

    /**
     * Registers a new customer and evicts cached statistics.
     *
     * @param customer Customer entity to persist.
     * @return Persisted Customer entity with generated ID.
     */
    Customer createCustomer(Customer customer);

    /**
     * Updates an existing customer's contact information and order totals.
     *
     * @param customerId ID of the customer to update.
     * @param updated    Updated customer fields.
     * @return Persisted Customer entity.
     */
    Customer updateCustomer(Long customerId, Customer updated);

    /**
     * Removes a customer from the database and clears related caches.
     *
     * @param customerId ID of the customer to delete.
     */
    void deleteCustomer(Long customerId);

    /**
     * Creates a new customer order and automatically initiates follow-up checkpoint
     * milestones for all active timeline filter presets.
     *
     * @param order CustomerOrder entity to persist.
     * @return Persisted CustomerOrder entity.
     */
    CustomerOrder createOrder(CustomerOrder order);

    /**
     * Records a new customer feedback review and evicts report and satisfaction caches.
     *
     * @param feedback CustomerFeedback entity to persist.
     * @return Persisted CustomerFeedback entity.
     */
    CustomerFeedback createFeedback(CustomerFeedback feedback);

    /**
     * Aggregates live system-wide KPIs: customer intake (today, week, month), revenue,
     * follow-up completion percentages, overdue counts, and average satisfaction ratings.
     * Caches result under 'customerStats'.
     *
     * @return Key-value map of system statistics.
     */
    Map<String, Object> getStats();

    /**
     * Aggregates system KPIs using pre-computed customer follow-up summaries to avoid
     * duplicate database queries when summaries are already present in memory.
     *
     * @param summaries Pre-computed customer follow-up summaries.
     * @return Key-value map of system statistics.
     */
    Map<String, Object> getStats(List<CustomerFollowupSummary> summaries);
}

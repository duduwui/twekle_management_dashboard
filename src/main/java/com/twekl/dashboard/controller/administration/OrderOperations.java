package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * ============================================================================
 * Order Controller Operations Contract (OrderOperations.java)
 * ============================================================================
 * 
 * Defines all REST API endpoints for customer order records and order-level follow-up checks.
 * 
 * Canonical Route: /admin/orders
 * 
 * Core Capabilities:
 *   1. Order Details: Lookup single order by primary key ID.
 *   2. Order Follow-up Milestones: Queries scheduled milestone checkpoints for an order.
 *   3. Order Creation: Places orders with automated timeline milestone generation.
 * ============================================================================
 */
public interface OrderOperations {

    /**
     * Looks up order metadata and associated customer by order number or numeric ID.
     */
    ResponseEntity<ApiResponse<java.util.Map<String, Object>>> lookupOrder(String orderIdentifier);

    /**
     * Retrieves all milestone checks generated for a specific order.
     *
     * @param orderId Customer order ID.
     * @return List of OrderFollowupCheck entities.
     */
    ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderFollowupChecks(Long orderId);

    /**
     * Retrieves customer order details by order ID.
     *
     * @param orderId Customer order ID.
     * @return CustomerOrder entity or 404 Not Found.
     */
    ResponseEntity<ApiResponse<CustomerOrder>> getOrderById(Long orderId);

    /**
     * Creates a new customer order and automatically initiates follow-up milestones.
     *
     * @param order CustomerOrder entity payload.
     * @return Created CustomerOrder entity.
     */
    ResponseEntity<ApiResponse<CustomerOrder>> createOrder(CustomerOrder order);
}

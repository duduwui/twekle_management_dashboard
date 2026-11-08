package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.ExternalReviewRequest;

import java.util.Map;

/**
 * Service for managing external website integrations, session handoffs,
 * non-expiring JWT tokens, and automatic synchronization of customer/order data.
 */
public interface ExternalIntegrationService {

    /**
     * Checks if the provided API key matches the configured external integration key.
     */
    boolean isValidApiKey(String apiKey);

    /**
     * Synchronizes or creates the customer and order, generates follow-up checks,
     * and produces review session metadata.
     */
    Map<String, Object> createReviewSession(ExternalReviewRequest request, String baseUrl);

    /**
     * Consumes a single-use handoff token and returns the customer and order metadata.
     * Returns null if the token is invalid or expired.
     */
    Map<String, Object> consumeHandoffToken(String token);

    /**
     * Generates a tamper-proof RFC 7519 HS256 JWT containing the 4 review parameters.
     * If nonExpiring is true, the token contains no "exp" claim and works indefinitely.
     */
    String generateReviewJwt(ExternalReviewRequest request, boolean nonExpiring);

    /**
     * Generates a signed RFC 7519 HS256 JWT for an authenticated admin user or service client.
     */
    String generateUserJwt(String username, String role, long validitySeconds);

    /**
     * Parses and cryptographically verifies an RFC 7519 HS256 JWT token using HMAC-SHA256.
     * Checks against primary and all rotated keys so key updates never break existing tokens.
     * Returns the verified ExternalReviewRequest or null if signature is invalid or expired.
     */
    ExternalReviewRequest parseAndVerifyReviewJwt(String jwtToken);

    /**
     * Encrypts the review/follow-up request parameters into an authenticated AES-256-GCM token.
     */
    String encryptSession(ExternalReviewRequest request);

    /**
     * Decrypts and verifies an authenticated session token (supports both AES-256-GCM and HMAC-SHA256 JWT).
     * Returns the verified ExternalReviewRequest, or null if tampered, corrupted, or invalid.
     */
    ExternalReviewRequest decryptSession(String encryptedToken);

    /**
     * Rotates to a new primary signing key while keeping existing keys valid for verification.
     * Ensures that key updates do not disrupt active users or existing links.
     */
    void rotatePrimaryKey(String newSecretKey);

    /**
     * Adds an additional allowed secret key for verification.
     */
    void addSecretKey(String secretKey);

    /**
     * Returns the set of all active secret keys.
     */
    java.util.Set<String> getValidSecretKeys();

    /**
     * Returns the current primary secret key.
     */
    String getPrimaryKey();

    /**
     * Looks up customer review feedback, returning ONLY reviewed checkpoints
     * (omits pending, overdue, and unfinished).
     *
     * @param customerId Optional customer ID (externalId or internal ID).
     * @param orderId    Optional order ID or orderNumber.
     * @param customerName Optional customer name.
     * @return Map containing search results, customer information, and reviewed checkpoints.
     */
    Map<String, Object> getCustomerReviews(String customerId, String orderId, String customerName);

    /**
     * Submits a customer review / milestone feedback directly via API.
     */
    Map<String, Object> submitReview(String orderId, String notes, String satisfaction, String photoUrl);
}

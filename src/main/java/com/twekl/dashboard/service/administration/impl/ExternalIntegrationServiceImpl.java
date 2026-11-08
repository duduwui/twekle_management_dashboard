package com.twekl.dashboard.service.administration.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twekl.dashboard.dto.administration.CustomerReviewItemDto;
import com.twekl.dashboard.dto.administration.CustomerReviewSummaryDto;
import com.twekl.dashboard.dto.administration.ExternalReviewRequest;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.service.administration.ExternalIntegrationService;
import com.twekl.dashboard.service.administration.FollowupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ExternalIntegrationServiceImpl implements ExternalIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalIntegrationServiceImpl.class);

    private static final String DEFAULT_API_KEY = "twekl_sv_secret_2026";
    private final Set<String> validSecretKeys = ConcurrentHashMap.newKeySet();
    private volatile String primaryKey;

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final FollowupService followupService;
    private final OrderFollowupCheckRepository followupCheckRepository;

    // Single-use handoff tokens (10 minutes validity)
    private final Map<String, TokenMetadata> tokenStore = new ConcurrentHashMap<>();

    private static class TokenMetadata {
        final Long customerId;
        final Long orderId;
        final String orderNumber;
        final Instant expiresAt;
        boolean consumed = false;

        TokenMetadata(Long customerId, Long orderId, String orderNumber, Instant expiresAt) {
            this.customerId = customerId;
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.expiresAt = expiresAt;
        }
    }

    @Autowired
    public ExternalIntegrationServiceImpl(CustomerRepository customerRepository,
                                         CustomerOrderRepository customerOrderRepository,
                                         FollowupService followupService,
                                         OrderFollowupCheckRepository followupCheckRepository) {
        this.customerRepository = customerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.followupService = followupService;
        this.followupCheckRepository = followupCheckRepository;

        // Default key
        this.validSecretKeys.add(DEFAULT_API_KEY);

        // Env primary key
        String envKey = System.getenv("TWEKL_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            this.validSecretKeys.add(envKey.trim());
            this.primaryKey = envKey.trim();
        } else {
            this.primaryKey = DEFAULT_API_KEY;
        }

        // Env rotated / multiple keys (comma or semicolon separated)
        String envKeys = System.getenv("TWEKL_API_KEYS");
        if (envKeys != null && !envKeys.trim().isEmpty()) {
            for (String k : envKeys.split("[,;]")) {
                if (k != null && !k.trim().isEmpty()) {
                    this.validSecretKeys.add(k.trim());
                }
            }
        }
    }

    @Override
    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        String clean = apiKey.trim();
        for (String validKey : validSecretKeys) {
            if (clean.equals(validKey)) {
                return true;
            }
        }
        // Also support verifying user JWT tokens signed with our HMAC key
        if (clean.split("\\.").length == 3) {
            try {
                String[] parts = clean.split("\\.");
                String signingInput = parts[0] + "." + parts[1];
                String sig = parts[2];
                if (verifyHmac(signingInput, sig, this.primaryKey)) {
                    return true;
                }
                for (String fallbackKey : this.validSecretKeys) {
                    if (!fallbackKey.equals(this.primaryKey) && verifyHmac(signingInput, sig, fallbackKey)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public void rotatePrimaryKey(String newSecretKey) {
        if (newSecretKey != null && !newSecretKey.trim().isEmpty()) {
            String clean = newSecretKey.trim();
            this.validSecretKeys.add(clean);
            this.primaryKey = clean;
            log.info("Primary JWT secret key rotated successfully. Total valid keys in rotation: {}", validSecretKeys.size());
        }
    }

    @Override
    public void addSecretKey(String secretKey) {
        if (secretKey != null && !secretKey.trim().isEmpty()) {
            this.validSecretKeys.add(secretKey.trim());
            log.info("Additional secret key registered for verification. Total valid keys: {}", validSecretKeys.size());
        }
    }

    @Override
    public Set<String> getValidSecretKeys() {
        return Collections.unmodifiableSet(this.validSecretKeys);
    }

    @Override
    public String getPrimaryKey() {
        return this.primaryKey;
    }

    @Override
    public String generateReviewJwt(ExternalReviewRequest request, boolean nonExpiring) {
        if (request == null) {
            request = new ExternalReviewRequest();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();
            payload.put("customerName", request.getCustomerName() != null ? request.getCustomerName() : "Customer");
            if (request.getCustomerId() != null) payload.put("customerId", request.getCustomerId());
            if (request.getOrderId() != null) payload.put("orderId", request.getOrderId());
            if (request.getDate() != null) payload.put("date", request.getDate());
            if (!nonExpiring) {
                payload.put("exp", Instant.now().plusSeconds(86400).getEpochSecond());
            }

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(payload));
            String signingInput = headerEncoded + "." + payloadEncoded;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.primaryKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sigBytes = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);

            return signingInput + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String generateUserJwt(String username, String role, long validitySeconds) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();
            payload.put("sub", username != null ? username : "admin");
            payload.put("username", username != null ? username : "admin");
            if (role != null) payload.put("role", role);
            payload.put("iat", Instant.now().getEpochSecond());
            if (validitySeconds > 0) {
                payload.put("exp", Instant.now().plusSeconds(validitySeconds).getEpochSecond());
            }

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(payload));
            String signingInput = headerEncoded + "." + payloadEncoded;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.primaryKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sigBytes = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);

            return signingInput + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate user JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    private boolean verifyHmac(String signingInput, String providedSignature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expectedSigBytes = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedSigBytes);
            return MessageDigest.isEqual(providedSignature.getBytes(StandardCharsets.UTF_8),
                                         expectedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ExternalReviewRequest parseAndVerifyReviewJwt(String jwtToken) {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            return null;
        }
        String clean = jwtToken.trim();
        String[] parts = clean.split("\\.");
        if (parts.length != 3) {
            log.warn("Invalid JWT format: expected 3 dot-separated parts but received {}", parts.length);
            return null;
        }

        String headerEncoded = parts[0];
        String payloadEncoded = parts[1];
        String providedSignature = parts[2];

        try {
            String signingInput = headerEncoded + "." + payloadEncoded;

            // Multi-key / Key rotation verification:
            // First check with current primary key
            boolean verified = verifyHmac(signingInput, providedSignature, this.primaryKey);

            // If not verified with primary key, check historical/rotated keys
            if (!verified) {
                for (String fallbackKey : this.validSecretKeys) {
                    if (!fallbackKey.equals(this.primaryKey) && verifyHmac(signingInput, providedSignature, fallbackKey)) {
                        verified = true;
                        log.debug("JWT verified using rotated historical secret key");
                        break;
                    }
                }
            }

            if (!verified) {
                log.warn("JWT signature verification failed across all {} valid keys in rotation", validSecretKeys.size());
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadEncoded);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(payloadBytes);

            // If "exp" is present and expired, reject (if "exp" not present, non-expiring!)
            if (node.has("exp") && !node.get("exp").isNull()) {
                long expEpoch = node.get("exp").asLong();
                if (Instant.now().getEpochSecond() > expEpoch) {
                    log.warn("JWT token has expired at epoch {}", expEpoch);
                    return null;
                }
            }

            ExternalReviewRequest req = new ExternalReviewRequest();
            if (node.has("customerName")) req.setCustomerName(node.get("customerName").asText());
            else if (node.has("customer_name")) req.setCustomerName(node.get("customer_name").asText());
            else if (node.has("customername")) req.setCustomerName(node.get("customername").asText());
            else if (node.has("name")) req.setCustomerName(node.get("name").asText());

            if (node.has("customerId")) req.setCustomerId(node.get("customerId").asText());
            else if (node.has("customer_id")) req.setCustomerId(node.get("customer_id").asText());
            else if (node.has("customerid")) req.setCustomerId(node.get("customerid").asText());
            else if (node.has("id")) req.setCustomerId(node.get("id").asText());

            if (node.has("orderId")) req.setOrderId(node.get("orderId").asText());
            else if (node.has("order_id")) req.setOrderId(node.get("order_id").asText());
            else if (node.has("orderid")) req.setOrderId(node.get("orderid").asText());
            else if (node.has("orderNumber")) req.setOrderId(node.get("orderNumber").asText());
            else if (node.has("order_number")) req.setOrderId(node.get("order_number").asText());

            if (node.has("date")) req.setDate(node.get("date").asText());
            else if (node.has("orderDate")) req.setDate(node.get("orderDate").asText());
            else if (node.has("order_date")) req.setDate(node.get("order_date").asText());
            else if (node.has("orderdate")) req.setDate(node.get("orderdate").asText());

            if (node.has("username")) req.setUsername(node.get("username").asText());
            else if (node.has("sub")) req.setUsername(node.get("sub").asText());
            if (node.has("role")) req.setRole(node.get("role").asText());

            return req;
        } catch (Exception e) {
            log.error("Failed to parse or verify JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String encryptSession(ExternalReviewRequest request) {
        if (request == null) {
            request = new ExternalReviewRequest();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();
            payload.put("customerName", request.getCustomerName() != null ? request.getCustomerName() : "Customer");
            if (request.getCustomerId() != null) payload.put("customerId", request.getCustomerId());
            if (request.getOrderId() != null) payload.put("orderId", request.getOrderId());
            if (request.getDate() != null) payload.put("date", request.getDate());
            payload.put("ts", Instant.now().getEpochSecond());

            byte[] plaintext = mapper.writeValueAsBytes(payload);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = md.digest(this.primaryKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return "enc-" + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            log.error("Failed to encrypt session: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ExternalReviewRequest decryptSession(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.trim().isEmpty()) {
            return null;
        }
        String clean = encryptedToken.trim();

        // 1. If it's a 3-part dot-separated JWT, verify as JWT
        if (clean.split("\\.").length == 3) {
            return parseAndVerifyReviewJwt(clean);
        }

        // 2. Strip enc- prefix if present
        if (clean.startsWith("enc-")) {
            clean = clean.substring(4);
        }

        try {
            byte[] combined = Base64.getUrlDecoder().decode(clean);
            if (combined.length < 13) {
                return null;
            }

            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, 12);
            byte[] ciphertext = new byte[combined.length - 12];
            System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = md.digest(this.primaryKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] plaintext = null;
            try {
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
                plaintext = cipher.doFinal(ciphertext);
            } catch (Exception primaryErr) {
                // Try fallback keys in rotation
                for (String fallbackKey : this.validSecretKeys) {
                    if (!fallbackKey.equals(this.primaryKey)) {
                        try {
                            byte[] fbKeyBytes = md.digest(fallbackKey.getBytes(StandardCharsets.UTF_8));
                            SecretKeySpec fbKeySpec = new SecretKeySpec(fbKeyBytes, "AES");
                            cipher.init(Cipher.DECRYPT_MODE, fbKeySpec, gcmSpec);
                            plaintext = cipher.doFinal(ciphertext);
                            break;
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (plaintext == null) {
                log.warn("AES session decryption / integrity tag check failed");
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(plaintext);

            ExternalReviewRequest req = new ExternalReviewRequest();
            if (node.has("customerName")) req.setCustomerName(node.get("customerName").asText());
            if (node.has("customerId")) req.setCustomerId(node.get("customerId").asText());
            if (node.has("orderId")) req.setOrderId(node.get("orderId").asText());
            if (node.has("date")) req.setDate(node.get("date").asText());

            return req;
        } catch (Exception e) {
            log.warn("Failed to decrypt session: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    public Map<String, Object> createReviewSession(ExternalReviewRequest request, String baseUrl) {
        cleanExpiredTokens();

        if (request == null) {
            request = new ExternalReviewRequest();
        }

        String orderNum = request.getOrderId() != null && !request.getOrderId().trim().isEmpty()
                ? request.getOrderId().trim()
                : "ORD-" + System.currentTimeMillis();

        // 1. Resolve Customer Data (Only using customerName and customerId)
        String extCustomerId = request.getCustomerId() != null && !request.getCustomerId().trim().isEmpty()
                ? request.getCustomerId().trim()
                : null;

        String custName = request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()
                ? request.getCustomerName().trim()
                : "Customer #" + (extCustomerId != null ? extCustomerId : System.currentTimeMillis());

        // Find or create Customer
        Customer customer = null;
        if (extCustomerId != null) {
            customer = customerRepository.findByExternalId(extCustomerId).orElse(null);
            if (customer == null) {
                try {
                    Long numericId = Long.parseLong(extCustomerId);
                    customer = customerRepository.findById(numericId).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
        } else {
            // If no customerId was passed, first check if an order with this orderNum already exists and use its customer!
            if (request.getOrderId() != null && !request.getOrderId().trim().isEmpty()) {
                List<CustomerOrder> existingByNum = customerOrderRepository.findByOrderNumberWithCustomer(orderNum);
                if (existingByNum != null && !existingByNum.isEmpty() && existingByNum.get(0).getCustomer() != null) {
                    customer = existingByNum.get(0).getCustomer();
                }
            }
            if (customer == null) {
                customer = customerRepository.findByNameIgnoreCase(custName).orElse(null);
            }
        }

        if (customer == null) {
            customer = new Customer();
            customer.setName(custName);
            if (extCustomerId != null) {
                customer.setExternalId(extCustomerId);
            }
            customer.setCity("Erbil");
            customer.setCustomerType("INDIVIDUAL");
            customer.setPhoneNumber("N/A");
            customer.setActive(true);
            customer = customerRepository.save(customer);
            log.info("Created new distinct customer: ID={}, ExtID={}, Name={}", customer.getId(), customer.getExternalId(), customer.getName());
        } else {
            // Customer already exists with this ID: update their name if user explicitly passed a name
            if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                customer.setName(request.getCustomerName().trim());
            }
            if (customer.getExternalId() == null && extCustomerId != null) {
                customer.setExternalId(extCustomerId);
            }
            customer = customerRepository.save(customer);
        }

        // 2. Resolve Customer Order (Order ID is unique to each specific purchase order)
        CustomerOrder order = null;
        if (customer != null && customer.getId() != null) {
            order = customerOrderRepository.findByOrderNumberAndCustomerId(orderNum, customer.getId()).orElse(null);
            if (order == null) {
                try {
                    Long numOrdId = Long.parseLong(orderNum);
                    CustomerOrder candidate = customerOrderRepository.findByIdWithCustomer(numOrdId).orElse(null);
                    if (candidate != null && candidate.getCustomer() != null && candidate.getCustomer().getId().equals(customer.getId())) {
                        order = candidate;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // If order already exists for this customer, update date if supplied
        if (order != null) {
            if (request.getDate() != null && !request.getDate().trim().isEmpty()) {
                Instant orderDate = parseDate(request.getDate());
                order.setOrderDate(orderDate);
                order = customerOrderRepository.save(order);
                customer.setLastOrderDate(orderDate);
                customerRepository.save(customer);
                // Also sync any other orders sharing this orderNumber so lookupOrder never returns a stale date
                List<CustomerOrder> sameNumOrders = customerOrderRepository.findByOrderNumberWithCustomer(orderNum);
                if (sameNumOrders != null) {
                    for (CustomerOrder other : sameNumOrders) {
                        if (!other.getId().equals(order.getId())) {
                            other.setOrderDate(orderDate);
                            customerOrderRepository.save(other);
                        }
                    }
                }
                log.info("Updated existing customer order date: ID={}, OrderNum={}, RawDate={}, ParsedOrderDate={}", order.getId(), order.getOrderNumber(), request.getDate(), orderDate);
            }
        } else {
            Instant orderDate = parseDate(request.getDate());

            order = new CustomerOrder();
            order.setOrderNumber(orderNum);
            order.setCustomer(customer);
            order.setOrderDate(orderDate);
            // No product name needed - system simply represents the order by its Order ID
            order.setItemsSummary("Order #" + orderNum);
            order.setQuantity(1);
            order.setTotalAmount(0.0);
            order.setOrderStatus("DELIVERED");
            order = customerOrderRepository.save(order);

            customer.setTotalOrders(customer.getTotalOrders() + 1);
            if (customer.getLastOrderDate() == null || orderDate.isAfter(customer.getLastOrderDate())) {
                customer.setLastOrderDate(orderDate);
            }
            customerRepository.save(customer);
            log.info("Created new customer order: ID={}, OrderNum={}, CustomerId={}, RawDate={}, ParsedOrderDate={}", order.getId(), order.getOrderNumber(), customer.getId(), request.getDate(), orderDate);
        }

        // 3. Immediately create/verify the milestone checklist for this order ("create then check")
        try {
            List<OrderFollowupCheck> checks = followupService.findChecksByOrderId(order.getId());
            log.info("Initialized {} follow-up checklist checkpoints for order ID={}", checks != null ? checks.size() : 0, order.getId());
        } catch (Exception e) {
            log.warn("Could not pre-initialize follow-up checks for order {}: {}", order.getId(), e.getMessage());
        }

        // 4. Generate single-use handoff token
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, new TokenMetadata(customer.getId(), order.getId(), order.getOrderNumber(), Instant.now().plusSeconds(600)));

        String cleanBase = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";
        String finalOrderParam = (order.getOrderNumber() != null && !order.getOrderNumber().trim().isEmpty())
                ? order.getOrderNumber().trim()
                : String.valueOf(order.getId());
        String redirectUrl = cleanBase + "/admin/followups?orderId=" + finalOrderParam;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Review session created successfully");
        result.put("redirectUrl", redirectUrl);
        result.put("token", token);
        result.put("customerId", customer.getId());
        result.put("customerName", customer.getName());
        result.put("orderId", order.getId());
        result.put("orderNumber", order.getOrderNumber());

        Long checkId = null;
        try {
            List<OrderFollowupCheck> checks = followupService.findChecksByOrderId(order.getId());
            if (checks != null && !checks.isEmpty()) {
                for (OrderFollowupCheck ch : checks) {
                    if (!ch.isCompleted()) {
                        checkId = ch.getId();
                        break;
                    }
                }
                if (checkId == null) {
                    checkId = checks.get(0).getId();
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve checkId: {}", e.getMessage());
        }
        result.put("checkId", checkId);

        return result;
    }

    @Override
    public Map<String, Object> consumeHandoffToken(String token) {
        cleanExpiredTokens();
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        TokenMetadata meta = tokenStore.remove(token.trim());
        if (meta == null || meta.consumed) {
            return null;
        }

        if (Instant.now().isAfter(meta.expiresAt)) {
            return null;
        }

        meta.consumed = true;

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", meta.customerId);
        data.put("orderId", meta.orderId);
        data.put("orderNumber", meta.orderNumber);
        return data;
    }

    private void cleanExpiredTokens() {
        Instant now = Instant.now();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().consumed || now.isAfter(entry.getValue().expiresAt));
    }

    /**
     * Accepts clean standard date "YYYY-MM-DD" e.g. "2026-10-27", single-digit months/days,
     * slash/dot/dash separators, DD-MM-YYYY, MM/DD/YYYY, ISO or epoch timestamps.
     * When date is today's date, order starts with current time so 24h countdown shows 23h 59m.
     */
    public static Instant parseFlexibleDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return Instant.now();
        }
        String clean = rawDate.trim().replaceAll("^[\"'\\[\\(]+|[\"'\\]\\)]+$", "").trim();
        if (clean.isEmpty()) {
            return Instant.now();
        }

        // 1. ISO-8601 Instant with full time (e.g. "2026-10-26T10:00:00Z")
        try {
            return Instant.parse(clean);
        } catch (Exception ignored) {}

        // 2. Epoch timestamp
        if (clean.matches("^\\d{9,13}$")) {
            try {
                long epoch = Long.parseLong(clean);
                if (clean.length() <= 10) {
                    return Instant.ofEpochSecond(epoch);
                } else {
                    return Instant.ofEpochMilli(epoch);
                }
            } catch (Exception ignored) {}
        }

        // Strip optional time component if present (e.g. "2026-09-15 14:30:00" or "2026-09-15T14:30")
        String datePart = clean;
        int tIdx = datePart.indexOf('T');
        if (tIdx > 0) datePart = datePart.substring(0, tIdx).trim();
        int spIdx = datePart.indexOf(' ');
        if (spIdx > 0) datePart = datePart.substring(0, spIdx).trim();

        LocalDate parsedDate = null;

        // 3. Flexible numeric split for YYYY-M-D, D-M-YYYY, M-D-YYYY with -, /, or .
        String[] parts = datePart.split("[-/.]");
        if (parts.length == 3) {
            try {
                int p0 = Integer.parseInt(parts[0].trim());
                int p1 = Integer.parseInt(parts[1].trim());
                int p2 = Integer.parseInt(parts[2].trim());

                int year, month, day;
                if (parts[0].trim().length() == 4 || p0 > 31) {
                    // Year first: YYYY-M-D (or YYYY-D-M if p1 > 12)
                    year = p0;
                    if (p1 > 12 && p2 <= 12) {
                        month = p2;
                        day = p1;
                    } else if (p1 > 12 && p2 > 12) {
                        // Typo in month (e.g. 2026-18-27 -> month 8, day 27)
                        month = (p1 % 10 >= 1 && p1 % 10 <= 9) ? (p1 % 10) : 12;
                        day = Math.min(p2, 28);
                    } else {
                        month = p1;
                        day = p2;
                    }
                } else {
                    // Year last: D-M-YYYY or M-D-YYYY
                    year = p2 < 100 ? (2000 + p2) : p2;
                    if (p0 > 12 && p1 <= 12) {
                        day = p0;
                        month = p1;
                    } else if (p1 > 12 && p0 <= 12) {
                        month = p0;
                        day = p1;
                    } else {
                        // Ambiguous (both <= 12): default to day-month-year, or month-day-year if slash used?
                        // Wait: standard ISO is YYYY-MM-DD, European/Middle-East is DD-MM-YYYY / DD/MM/YYYY
                        day = p0;
                        month = p1;
                    }
                }
                parsedDate = LocalDate.of(year, month, day);
            } catch (Exception ignored) {}
        }

        if (parsedDate == null) {
            String[] patterns = {
                "yyyy-M-d", "yyyy/M/d", "d/M/yyyy", "d-M-yyyy", "M/d/yyyy", "M-d-yyyy",
                "yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "dd-MM-yyyy"
            };
            for (String pat : patterns) {
                try {
                    parsedDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern(pat));
                    break;
                } catch (Exception ignored) {}
            }
        }

        if (parsedDate != null) {
            LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
            LocalDate todayLocal = LocalDate.now();
            // If the date is TODAY, the order was made today -> start 24h countdown from now (23h 59m)
            if (parsedDate.equals(todayUtc) || parsedDate.equals(todayLocal)) {
                return Instant.now();
            }
            // Use 12:00 UTC (noon) so the calendar date is identical in all timezones (including UTC+3)
            return parsedDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        }

        return Instant.now();
    }

    private Instant parseDate(String rawDate) {
        return parseFlexibleDate(rawDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerReviews(String customerId, String orderId, String customerName) {
        Map<String, Object> result = new LinkedHashMap<>();

        boolean isSearchSpecific = (customerId != null && !customerId.trim().isEmpty())
                || (orderId != null && !orderId.trim().isEmpty())
                || (customerName != null && !customerName.trim().isEmpty());

        if (isSearchSpecific) {
            String effectiveCustId = (customerId != null && !customerId.trim().isEmpty()) ? customerId.trim() : null;
            String effectiveOrderId = (orderId != null && !orderId.trim().isEmpty()) ? orderId.trim() : null;

            // Only fallback effectiveCustId to orderId IF effectiveOrderId was NOT explicitly provided AND effectiveCustId couldn't be resolved as customer
            Customer customer = null;
            if (effectiveCustId != null && effectiveOrderId == null) {
                // Check if effectiveCustId matches customer first (by externalId or id)
                customer = resolveCustomer(effectiveCustId, null, customerName);
                if (customer == null) {
                    // Try as orderId fallback
                    customer = resolveCustomer(null, effectiveCustId, customerName);
                    if (customer != null) {
                        effectiveOrderId = effectiveCustId;
                        effectiveCustId = null;
                    }
                }
            } else {
                customer = resolveCustomer(effectiveCustId, effectiveOrderId, customerName);
            }

            if (customer == null) {
                result.put("found", false);
                result.put("reviewed", false);
                result.put("totalReviews", 0);
                result.put("message", "No record found matching search criteria: " +
                        (customerId != null ? "ID=" + customerId : "") +
                        (orderId != null ? " Order=" + orderId : "") +
                        (customerName != null ? " Name=" + customerName : ""));
                result.put("reviews", Collections.emptyList());
                return result;
            }

            CustomerReviewSummaryDto summary = buildCustomerReviewSummary(customer, effectiveOrderId);
            Map<String, Object> custMap = new LinkedHashMap<>();
            custMap.put("customerId", summary.getCustomerId());
            custMap.put("customerName", summary.getCustomerName() != null ? summary.getCustomerName() : "");
            result.put("customer", custMap);

            if (effectiveOrderId != null && !effectiveOrderId.isEmpty()) {
                // Single order requested: return orderId, totalReviews, reviews
                result.put("orderId", effectiveOrderId);
                result.put("totalReviews", summary.getTotalReviews());

                List<Map<String, Object>> cleanReviews = new ArrayList<>();
                if (summary.getReviews() != null) {
                    for (CustomerReviewItemDto r : summary.getReviews()) {
                        Map<String, Object> rMap = new LinkedHashMap<>();
                        rMap.put("milestoneName", r.getMilestoneName());
                        rMap.put("status", r.getStatus());
                        rMap.put("satisfaction", r.getSatisfaction());
                        if (r.getNotes() != null && !r.getNotes().trim().isEmpty()) {
                            rMap.put("notes", r.getNotes().trim());
                        }
                        rMap.put("checkedBy", r.getCheckedBy());
                        if (r.getReviewedAt() != null) {
                            String dateStr = LocalDate.ofInstant(r.getReviewedAt(), ZoneOffset.UTC).toString();
                            rMap.put("reviewedAt", dateStr);
                        } else {
                            rMap.put("reviewedAt", null);
                        }
                        cleanReviews.add(rMap);
                    }
                }
                result.put("reviews", cleanReviews);
            } else {
                // Customer-wide lookup across orders: group reviews by order!
                result.put("totalReviews", summary.getTotalReviews());

                Map<String, List<Map<String, Object>>> reviewsByOrder = new LinkedHashMap<>();
                if (summary.getReviews() != null) {
                    for (CustomerReviewItemDto r : summary.getReviews()) {
                        String oNum = (r.getOrderNumber() != null && !r.getOrderNumber().trim().isEmpty())
                                ? r.getOrderNumber() : (r.getOrderId() != null ? String.valueOf(r.getOrderId()) : "N/A");
                        Map<String, Object> rMap = new LinkedHashMap<>();
                        rMap.put("milestoneName", r.getMilestoneName());
                        rMap.put("status", r.getStatus());
                        rMap.put("satisfaction", r.getSatisfaction());
                        if (r.getNotes() != null && !r.getNotes().trim().isEmpty()) {
                            rMap.put("notes", r.getNotes().trim());
                        }
                        rMap.put("checkedBy", r.getCheckedBy());
                        if (r.getReviewedAt() != null) {
                            String dateStr = LocalDate.ofInstant(r.getReviewedAt(), ZoneOffset.UTC).toString();
                            rMap.put("reviewedAt", dateStr);
                        } else {
                            rMap.put("reviewedAt", null);
                        }
                        reviewsByOrder.computeIfAbsent(oNum, k -> new ArrayList<>()).add(rMap);
                    }
                }

                List<Map<String, Object>> ordersList = new ArrayList<>();
                for (Map.Entry<String, List<Map<String, Object>>> entry : reviewsByOrder.entrySet()) {
                    Map<String, Object> oMap = new LinkedHashMap<>();
                    oMap.put("orderId", entry.getKey());
                    oMap.put("totalReviews", entry.getValue().size());
                    oMap.put("reviews", entry.getValue());
                    ordersList.add(oMap);
                }
                result.put("orders", ordersList);
            }
            return result;
        }

        // Return ALL customers who have completed reviews (filter out customers with 0 reviews)
        Map<Long, CustomerReviewSummaryDto> summaryMap = new LinkedHashMap<>();
        List<Customer> allCusts = customerRepository.findAll();
        for (Customer c : allCusts) {
            CustomerReviewSummaryDto s = buildCustomerReviewSummary(c, null);
            if (s.getTotalReviews() > 0) {
                summaryMap.put(c.getId(), s);
            }
        }

        List<CustomerReviewSummaryDto> reviewedList = new ArrayList<>(summaryMap.values());
        int totalReviewsSum = reviewedList.stream().mapToInt(CustomerReviewSummaryDto::getTotalReviews).sum();

        result.put("totalReviewedCustomers", reviewedList.size());
        result.put("totalReviews", totalReviewsSum);
        result.put("customers", reviewedList);
        result.put("message", "Reviewed customers retrieved successfully");
        return result;
    }

    private CustomerReviewSummaryDto buildCustomerReviewSummary(Customer customer, String orderFilter) {
        CustomerReviewSummaryDto summary = new CustomerReviewSummaryDto();
        summary.setCustomerId(customer.getId());
        summary.setExternalId(customer.getExternalId() != null ? customer.getExternalId() : String.valueOf(customer.getId()));
        summary.setCustomerName(customer.getName());
        summary.setPhoneNumber(customer.getPhoneNumber());
        summary.setCity(customer.getCity());

        List<CustomerOrder> orders = customerOrderRepository.findByCustomerIdOrderByOrderDateDesc(customer.getId());
        summary.setTotalOrders(orders.size());
        if (!orders.isEmpty()) {
            summary.setLastOrderDate(orders.get(0).getOrderDate());
        }

        List<Long> orderIds = orders.stream().map(CustomerOrder::getId).toList();
        List<OrderFollowupCheck> checks = orderIds.isEmpty() ? Collections.emptyList() : followupCheckRepository.findByOrderIds(orderIds);

        List<CustomerReviewItemDto> reviewItems = new ArrayList<>();
        OrderFollowupCheck latestRated = null;
        Instant latestRatedTime = null;

        for (OrderFollowupCheck ch : checks) {
            boolean isCompleted = ch.isCompleted()
                    || "COMPLETED".equalsIgnoreCase(ch.getStatus())
                    || "FINISHED".equalsIgnoreCase(ch.getStatus());

            boolean hasContent = (ch.getNotes() != null && !ch.getNotes().trim().isEmpty())
                    || (ch.getSatisfaction() != null && !ch.getSatisfaction().trim().isEmpty())
                    || (ch.getImageUrl() != null && !ch.getImageUrl().trim().isEmpty());

            boolean isUnfinished = "UNFINISHED".equalsIgnoreCase(ch.getStatus())
                    || "EXPIRED".equalsIgnoreCase(ch.getStatus());

            // Strict filter: MUST be completed / reviewed, NOT marked unfinished, and has feedback content
            if (!isCompleted || isUnfinished || !hasContent) {
                continue;
            }

            CustomerOrder ord = ch.getOrder();
            if (orderFilter != null && !orderFilter.trim().isEmpty() && ord != null) {
                String cleanFilter = orderFilter.trim();
                boolean matchesOrder = cleanFilter.equalsIgnoreCase(ord.getOrderNumber())
                        || cleanFilter.equals(String.valueOf(ord.getId()));
                if (!matchesOrder) {
                    continue;
                }
            }

            CustomerReviewItemDto item = new CustomerReviewItemDto();
            item.setCheckId(ch.getId());
            item.setOrderId(ord != null ? ord.getId() : null);
            item.setOrderNumber(ord != null ? ord.getOrderNumber() : null);
            item.setOrderDate(ord != null ? ord.getOrderDate() : null);
            item.setMilestoneName(ch.getMilestoneName());
            item.setStatus("COMPLETED");
            item.setSatisfaction(ch.getSatisfaction() != null ? ch.getSatisfaction().toUpperCase() : "NEUTRAL");
            item.setNotes(ch.getNotes() != null ? ch.getNotes().trim() : "");
            item.setImageUrl(ch.getImageUrl() != null ? ch.getImageUrl() : "");
            item.setImages(parseImages(ch.getImageUrl()));
            item.setCheckedBy(ch.getCheckedBy() != null ? ch.getCheckedBy() : "Admin");
            Instant rTime = ch.getCheckedAt() != null ? ch.getCheckedAt() :
                    (ch.getCompletedDate() != null ? ch.getCompletedDate() : ch.getUpdatedAt());
            item.setReviewedAt(rTime);
            item.setCompletedDate(ch.getCompletedDate() != null ? ch.getCompletedDate() : rTime);

            reviewItems.add(item);

            if (ch.getSatisfaction() != null && (latestRatedTime == null || (rTime != null && rTime.isAfter(latestRatedTime)))) {
                latestRated = ch;
                latestRatedTime = rTime;
            }
        }

        reviewItems.sort((a, b) -> {
            if (a.getReviewedAt() != null && b.getReviewedAt() != null) {
                return b.getReviewedAt().compareTo(a.getReviewedAt());
            }
            return Long.compare(b.getCheckId(), a.getCheckId());
        });

        summary.setReviews(reviewItems);
        summary.setTotalReviews(reviewItems.size());
        if (latestRated != null) {
            summary.setLatestSatisfaction(latestRated.getSatisfaction().toUpperCase());
            summary.setLatestReviewDate(latestRatedTime);
        }

        return summary;
    }

    private Customer resolveCustomer(String customerId, String orderId, String customerName) {
        if (customerId != null && !customerId.trim().isEmpty()) {
            String cleanId = customerId.trim();
            Customer c = customerRepository.findByExternalId(cleanId).orElse(null);
            if (c != null) return c;
            try {
                Long numId = Long.parseLong(cleanId);
                c = customerRepository.findById(numId).orElse(null);
                if (c != null) return c;
            } catch (NumberFormatException ignored) {}
        }

        if (orderId != null && !orderId.trim().isEmpty()) {
            String cleanOrd = orderId.trim();
            List<CustomerOrder> orders = customerOrderRepository.findByOrderNumberWithCustomer(cleanOrd);
            if (orders != null && !orders.isEmpty() && orders.get(0).getCustomer() != null) {
                return orders.get(0).getCustomer();
            }
            try {
                Long numOrd = Long.parseLong(cleanOrd);
                CustomerOrder o = customerOrderRepository.findById(numOrd).orElse(null);
                if (o != null && o.getCustomer() != null) {
                    return o.getCustomer();
                }
            } catch (NumberFormatException ignored) {}
        }

        if (customerName != null && !customerName.trim().isEmpty()) {
            List<Customer> list = customerRepository.findByNameContainingIgnoreCase(customerName.trim());
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    private List<String> parseImages(String rawImages) {
        if (rawImages == null || rawImages.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String trimmed = rawImages.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<String> list = mapper.readValue(trimmed, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                if (list != null) return list;
            } catch (Exception ignored) {}
        }
        return List.of(trimmed);
    }
}

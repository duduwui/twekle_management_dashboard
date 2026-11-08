package com.twekl.dashboard.dto.administration;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object summarizing a customer's reviewed follow-ups.
 * Includes only customers with completed reviews (excludes pending/unfinished).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerReviewSummaryDto {

    private Long customerId;
    private String externalId;
    private String customerName;
    private String phoneNumber;
    private String city;
    private int totalOrders;
    private Instant lastOrderDate;
    private int totalReviews;
    private String latestSatisfaction;
    private Instant latestReviewDate;
    private List<CustomerReviewItemDto> reviews = new ArrayList<>();

    public CustomerReviewSummaryDto() {}

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public Instant getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(Instant lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public String getLatestSatisfaction() { return latestSatisfaction; }
    public void setLatestSatisfaction(String latestSatisfaction) { this.latestSatisfaction = latestSatisfaction; }

    public Instant getLatestReviewDate() { return latestReviewDate; }
    public void setLatestReviewDate(Instant latestReviewDate) { this.latestReviewDate = latestReviewDate; }

    public List<CustomerReviewItemDto> getReviews() { return reviews; }
    public void setReviews(List<CustomerReviewItemDto> reviews) {
        this.reviews = reviews != null ? reviews : new ArrayList<>();
        this.totalReviews = this.reviews.size();
    }
}

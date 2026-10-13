package com.twekl.dashboard.dto.administration;

import com.twekl.dashboard.model.administration.Customer;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * DTO for Customer Satisfaction Reports.
 */
public class SatisfactionReportDto {

    private Long id;
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private String city;
    private Long orderId;
    private String orderNumber;
    private int rating;
    private String comment;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant feedbackDate;
    private String followupCheckedBy;
    private String satisfaction;
    private String milestoneName;

    public SatisfactionReportDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(Instant feedbackDate) { this.feedbackDate = feedbackDate; }

    public String getFollowupCheckedBy() { return followupCheckedBy; }
    public void setFollowupCheckedBy(String followupCheckedBy) { this.followupCheckedBy = followupCheckedBy; }

    public String getSatisfaction() { return satisfaction; }
    public void setSatisfaction(String satisfaction) { this.satisfaction = satisfaction; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
}


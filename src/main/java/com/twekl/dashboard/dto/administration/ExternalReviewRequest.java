package com.twekl.dashboard.dto.administration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for external review ingestion.
 * Strictly 4 simple fields with full case-insensitivity:
 * 1. customerName (aliases: customer_name, customername, name)
 * 2. customerId   (aliases: customer_id, customerid, id)
 * 3. orderId      (aliases: order_id, orderid, orderNumber, order_number)
 * 4. date         (aliases: orderDate, order_date, orderdate)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalReviewRequest {

    @JsonProperty("customerName")
    @JsonAlias({"customer_name", "customername", "name", "customer"})
    private String customerName;

    @JsonProperty("customerId")
    @JsonAlias({"customer_id", "customerid", "id"})
    private String customerId;

    @JsonProperty("orderId")
    @JsonAlias({"order_id", "orderid", "orderNumber", "order_number"})
    private String orderId;

    @JsonProperty("date")
    @JsonAlias({"orderDate", "order_date", "orderdate"})
    private String date;

    @JsonProperty("username")
    @JsonAlias({"user", "sub"})
    private String username;

    @JsonProperty("role")
    private String role;

    public ExternalReviewRequest() {}

    public ExternalReviewRequest(String customerName, String customerId, String orderId, String date) {
        this.customerName = customerName;
        this.customerId = customerId;
        this.orderId = orderId;
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "ExternalReviewRequest{" +
                "customerName='" + customerName + '\'' +
                ", customerId='" + customerId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}

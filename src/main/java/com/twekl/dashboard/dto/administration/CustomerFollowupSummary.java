package com.twekl.dashboard.dto.administration;

import com.twekl.dashboard.model.administration.Customer;

/**
 * Data Transfer Object for Customer with follow-up calculation metrics.
 * Decouples runtime status calculations from the persistent JPA Customer entity.
 */
public class CustomerFollowupSummary {

    private Customer customer;
    private String followupStatus = "IDLE"; // ALERT, IDLE, DONE
    private String nextPendingFollowup;
    private Long nextPendingOrderId;
    private boolean allFollowupsCompleted = false;
    private int remainingFollowupsCount = 0;
    private Integer nextPendingMinutesLeft;

    public CustomerFollowupSummary() {}

    public CustomerFollowupSummary(Customer customer, String followupStatus, String nextPendingFollowup, Long nextPendingOrderId, boolean allFollowupsCompleted, int remainingFollowupsCount, Integer nextPendingMinutesLeft) {
        this.customer = customer;
        this.followupStatus = followupStatus;
        this.nextPendingFollowup = nextPendingFollowup;
        this.nextPendingOrderId = nextPendingOrderId;
        this.allFollowupsCompleted = allFollowupsCompleted;
        this.remainingFollowupsCount = remainingFollowupsCount;
        this.nextPendingMinutesLeft = nextPendingMinutesLeft;
    }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getFollowupStatus() { return followupStatus; }
    public void setFollowupStatus(String followupStatus) { this.followupStatus = followupStatus; }

    public String getNextPendingFollowup() { return nextPendingFollowup; }
    public void setNextPendingFollowup(String nextPendingFollowup) { this.nextPendingFollowup = nextPendingFollowup; }

    public Long getNextPendingOrderId() { return nextPendingOrderId; }
    public void setNextPendingOrderId(Long nextPendingOrderId) { this.nextPendingOrderId = nextPendingOrderId; }

    public boolean isAllFollowupsCompleted() { return allFollowupsCompleted; }
    public void setAllFollowupsCompleted(boolean allFollowupsCompleted) { this.allFollowupsCompleted = allFollowupsCompleted; }

    public int getRemainingFollowupsCount() { return remainingFollowupsCount; }
    public void setRemainingFollowupsCount(int remainingFollowupsCount) { this.remainingFollowupsCount = remainingFollowupsCount; }

    public Integer getNextPendingMinutesLeft() { return nextPendingMinutesLeft; }
    public void setNextPendingMinutesLeft(Integer nextPendingMinutesLeft) { this.nextPendingMinutesLeft = nextPendingMinutesLeft; }
}


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

    public boolean isOverdue() {
        return "ALERT".equalsIgnoreCase(followupStatus) || (nextPendingMinutesLeft != null && nextPendingMinutesLeft <= 0);
    }

    public String getFormattedRemainingTime() {
        if (nextPendingMinutesLeft == null) {
            return "Scheduled";
        }
        int mins = nextPendingMinutesLeft;
        if (mins >= 0) {
            if (mins < 60) return mins + "m left";
            int hours = mins / 60;
            if (hours < 24) return hours + "h " + (mins % 60) + "m left";
            int days = hours / 24;
            int remH = hours % 24;
            return remH > 0 ? (days + "d " + remH + "h left") : (days + "d left");
        } else {
            int overdue = Math.abs(mins);
            if (overdue < 60) return overdue + "m overdue";
            int ovH = overdue / 60;
            if (ovH < 24) return ovH + "h overdue";
            int ovD = ovH / 24;
            int ovRH = ovH % 24;
            return ovRH > 0 ? (ovD + "d " + ovRH + "h overdue") : (ovD + "d overdue");
        }
    }
}


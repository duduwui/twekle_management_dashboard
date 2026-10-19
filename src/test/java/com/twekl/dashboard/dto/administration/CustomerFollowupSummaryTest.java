package com.twekl.dashboard.dto.administration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerFollowupSummaryTest {

    @Test
    void testFormattedRemainingTime() {
        CustomerFollowupSummary summary = new CustomerFollowupSummary();

        // Null
        summary.setNextPendingMinutesLeft(null);
        assertEquals("Scheduled", summary.getFormattedRemainingTime());

        // Upcoming (< 60m)
        summary.setNextPendingMinutesLeft(15);
        assertEquals("15m left", summary.getFormattedRemainingTime());

        // Upcoming exact hour
        summary.setNextPendingMinutesLeft(60);
        assertEquals("1h left", summary.getFormattedRemainingTime());

        // Upcoming hour + minutes
        summary.setNextPendingMinutesLeft(75);
        assertEquals("1h 15m left", summary.getFormattedRemainingTime());

        // Overdue (< 60m)
        summary.setNextPendingMinutesLeft(-15);
        assertEquals("15m overdue", summary.getFormattedRemainingTime());

        // Overdue exact 1 hour
        summary.setNextPendingMinutesLeft(-60);
        assertEquals("1h overdue", summary.getFormattedRemainingTime());

        // Overdue 1 hour 15 minutes (Fixes the issue where 1h was shown without minutes)
        summary.setNextPendingMinutesLeft(-75);
        assertEquals("1h 15m overdue", summary.getFormattedRemainingTime());

        // Overdue 2 hours 40 minutes
        summary.setNextPendingMinutesLeft(-160);
        assertEquals("2h 40m overdue", summary.getFormattedRemainingTime());

        // Overdue exact days
        summary.setNextPendingMinutesLeft(-1440);
        assertEquals("1d overdue", summary.getFormattedRemainingTime());

        // Overdue days + hours
        summary.setNextPendingMinutesLeft(-1560);
        assertEquals("1d 2h overdue", summary.getFormattedRemainingTime());
    }
}

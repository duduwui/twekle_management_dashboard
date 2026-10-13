package com.twekl.dashboard.service.administration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.twekl.dashboard.model.administration.*;
import com.twekl.dashboard.repository.administration.*;

import org.junit.jupiter.api.Test;

import java.util.*;

class FollowupServiceTest {
    private final CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    private final OrderFollowupCheckRepository checks = mock(OrderFollowupCheckRepository.class);
    private final TimeFilterPresetRepository presets = mock(TimeFilterPresetRepository.class);
    private final FollowupService service = new FollowupService(orders, checks, presets);

    @Test
    void checklistReadNeverCreatesOrSynchronizesRecords() {
        CustomerOrder order = new CustomerOrder();
        when(orders.findWithCustomerById(7L)).thenReturn(Optional.of(order));
        when(checks.findByOrderId(7L)).thenReturn(List.of());
        assertTrue(service.findChecksByOrderId(7L).isEmpty());
        verify(checks, never()).save(any());
        verifyNoInteractions(presets);
        verify(orders, never()).lockById(any());
    }

    @Test
    void reconciliationLocksParentAndKeepsExistingHistory() {
        CustomerOrder order = new CustomerOrder();
        TimeFilterPreset preset = new TimeFilterPreset();
        preset.setId(3L);
        OrderFollowupCheck existing = new OrderFollowupCheck(order, preset);
        existing.setCompleted(true);
        when(orders.lockById(7L)).thenReturn(Optional.of(order));
        when(checks.findByOrderId(7L)).thenReturn(List.of(existing));
        when(presets.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(preset));
        service.synchronizeChecks(7L);
        var sequence = inOrder(orders, checks, presets);
        sequence.verify(orders).lockById(7L);
        sequence.verify(checks).findByOrderId(7L);
        verify(checks, never()).save(any());
        assertTrue(existing.isCompleted());
    }

    @Test
    void invalidImageAndRatingAreRejectedBeforeDatabaseAccess() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveCheck(1, "", "javascript:alert(1)", "", false, "actor"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveCheck(1, "", "", "FAKE", false, "actor"));
        verifyNoInteractions(checks);
    }
}

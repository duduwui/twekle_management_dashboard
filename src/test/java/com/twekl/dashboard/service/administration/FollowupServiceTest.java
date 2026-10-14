package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.repository.administration.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FollowupServiceTest {
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    private final OrderFollowupCheckRepository checks = mock(OrderFollowupCheckRepository.class);
    private final TimeFilterPresetRepository presets = mock(TimeFilterPresetRepository.class);
    private final FollowupService service = new FollowupService(customers, orders, checks, presets);

    @Test
    void emptyDirectoryDoesNotReadOrdersChecksOrPresets() {
        when(customers.findAll()).thenReturn(List.of());
        assertTrue(service.findAllSummaries("ALL").isEmpty());
        verifyNoInteractions(orders, checks, presets);
    }

    @Test
    void filteredDirectoryLoadsOnlyMatchingCustomerDependencies() {
        Customer customer = new Customer(7L, "Customer", "123", null, null);
        when(customers.findByLastOrderDateAfter(any())).thenReturn(List.of(customer));
        var summaries = service.findAllSummaries("7_DAYS");
        assertEquals(1, summaries.size());
        assertEquals("IDLE", summaries.getFirst().getFollowupStatus());
        verify(customers, never()).findAll();
        verify(orders).findByCustomerIds(List.of(7L));
        verify(checks).findByCustomerIds(List.of(7L));
        verify(orders, never()).findAll();
        verify(checks, never()).findAll();
    }
}

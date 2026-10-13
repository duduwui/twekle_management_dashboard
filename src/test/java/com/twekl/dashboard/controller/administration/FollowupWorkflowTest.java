package com.twekl.dashboard.controller.administration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.twekl.dashboard.config.administration.*;
import com.twekl.dashboard.model.administration.*;
import com.twekl.dashboard.repository.administration.*;
import com.twekl.dashboard.service.administration.FollowupService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration(
            name = "root",
            classes = {JpaConfig.class, SecurityConfig.class}),
    @ContextConfiguration(name = "mvc", classes = WebMvcConfig.class)
})
@Transactional
@WithMockUser(username = "twekl_super_admin", roles = "SUPER_ADMIN")
class FollowupWorkflowTest {
    @Autowired WebApplicationContext context;
    @Autowired CustomerRepository customers;
    @Autowired CustomerOrderRepository orders;
    @Autowired OrderFollowupCheckRepository checks;
    @Autowired TimeFilterPresetRepository presets;
    @Autowired PortalQueryRepository queries;
    @Autowired FollowupService followups;
    MockMvc mvc;
    Customer customer;
    CustomerOrder order;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customer =
                customers.saveAndFlush(
                        new Customer(null, "Review " + UUID.randomUUID(), "123", null, null));
        order =
                orders.saveAndFlush(
                        new CustomerOrder(
                                "T-" + UUID.randomUUID().toString().substring(0, 20),
                                customer,
                                "Tea; Coffee",
                                1,
                                12.5,
                                Instant.now()));
    }

    @Test
    void readsRenderHistoryWithoutCreatingChecks() throws Exception {
        long before = checks.count();
        mvc.perform(get("/admin/followups/customers/" + customer.getId() + "/orders"))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/followups/customers/" + customer.getId() + "/feedback"))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/followups/orders/" + order.getId())).andExpect(status().isOk());
        assertEquals(before, checks.count());
    }

    @Test
    void reconciliationAndCompletionAreIdempotentAndAudited() throws Exception {
        String route = "/admin/followups/orders/" + order.getId() + "/synchronize";
        mvc.perform(post(route)).andExpect(status().isForbidden());
        mvc.perform(post(route).with(csrf())).andExpect(status().is3xxRedirection());
        checks.flush();
        int size = checks.findByOrderId(order.getId()).size();
        mvc.perform(post(route).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals(size, checks.findByOrderId(order.getId()).size());
        var check = checks.findByOrderId(order.getId()).getFirst();
        for (int attempt = 0; attempt < 2; attempt++)
            mvc.perform(
                            post("/admin/followups/checks/" + check.getId())
                                    .with(csrf())
                                    .param("completed", "true")
                                    .param("notes", "Test note")
                                    .param("satisfaction", "SATISFIED")
                                    .param("checkedBy", "forged"))
                    .andExpect(status().is3xxRedirection());
        assertTrue(check.isCompleted());
        assertEquals("twekl_super_admin", check.getCheckedBy());
        mvc.perform(get("/admin/followups/orders/" + order.getId())).andExpect(status().isOk());
    }

    @Test
    void directoryPagesHaveStableBoundariesAndEscapedSearch() {
        String prefix = "Page_" + UUID.randomUUID();
        for (int i = 0; i < 30; i++)
            customers.save(new Customer(null, prefix + i, "123", null, null));
        customers.flush();
        var first = queries.customers(prefix, "ALL", null, 0);
        var second = queries.customers(prefix, "ALL", null, 1);
        assertEquals(30, first.getTotalElements());
        assertEquals(25, first.getNumberOfElements());
        assertEquals(5, second.getNumberOfElements());
        var ids = first.stream().map(r -> r.get("id")).collect(java.util.stream.Collectors.toSet());
        assertTrue(second.stream().noneMatch(r -> ids.contains(r.get("id"))));
        assertEquals(0, queries.customers(prefix + "%", "ALL", null, 0).getTotalElements());
    }

    @Test
    void legacyAuthPostCannotBypassCsrf() throws Exception {
        mvc.perform(post("/admin/auth/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/auth/login")).andExpect(status().isForbidden());
    }

    @Test
    void exportEscapesSpreadsheetFormulaAndQuotes() {
        assertEquals("\"'=SUM(A1)\"", ReportController.csv("=SUM(A1)"));
        assertEquals("\"a\"\"b\"", ReportController.csv("a\"b"));
    }
}

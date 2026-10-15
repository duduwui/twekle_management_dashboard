package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.config.JpaConfig;
import com.twekl.dashboard.config.WebMvcConfig;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.security.administration.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@Transactional
@WebAppConfiguration
@ContextConfiguration(classes = {JpaConfig.class, WebMvcConfig.class, SecurityConfig.class})
public class OrderControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .defaultRequest(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /admin/orders/{id} unauthenticated as JSON returns 401 Unauthorized")
    public void testGetOrderUnauthenticatedJson() throws Exception {
        mockMvc.perform(get("/admin/orders/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/orders/{id} unauthenticated as HTML redirects to /login")
    public void testGetOrderUnauthenticatedHtml() throws Exception {
        mockMvc.perform(get("/admin/orders/1").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/orders/{id} for non-existent order returns 404")
    public void testGetNonExistentOrder() throws Exception {
        mockMvc.perform(get("/admin/orders/999999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found with ID: 999999"));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/orders/{id} for existing order returns order data")
    public void testGetExistingOrder() throws Exception {
        Customer customer = new Customer(null, "Test Customer", "+966500000000", "test@twekl.com", "Riyadh");
        customer = customerRepository.save(customer);

        CustomerOrder order = new CustomerOrder("ORD-TEST-1001", customer, "Perfume Sample", 2, 199.50, Instant.now());
        order = customerOrderRepository.save(order);

        mockMvc.perform(get("/admin/orders/" + order.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-TEST-1001"))
                .andExpect(jsonPath("$.data.totalAmount").value(199.50));
    }

    @Test
    @WithMockUser(username = "twekl_super_admin", roles = {"SUPER_ADMIN"})
    @DisplayName("GET /admin/orders/{id}/followups returns followups array")
    public void testGetOrderFollowupChecks() throws Exception {
        mockMvc.perform(get("/admin/orders/1/followups").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}

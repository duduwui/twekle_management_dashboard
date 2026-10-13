package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CustomerForm;
import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.service.administration.CustomerService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {
    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    public String directory() {
        return "redirect:/admin/followups";
    }

    @GetMapping("/new")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','CREATE')")
    public String createForm(Model model) {
        model.addAttribute("customerForm", new CustomerForm("", "", "", ""));
        model.addAttribute("activeTab", "followups");
        return "administration/followup/customerForm";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','UPDATE')")
    public String editForm(@PathVariable long id, Model model) {
        Customer c =
                customers
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        model.addAttribute(
                "customerForm",
                new CustomerForm(c.getName(), c.getPhoneNumber(), c.getEmail(), c.getCity()));
        model.addAttribute("customerId", id);
        model.addAttribute("activeTab", "followups");
        return "administration/followup/customerForm";
    }

    @PostMapping
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','CREATE')")
    public String create(
            @Valid @ModelAttribute CustomerForm customerForm, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("activeTab", "followups");
            return "administration/followup/customerForm";
        }
        customers.createCustomer(entity(customerForm));
        return "redirect:/admin/followups";
    }

    @PostMapping("/{id}")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','UPDATE')")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute CustomerForm customerForm,
            BindingResult errors,
            Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("customerId", id);
            model.addAttribute("activeTab", "followups");
            return "administration/followup/customerForm";
        }
        customers.updateCustomer(id, entity(customerForm));
        return "redirect:/admin/followups";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','DELETE')")
    public String delete(@PathVariable long id) {
        customers.deleteCustomer(id);
        return "redirect:/admin/followups";
    }

    private Customer entity(CustomerForm form) {
        return new Customer(
                null, form.name().trim(), form.phoneNumber().trim(), form.email(), form.city());
    }
}

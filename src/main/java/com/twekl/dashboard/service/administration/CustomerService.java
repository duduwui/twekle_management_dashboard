package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.repository.administration.CustomerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Contact details and customer lifecycle mutations. */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long customerId, Customer updated) {
        Customer existing =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer not found with ID: " + customerId));

        if (updated.getName() != null) existing.setName(updated.getName().trim());
        if (updated.getPhoneNumber() != null)
            existing.setPhoneNumber(updated.getPhoneNumber().trim());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail().trim());
        if (updated.getCity() != null) existing.setCity(updated.getCity().trim());

        return customerRepository.save(existing);
    }

    public void deleteCustomer(Long customerId) {
        Customer existing =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer not found with ID: " + customerId));
        customerRepository.delete(existing);
    }
}

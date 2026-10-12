package com.twekl.dashboard.administration.service.customer;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.repository.customer.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerUpdateService {

    private final CustomerRepository customerRepository;

    public CustomerUpdateService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer updateCustomer(Long id, Customer updated) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getPhoneNumber() != null) existing.setPhoneNumber(updated.getPhoneNumber());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getCity() != null) existing.setCity(updated.getCity());

        return customerRepository.save(existing);
    }
}

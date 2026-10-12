package com.twekl.dashboard.administration.service.customer;

import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.repository.customer.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerCreateService {

    private final CustomerRepository customerRepository;

    public CustomerCreateService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
}

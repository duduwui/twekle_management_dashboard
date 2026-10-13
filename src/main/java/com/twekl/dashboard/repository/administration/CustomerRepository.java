package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.Customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}

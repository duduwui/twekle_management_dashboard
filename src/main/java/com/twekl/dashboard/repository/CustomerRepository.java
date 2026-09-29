package com.twekl.dashboard.repository;

import com.twekl.dashboard.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByStatusOrderByDaysSinceLastOrderAsc(String status);
    List<Customer> findByDaysSinceLastOrderLessThanEqualOrderByDaysSinceLastOrderAsc(Integer days);
    List<Customer> findByDaysSinceLastOrderBetweenOrderByDaysSinceLastOrderAsc(Integer minDays, Integer maxDays);
    List<Customer> findByDaysSinceLastOrderGreaterThanEqualOrderByDaysSinceLastOrderAsc(Integer minDays);
}

package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.CustomerFeedback;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long> {}

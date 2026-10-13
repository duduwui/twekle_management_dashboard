package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller interface contract for Staff Administrators operations.
 */
public interface AdminOperations {

    String showAdmins(Model model);

    String createAdminSsr(CreateAdminRequest request, BindingResult bindingResult, RedirectAttributes flash);

    String toggleAdminStatusSsr(Long adminId, RedirectAttributes flash);

    String deleteAdminSsr(Long adminId, RedirectAttributes flash);
}

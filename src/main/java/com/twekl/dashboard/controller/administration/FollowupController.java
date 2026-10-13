package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.repository.administration.PortalQueryRepository;
import com.twekl.dashboard.service.administration.*;
import com.twekl.dashboard.util.administration.DateUtil;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Set;

@Controller
@RequestMapping("/admin/followups")
@PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','READ')")
public class FollowupController {
    private final PortalQueryRepository queries;
    private final CustomerService customers;
    private final FollowupService followups;

    public FollowupController(
            PortalQueryRepository queries, CustomerService customers, FollowupService followups) {
        this.queries = queries;
        this.customers = customers;
        this.followups = followups;
    }

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "followups";
    }

    @GetMapping
    public String directory(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String period,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        if (search.length() > 100 || !Set.of("ALL", "IDLE", "ALERT", "DONE").contains(status))
            throw new IllegalArgumentException("Invalid directory filter");
        model.addAttribute(
                "rows", queries.customers(search, status, DateUtil.getFilterCutoff(period), page));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("period", period);
        return "administration/followup/followups";
    }

    @GetMapping("/customers/{customerId}/orders")
    public String orders(
            @PathVariable long customerId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute(
                "customer",
                customers
                        .findById(customerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
        model.addAttribute("rows", queries.orders(customerId, page));
        return "administration/followup/orderDirectory";
    }

    @GetMapping("/customers/{customerId}/feedback")
    public String feedback(
            @PathVariable long customerId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute(
                "customer",
                customers
                        .findById(customerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
        model.addAttribute("rows", queries.feedback(customerId, page));
        return "administration/followup/feedbackDirectory";
    }

    @GetMapping("/orders/{orderId}")
    public String checklist(
            @PathVariable long orderId, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("order", followups.order(orderId));
        var rows = queries.checklist(orderId, page);
        model.addAttribute("rows", rows);
        model.addAttribute("checks", rows.getContent());
        return "administration/followup/checklist";
    }

    @PostMapping("/orders/{orderId}/synchronize")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','CREATE')")
    public String synchronize(@PathVariable long orderId, RedirectAttributes flash) {
        followups.synchronizeChecks(orderId);
        flash.addFlashAttribute(
                "successMessage", "Active milestones added. Existing history was preserved.");
        return "redirect:/admin/followups/orders/" + orderId;
    }

    @PostMapping("/checks/{checkId}")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','UPDATE')")
    public String save(
            @PathVariable long checkId,
            @RequestParam(defaultValue = "") String notes,
            @RequestParam(defaultValue = "") String imageUrl,
            @RequestParam(defaultValue = "") String satisfaction,
            @RequestParam(defaultValue = "false") boolean completed,
            Principal principal,
            RedirectAttributes flash) {
        long orderId =
                followups.saveCheck(
                        checkId, notes, imageUrl, satisfaction, completed, principal.getName());
        flash.addFlashAttribute("successMessage", "Checkpoint saved.");
        return "redirect:/admin/followups/orders/" + orderId;
    }

    /** Delivers an attachment on demand; directory HTML never embeds image bodies. */
    @GetMapping("/checks/{checkId}/photo")
    public void photo(@PathVariable long checkId, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        String image = followups.image(checkId);
        if (image.startsWith("https://") || image.startsWith("http://")) {
            response.sendRedirect(image);
            return;
        }
        String prefix =
                image.startsWith("data:image/png;base64,")
                        ? "data:image/png;base64,"
                        : "data:image/jpeg;base64,";
        if (!image.startsWith(prefix) || image.length() > 2800000)
            throw new IllegalArgumentException("Unsupported photo format");
        byte[] bytes = java.util.Base64.getDecoder().decode(image.substring(prefix.length()));
        response.setContentType(prefix.contains("png") ? "image/png" : "image/jpeg");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    @PostMapping("/checks/{checkId}/photo")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','UPDATE')")
    public String upload(
            @PathVariable long checkId,
            @RequestParam org.springframework.web.multipart.MultipartFile photo,
            Principal principal)
            throws java.io.IOException {
        if (photo.getSize() > 1048576)
            throw new IllegalArgumentException("Photo must be at most 1 MB");
        return "redirect:/admin/followups/orders/"
                + followups.uploadImage(checkId, photo.getBytes(), principal.getName());
    }

    @PostMapping("/checks/{checkId}/remove-image")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','UPDATE')")
    public String removeImage(@PathVariable long checkId, Principal principal) {
        return "redirect:/admin/followups/orders/"
                + followups.removeImage(checkId, principal.getName());
    }
}

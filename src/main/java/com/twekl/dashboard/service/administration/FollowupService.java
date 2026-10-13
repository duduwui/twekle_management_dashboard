package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.*;
import com.twekl.dashboard.repository.administration.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class FollowupService {
    private final CustomerOrderRepository orders;
    private final OrderFollowupCheckRepository checks;
    private final TimeFilterPresetRepository presets;

    public FollowupService(
            CustomerOrderRepository orders,
            OrderFollowupCheckRepository checks,
            TimeFilterPresetRepository presets) {
        this.orders = orders;
        this.checks = checks;
        this.presets = presets;
    }

    public String image(long checkId) {
        return checks.findImage(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
    }

    public CustomerOrder order(long orderId) {
        return orders.findWithCustomerById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /** GET reads stored history; adding presets is an explicit, authorized POST. */
    public List<OrderFollowupCheck> findChecksByOrderId(Long orderId) {
        CustomerOrder order = order(orderId);
        Instant now = Instant.now();
        List<OrderFollowupCheck> result = checks.findByOrderId(orderId);
        for (OrderFollowupCheck check : result) {
            Instant due =
                    calculateDueTime(
                            order.getOrderDate(),
                            check.getDurationUnit(),
                            check.getDurationValue() == null ? 1 : check.getDurationValue());
            if (due != null) {
                long minutes = java.time.Duration.between(now, due).toMinutes();
                check.setMinutesUntilDue(
                        (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, minutes)));
                check.setIsDue(!check.isCompleted() && due.isBefore(now));
            }
        }
        return result;
    }

    /** Lock the parent so concurrent reconciliation cannot insert the same preset twice. */
    @Transactional
    public void synchronizeChecks(long orderId) {
        CustomerOrder order =
                orders.lockById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        List<OrderFollowupCheck> existing = checks.findByOrderId(orderId);
        Set<Long> ids = new HashSet<>();
        for (OrderFollowupCheck check : existing)
            if (check.getPreset() != null) ids.add(check.getPreset().getId());
        for (TimeFilterPreset preset : presets.findByActiveTrueOrderByDisplayOrderAsc()) {
            if (ids.contains(preset.getId())) continue;
            OrderFollowupCheck legacy =
                    existing.stream()
                            .filter(
                                    c ->
                                            c.getPreset() == null
                                                    && c.getMilestoneName() != null
                                                    && (c.getMilestoneName()
                                                                    .equalsIgnoreCase(
                                                                            preset.getLabelEn())
                                                            || c.getMilestoneName()
                                                                    .equalsIgnoreCase(
                                                                            preset.getPresetKey())))
                            .findFirst()
                            .orElse(null);
            if (legacy != null) legacy.setPreset(preset);
            else checks.save(new OrderFollowupCheck(order, preset));
            ids.add(preset.getId());
        }
    }

    @Transactional
    public long saveCheck(
            long checkId,
            String notes,
            String imageUrl,
            String satisfaction,
            boolean completed,
            String actor) {
        if (notes.length() > 10000)
            throw new IllegalArgumentException("Notes must be at most 10,000 characters");
        if (!Set.of("", "SATISFIED", "NEUTRAL", "UNSATISFIED").contains(satisfaction))
            throw new IllegalArgumentException("Choose a valid satisfaction level");
        if (imageUrl.length() > 2048
                || (!imageUrl.isBlank() && !imageUrl.matches("https?://[^\\s]+")))
            throw new IllegalArgumentException(
                    "Photo must be an HTTP or HTTPS URL of at most 2,048 characters");
        OrderFollowupCheck check =
                checks.findById(checkId)
                        .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
        Instant now = Instant.now();
        check.setNotes(notes.trim());
        // Empty input keeps legacy uploaded images; removal is explicit in the form.
        if (!imageUrl.isBlank()) check.setImageUrl(imageUrl.trim());
        check.setSatisfaction(satisfaction.isBlank() ? null : satisfaction);
        if (completed != check.isCompleted()) check.setCompletedDate(completed ? now : null);
        check.setCompleted(completed);
        check.setStatus(completed ? "COMPLETED" : "PENDING");
        check.setCheckedAt(now);
        check.setCheckedBy(actor);
        return check.getOrder().getId();
    }

    @Transactional
    public long uploadImage(long checkId, byte[] bytes, String actor) {
        if (bytes.length == 0 || bytes.length > 1048576)
            throw new IllegalArgumentException("Photo must be at most 1 MB");
        String mime;
        if (bytes.length >= 8
                && bytes[0] == (byte) 137
                && bytes[1] == 80
                && bytes[2] == 78
                && bytes[3] == 71) mime = "image/png";
        else if (bytes.length >= 3
                && bytes[0] == (byte) 255
                && bytes[1] == (byte) 216
                && bytes[2] == (byte) 255) mime = "image/jpeg";
        else throw new IllegalArgumentException("Choose a PNG or JPEG photo");
        OrderFollowupCheck check =
                checks.findById(checkId)
                        .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
        check.setImageUrl("data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes));
        check.setCheckedBy(actor);
        check.setCheckedAt(Instant.now());
        return check.getOrder().getId();
    }

    @Transactional
    public long removeImage(long checkId, String actor) {
        OrderFollowupCheck check =
                checks.findById(checkId)
                        .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
        check.setImageUrl(null);
        check.setCheckedBy(actor);
        check.setCheckedAt(Instant.now());
        return check.getOrder().getId();
    }

    public static Instant calculateDueTime(Instant start, String unit, int value) {
        if (start == null) return null;
        long days = Math.max(value, 1);
        return switch (unit == null ? "DAYS" : unit.toUpperCase(Locale.ROOT)) {
            case "HOURS" -> start.plus(days, ChronoUnit.HOURS);
            case "WEEKS" -> start.plus(days * 7, ChronoUnit.DAYS);
            case "MONTHS" -> start.plus(days * 30, ChronoUnit.DAYS);
            default -> start.plus(days, ChronoUnit.DAYS);
        };
    }
}

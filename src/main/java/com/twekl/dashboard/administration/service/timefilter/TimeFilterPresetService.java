package com.twekl.dashboard.administration.service.timefilter;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.model.order.OrderFollowupCheck;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.order.CustomerOrderRepository;
import com.twekl.dashboard.administration.repository.order.OrderFollowupCheckRepository;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TimeFilterPresetService {

    private final TimeFilterPresetRepository presetRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final CustomerOrderRepository orderRepository;

    public TimeFilterPresetService(TimeFilterPresetRepository presetRepository,
                                   OrderFollowupCheckRepository followupCheckRepository,
                                   CustomerOrderRepository orderRepository) {
        this.presetRepository = presetRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> findAll() {
        return presetRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> findActive() {
        return presetRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<TimeFilterPreset> findById(Long id) {
        return presetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return presetRepository.count();
    }

    public TimeFilterPreset createPreset(TimeFilterPreset preset) {
        String key = preset.getPresetKey();
        if (key == null || key.trim().isEmpty()) {
            key = preset.getLabelEn() != null
                    ? preset.getLabelEn().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_")
                    : "PRESET_" + System.currentTimeMillis();
        }
        key = key.trim().toUpperCase().replaceAll("^_+|_+$", "");
        if (key.isEmpty()) {
            key = "PRESET_" + System.currentTimeMillis();
        }
        if (presetRepository.existsByPresetKey(key)) {
            key = key + "_" + System.currentTimeMillis();
        }
        preset.setPresetKey(key);

        TimeFilterPreset saved = presetRepository.save(preset);

        // Propagate newly created milestone to all existing orders
        List<CustomerOrder> allOrders = orderRepository.findAll();
        List<OrderFollowupCheck> checksToSave = new ArrayList<>(allOrders.size());
        for (CustomerOrder order : allOrders) {
            checksToSave.add(new OrderFollowupCheck(order, saved));
        }
        if (!checksToSave.isEmpty()) {
            followupCheckRepository.saveAll(checksToSave);
        }

        return saved;
    }

    public TimeFilterPreset updatePreset(Long id, TimeFilterPreset updated) {
        TimeFilterPreset existing = presetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with ID: " + id));

        if (updated.getLabelEn() != null) existing.setLabelEn(updated.getLabelEn());
        if (updated.getLabelAr() != null) existing.setLabelAr(updated.getLabelAr());
        if (updated.getLabelKu() != null) existing.setLabelKu(updated.getLabelKu());
        if (updated.getDurationValue() > 0) existing.setDurationValue(updated.getDurationValue());
        if (updated.getDurationUnit() != null) existing.setDurationUnit(updated.getDurationUnit());
        existing.setActive(updated.isActive());
        if (updated.getDisplayOrder() >= 0) existing.setDisplayOrder(updated.getDisplayOrder());

        return presetRepository.save(existing);
    }

    public TimeFilterPreset toggleActive(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with ID: " + id));
        preset.setActive(!preset.isActive());
        return presetRepository.save(preset);
    }

    public void deletePreset(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with ID: " + id));

        followupCheckRepository.deleteByPresetId(id);
        followupCheckRepository.deleteByPresetName(preset.getLabelEn());
        presetRepository.delete(preset);
    }
}


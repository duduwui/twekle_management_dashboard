package com.twekl.dashboard.modules.timefilter.service;

import com.twekl.dashboard.modules.timefilter.dto.TimeFilterPresetDto;
import com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset;
import com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository;
import com.twekl.dashboard.modules.customer.entity.CustomerOrder;
import com.twekl.dashboard.modules.customer.entity.OrderFollowupCheck;
import com.twekl.dashboard.modules.customer.repository.CustomerOrderRepository;
import com.twekl.dashboard.modules.customer.repository.OrderFollowupCheckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public TimeFilterPreset createPreset(TimeFilterPresetDto dto) {
        String key = dto.getPresetKey();
        if (key == null || key.trim().isEmpty()) {
            key = dto.getLabelEn() != null
                    ? dto.getLabelEn().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_")
                    : "PRESET_" + System.currentTimeMillis();
        }
        key = key.trim().toUpperCase().replaceAll("^_+|_+$", "");
        if (key.isEmpty()) {
            key = "PRESET_" + System.currentTimeMillis();
        }
        // Deduplicate key if already taken so creation never crashes
        if (presetRepository.existsByPresetKey(key)) {
            key = key + "_" + System.currentTimeMillis();
        }

        String labelEn = dto.getLabelEn() != null && !dto.getLabelEn().trim().isEmpty()
                ? dto.getLabelEn().trim()
                : ("Last " + dto.getDurationValue() + " " + dto.getDurationUnit());

        TimeFilterPreset preset = new TimeFilterPreset(
                key,
                labelEn,
                dto.getLabelAr() != null ? dto.getLabelAr().trim() : null,
                dto.getLabelKu() != null ? dto.getLabelKu().trim() : null,
                dto.getDurationValue(),
                dto.getDurationUnit(),
                dto.getDisplayOrder()
        );
        preset.setActive(dto.isActive());
        preset = presetRepository.save(preset);

        // Auto-generate checkpoints for all existing orders so the milestone is immediately tracked
        List<CustomerOrder> allOrders = orderRepository.findAll();
        for (CustomerOrder order : allOrders) {
            OrderFollowupCheck ch = new OrderFollowupCheck(order, labelEn, labelEn, dto.getDurationValue());
            ch.setPresetId(preset.getId());
            ch.setDurationUnit(dto.getDurationUnit() != null ? dto.getDurationUnit() : "DAYS");
            ch.setDurationValue(dto.getDurationValue());
            ch.setStatus("PENDING");
            ch.setIsCompleted(false);
            ch.setCompleted(false);
            followupCheckRepository.save(ch);
        }

        return preset;
    }


    public TimeFilterPreset toggleActive(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found with id: " + id));
        preset.setActive(!preset.isActive());
        return presetRepository.save(preset);
    }

    public void deletePreset(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found with id: " + id));

        // Cascade delete order followup checks linked to this preset
        followupCheckRepository.deleteByPresetId(id);
        if (preset.getLabelEn() != null && !preset.getLabelEn().trim().isEmpty()) {
            followupCheckRepository.deleteByPresetName(preset.getLabelEn().trim());
        }
        if (preset.getPresetKey() != null && !preset.getPresetKey().trim().isEmpty()) {
            followupCheckRepository.deleteByPresetName(preset.getPresetKey().trim());
        }

        presetRepository.delete(preset);
    }

    public long count() {
        return presetRepository.count();
    }
}


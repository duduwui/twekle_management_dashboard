package com.twekl.dashboard.service.administrative;

import com.twekl.dashboard.exception.administrative.ResourceNotFoundException;
import com.twekl.dashboard.model.administrative.TimeFilterPreset;
import com.twekl.dashboard.repository.administrative.TimeFilterPresetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for dynamic time filters, intervals, and milestone presets.
 */
@Service
@Transactional
public class TimeFilterService {

    @Autowired
    private TimeFilterPresetRepository timeFilterPresetRepository;

    public TimeFilterService() {
    }

    @Autowired
    public TimeFilterService(TimeFilterPresetRepository timeFilterPresetRepository) {
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> findAll() {
        return timeFilterPresetRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> findActive() {
        return timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<TimeFilterPreset> findById(Long id) {
        return timeFilterPresetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<TimeFilterPreset> findByPresetKey(String key) {
        return timeFilterPresetRepository.findByPresetKey(key);
    }

    @Transactional(readOnly = true)
    public long count() {
        return timeFilterPresetRepository.count();
    }

    public TimeFilterPreset createPreset(TimeFilterPreset preset) {
        if (preset.getPresetKey() == null || preset.getPresetKey().trim().isEmpty()) {
            String generatedKey = (preset.getDurationValue() + "_" + preset.getDurationUnit()).toUpperCase();
            preset.setPresetKey(generatedKey);
        }

        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            preset.setLabelEn("Last " + preset.getDurationValue() + " " + preset.getDurationUnit().toLowerCase());
        }

        if (preset.getLabelAr() == null || preset.getLabelAr().trim().isEmpty()) {
            preset.setLabelAr(preset.getLabelEn());
        }

        if (preset.getLabelKu() == null || preset.getLabelKu().trim().isEmpty()) {
            preset.setLabelKu(preset.getLabelEn());
        }

        if (preset.getDisplayOrder() <= 0) {
            preset.setDisplayOrder((int) timeFilterPresetRepository.count() + 1);
        }

        return timeFilterPresetRepository.save(preset);
    }

    public TimeFilterPreset updatePreset(Long presetId, TimeFilterPreset updated) {
        TimeFilterPreset preset = timeFilterPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Time filter preset not found with ID: " + presetId));

        if (updated.getLabelEn() != null) preset.setLabelEn(updated.getLabelEn().trim());
        if (updated.getLabelAr() != null) preset.setLabelAr(updated.getLabelAr().trim());
        if (updated.getLabelKu() != null) preset.setLabelKu(updated.getLabelKu().trim());
        if (updated.getDurationValue() > 0) preset.setDurationValue(updated.getDurationValue());
        if (updated.getDurationUnit() != null) preset.setDurationUnit(updated.getDurationUnit());
        if (updated.getDisplayOrder() > 0) preset.setDisplayOrder(updated.getDisplayOrder());
        preset.setActive(updated.isActive());

        return timeFilterPresetRepository.save(preset);
    }

    public TimeFilterPreset toggleStatus(Long presetId) {
        TimeFilterPreset preset = timeFilterPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Time filter preset not found with ID: " + presetId));

        preset.setActive(!preset.isActive());
        return timeFilterPresetRepository.save(preset);
    }

    public void deletePreset(Long presetId) {
        TimeFilterPreset preset = timeFilterPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Time filter preset not found with ID: " + presetId));

        timeFilterPresetRepository.delete(preset);
    }
}

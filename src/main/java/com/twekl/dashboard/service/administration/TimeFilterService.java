package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Service for dynamic time filters, intervals, and milestone presets. */
@Service
@Transactional
public class TimeFilterService {

    private final TimeFilterPresetRepository timeFilterPresetRepository;

    public TimeFilterService(TimeFilterPresetRepository timeFilterPresetRepository) {
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TimeFilterPreset> findPage(int page) {
        return timeFilterPresetRepository.findAll(
                org.springframework.data.domain.PageRequest.of(
                        Math.max(0, page),
                        25,
                        org.springframework.data.domain.Sort.by("displayOrder", "id")));
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
        if (preset.getDurationValue() < 1
                || preset.getDurationValue() > 10000
                || !java.util.Set.of("HOURS", "DAYS", "WEEKS", "MONTHS")
                        .contains(preset.getDurationUnit()))
            throw new IllegalArgumentException(
                    "Choose a duration from 1 to 10,000 and a supported unit");
        if (preset.getLabelEn() != null && preset.getLabelEn().length() > 80)
            throw new IllegalArgumentException("Preset name must be at most 80 characters");
        if (preset.getPresetKey() == null || preset.getPresetKey().trim().isEmpty()) {
            String generatedKey =
                    (preset.getDurationValue() + "_" + preset.getDurationUnit()).toUpperCase();
            preset.setPresetKey(generatedKey);
        }

        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            preset.setLabelEn(
                    "Last "
                            + preset.getDurationValue()
                            + " "
                            + preset.getDurationUnit().toLowerCase());
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
        TimeFilterPreset preset =
                timeFilterPresetRepository
                        .findById(presetId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Time filter preset not found with ID: "
                                                        + presetId));

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
        TimeFilterPreset preset =
                timeFilterPresetRepository
                        .findById(presetId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Time filter preset not found with ID: "
                                                        + presetId));

        preset.setActive(!preset.isActive());
        return timeFilterPresetRepository.save(preset);
    }

    public void deletePreset(Long presetId) {
        TimeFilterPreset preset =
                timeFilterPresetRepository
                        .findById(presetId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Time filter preset not found with ID: "
                                                        + presetId));

        timeFilterPresetRepository.delete(preset);
    }
}

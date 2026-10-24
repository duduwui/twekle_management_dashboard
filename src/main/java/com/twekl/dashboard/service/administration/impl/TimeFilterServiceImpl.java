package com.twekl.dashboard.service.administration.impl;

import com.twekl.dashboard.exception.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;
import com.twekl.dashboard.service.administration.TimeFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Production implementation of TimeFilterService.
 * Manages configurable milestone timeline presets, interval rules, and cache evictions.
 */
@Service
@Transactional
public class TimeFilterServiceImpl implements TimeFilterService {

    private final TimeFilterPresetRepository timeFilterPresetRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;

    @Autowired
    public TimeFilterServiceImpl(final TimeFilterPresetRepository timeFilterPresetRepository,
                                 final OrderFollowupCheckRepository followupCheckRepository) {
        this.timeFilterPresetRepository = timeFilterPresetRepository;
        this.followupCheckRepository = followupCheckRepository;
    }

    @Override
    @Cacheable("timeFilterPresets")
    @Transactional(readOnly = true)
    public List<TimeFilterPreset> findAll() {
        return timeFilterPresetRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimeFilterPreset> findById(Long id) {
        return timeFilterPresetRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return timeFilterPresetRepository.count();
    }

    @Override
    @CacheEvict(value = {"timeFilterPresets", "dashboardSummary", "reportData", "followupSummaries", "customerStats", "satisfactionDetails"}, allEntries = true)
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

        TimeFilterPreset saved = timeFilterPresetRepository.save(preset);
        followupCheckRepository.syncPendingChecks();
        followupCheckRepository.syncOverdueChecks();
        return saved;
    }

    @Override
    @CacheEvict(value = {"timeFilterPresets", "dashboardSummary", "reportData", "followupSummaries", "customerStats", "satisfactionDetails"}, allEntries = true)
    public TimeFilterPreset toggleStatus(Long presetId) {
        TimeFilterPreset preset = timeFilterPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Time filter preset not found with ID: " + presetId));

        preset.setActive(!preset.isActive());
        TimeFilterPreset saved = timeFilterPresetRepository.save(preset);
        followupCheckRepository.syncPendingChecks();
        followupCheckRepository.syncOverdueChecks();
        return saved;
    }

    @Override
    @CacheEvict(value = {"timeFilterPresets", "dashboardSummary", "reportData", "followupSummaries", "customerStats", "satisfactionDetails"}, allEntries = true)
    public void deletePreset(Long presetId) {
        TimeFilterPreset preset = timeFilterPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Time filter preset not found with ID: " + presetId));

        timeFilterPresetRepository.delete(preset);
        followupCheckRepository.syncPendingChecks();
        followupCheckRepository.syncOverdueChecks();
    }
}

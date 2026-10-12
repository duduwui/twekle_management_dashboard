package com.twekl.dashboard.administration.service.timefilter;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TimeFilterUpdateService {

    private final TimeFilterPresetRepository presetRepository;

    public TimeFilterUpdateService(TimeFilterPresetRepository presetRepository) {
        this.presetRepository = presetRepository;
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
}

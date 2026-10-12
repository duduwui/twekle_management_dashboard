package com.twekl.dashboard.administration.service.timefilter;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TimeFilterStatusService {

    private final TimeFilterPresetRepository presetRepository;

    public TimeFilterStatusService(TimeFilterPresetRepository presetRepository) {
        this.presetRepository = presetRepository;
    }

    public TimeFilterPreset toggleActive(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with ID: " + id));
        preset.setActive(!preset.isActive());
        return presetRepository.save(preset);
    }
}

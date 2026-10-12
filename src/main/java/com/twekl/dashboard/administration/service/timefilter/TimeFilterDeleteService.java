package com.twekl.dashboard.administration.service.timefilter;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.followup.OrderFollowupCheckRepository;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TimeFilterDeleteService {

    private final TimeFilterPresetRepository presetRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;

    public TimeFilterDeleteService(TimeFilterPresetRepository presetRepository,
                                   OrderFollowupCheckRepository followupCheckRepository) {
        this.presetRepository = presetRepository;
        this.followupCheckRepository = followupCheckRepository;
    }

    public void deletePreset(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preset not found with ID: " + id));

        followupCheckRepository.deleteByPresetId(id);
        followupCheckRepository.deleteByPresetName(preset.getLabelEn());
        presetRepository.delete(preset);
    }
}

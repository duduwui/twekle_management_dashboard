package com.twekl.dashboard.administration.service.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TimeFilterQueryService {

    private final TimeFilterPresetRepository presetRepository;

    public TimeFilterQueryService(TimeFilterPresetRepository presetRepository) {
        this.presetRepository = presetRepository;
    }

    public List<TimeFilterPreset> findAll() {
        return presetRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<TimeFilterPreset> findActive() {
        return presetRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public Optional<TimeFilterPreset> findById(Long id) {
        return presetRepository.findById(id);
    }

    public long count() {
        return presetRepository.count();
    }
}

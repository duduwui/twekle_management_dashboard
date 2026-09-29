package com.twekl.dashboard.service;

import com.twekl.dashboard.model.TimeFilterPreset;
import com.twekl.dashboard.repository.TimeFilterPresetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TimeFilterPresetService {

    private final TimeFilterPresetRepository repository;

    @Autowired
    public TimeFilterPresetService(TimeFilterPresetRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> getAllPresets() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> getActivePresets() {
        return repository.findByIsActiveTrueOrderByIdAsc();
    }

    public TimeFilterPreset createPreset(TimeFilterPreset preset) {
        if (preset.getIsActive() == null) {
            preset.setIsActive(true);
        }
        return repository.save(preset);
    }

    public TimeFilterPreset togglePresetStatus(Long id) {
        TimeFilterPreset preset = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Filter preset not found with id: " + id));
        preset.setIsActive(!Boolean.TRUE.equals(preset.getIsActive()));
        return repository.save(preset);
    }

    public void deletePreset(Long id) {
        repository.deleteById(id);
    }
}

package com.twekl.dashboard.modules.timefilter.service;

import com.twekl.dashboard.modules.timefilter.dto.TimeFilterPresetDto;
import com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset;
import com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TimeFilterPresetService {

    private final TimeFilterPresetRepository presetRepository;

    public TimeFilterPresetService(TimeFilterPresetRepository presetRepository) {
        this.presetRepository = presetRepository;
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
        if (presetRepository.existsByPresetKey(dto.getPresetKey())) {
            throw new IllegalArgumentException("Preset with key '" + dto.getPresetKey() + "' already exists");
        }

        TimeFilterPreset preset = new TimeFilterPreset(
                dto.getPresetKey().trim(),
                dto.getLabelEn().trim(),
                dto.getLabelAr() != null ? dto.getLabelAr().trim() : null,
                dto.getLabelKu() != null ? dto.getLabelKu().trim() : null,
                dto.getDurationValue(),
                dto.getDurationUnit(),
                dto.getDisplayOrder()
        );
        preset.setActive(dto.isActive());

        return presetRepository.save(preset);
    }

    public TimeFilterPreset toggleActive(Long id) {
        TimeFilterPreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found with id: " + id));
        preset.setActive(!preset.isActive());
        return presetRepository.save(preset);
    }

    public void deletePreset(Long id) {
        if (!presetRepository.existsById(id)) {
            throw new IllegalArgumentException("Preset not found with id: " + id);
        }
        presetRepository.deleteById(id);
    }

    public long count() {
        return presetRepository.count();
    }
}

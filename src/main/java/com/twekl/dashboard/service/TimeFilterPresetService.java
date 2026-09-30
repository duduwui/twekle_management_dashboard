package com.twekl.dashboard.service;

import com.twekl.dashboard.model.TimeFilterPreset;
import com.twekl.dashboard.repository.TimeFilterPresetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimeFilterPresetService {

    private final TimeFilterPresetRepository repository;

    @Autowired
    public TimeFilterPresetService(TimeFilterPresetRepository repository) {
        this.repository = repository;
    }

    public static long toTotalMinutes(TimeFilterPreset p) {
        if (p == null) return 0L;
        Integer val = p.getDurationValue();
        if (val == null) val = 0;
        String unit = p.getDurationUnit() != null ? p.getDurationUnit().trim().toUpperCase() : "HOURS";
        if (unit.startsWith("MIN")) return val;
        if (unit.startsWith("HOUR")) return val * 60L;
        if (unit.startsWith("DAY")) return val * 24L * 60L;
        if (unit.startsWith("WEEK")) return val * 7L * 24L * 60L;
        if (unit.startsWith("MONTH")) return val * 30L * 24L * 60L;
        return val * 60L;
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> getAllPresets() {
        List<TimeFilterPreset> list = new ArrayList<>(repository.findAll());
        list.sort(Comparator.comparingLong(TimeFilterPresetService::toTotalMinutes)
                .thenComparing(p -> p.getId() != null ? p.getId() : 0L));
        return list;
    }

    @Transactional(readOnly = true)
    public List<TimeFilterPreset> getActivePresets() {
        List<TimeFilterPreset> list = repository.findByIsActiveTrueOrderByIdAsc();
        if (list == null) {
            list = repository.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).collect(Collectors.toList());
        } else {
            list = new ArrayList<>(list);
        }
        list.sort(Comparator.comparingLong(TimeFilterPresetService::toTotalMinutes)
                .thenComparing(p -> p.getId() != null ? p.getId() : 0L));
        return list;
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

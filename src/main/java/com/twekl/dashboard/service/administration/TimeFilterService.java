package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for managing operational time filters and milestone presets.
 * Summarizes available time filter preset configuration actions.
 */
public interface TimeFilterService {

    List<TimeFilterPreset> findAll();

    List<TimeFilterPreset> findActive();

    Optional<TimeFilterPreset> findById(Long id);

    Optional<TimeFilterPreset> findByPresetKey(String key);

    long count();

    TimeFilterPreset createPreset(TimeFilterPreset preset);

    TimeFilterPreset updatePreset(Long presetId, TimeFilterPreset updated);

    TimeFilterPreset toggleStatus(Long presetId);

    void deletePreset(Long presetId);
}

package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * Time Filter Presets Service Contract (TimeFilterService.java)
 * ============================================================================
 * 
 * Defines all operations for dynamic time filter intervals, milestone presets,
 * and display ordering for executive filters and follow-up milestones.
 * 
 * Core Capabilities:
 *   1. Filter Discovery: Lists all configured presets sorted by display order.
 *   2. Dynamic Milestones: Adds new timeline milestones (e.g., 24 Hours, 3 Days, 2 Weeks).
 *   3. Real-time Toggling: Activates or deactivates presets, triggering check re-syncs.
 *   4. Cache Synchronization: Evicts all related caches across reports, follow-ups, and dashboard.
 * ============================================================================
 */
public interface TimeFilterService {

    /**
     * Retrieves all timeline presets ordered by display index ascending.
     * Caches result under 'timeFilterPresets'.
     *
     * @return List of all time filter presets.
     */
    List<TimeFilterPreset> findAll();

    /**
     * Finds a single time filter preset by its primary key ID.
     *
     * @param id Preset primary key ID.
     * @return Optional containing TimeFilterPreset if found.
     */
    Optional<TimeFilterPreset> findById(Long id);

    /**
     * Returns total number of configured presets.
     *
     * @return Total count.
     */
    long count();

    /**
     * Persists a new time filter preset and triggers automatic follow-up check re-syncs.
     *
     * @param preset TimeFilterPreset entity to create.
     * @return Persisted TimeFilterPreset entity with ID.
     */
    TimeFilterPreset createPreset(TimeFilterPreset preset);

    /**
     * Toggles the active status of a preset (active <-> inactive) and updates pending check statuses.
     *
     * @param presetId ID of the preset to toggle.
     * @return Updated TimeFilterPreset entity.
     */
    TimeFilterPreset toggleStatus(Long presetId);

    /**
     * Deletes a time filter preset and refreshes system overdue/pending check statuses.
     *
     * @param presetId ID of the preset to delete.
     */
    void deletePreset(Long presetId);
}

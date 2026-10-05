package com.twekl.dashboard.modules.timefilter.repository;

import com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeFilterPresetRepository extends JpaRepository<TimeFilterPreset, Long> {

    // Sort soonest-to-latest by converting all units to hours
    @Query("SELECT p FROM TimeFilterPreset p ORDER BY CASE p.durationUnit " +
           "  WHEN 'HOURS' THEN p.durationValue " +
           "  WHEN 'DAYS'  THEN p.durationValue * 24 " +
           "  WHEN 'WEEKS' THEN p.durationValue * 168 " +
           "  WHEN 'MONTHS' THEN p.durationValue * 720 " +
           "  ELSE p.durationValue END ASC")
    List<TimeFilterPreset> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT p FROM TimeFilterPreset p WHERE p.active = true ORDER BY CASE p.durationUnit " +
           "  WHEN 'HOURS' THEN p.durationValue " +
           "  WHEN 'DAYS'  THEN p.durationValue * 24 " +
           "  WHEN 'WEEKS' THEN p.durationValue * 168 " +
           "  WHEN 'MONTHS' THEN p.durationValue * 720 " +
           "  ELSE p.durationValue END ASC")
    List<TimeFilterPreset> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<TimeFilterPreset> findByPresetKey(String presetKey);
    boolean existsByPresetKey(String presetKey);
}


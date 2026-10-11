package com.twekl.dashboard.administration.repository.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeFilterPresetRepository extends JpaRepository<TimeFilterPreset, Long> {
    List<TimeFilterPreset> findAllByOrderByDisplayOrderAsc();
    List<TimeFilterPreset> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<TimeFilterPreset> findByPresetKey(String presetKey);
    boolean existsByPresetKey(String presetKey);
}

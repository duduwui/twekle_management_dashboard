package com.twekl.dashboard.modules.timefilter;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeFilterPresetRepository extends JpaRepository<TimeFilterPreset, Long> {
    List<TimeFilterPreset> findByIsActiveTrueOrderByIdAsc();
    List<TimeFilterPreset> findAllByOrderByIdAsc();
}

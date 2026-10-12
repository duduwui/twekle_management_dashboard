package com.twekl.dashboard.administration.service.timefilter;

import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.model.order.OrderFollowupCheck;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.followup.OrderFollowupCheckRepository;
import com.twekl.dashboard.administration.repository.order.CustomerOrderRepository;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TimeFilterCreateService {

    private final TimeFilterPresetRepository presetRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final CustomerOrderRepository orderRepository;

    public TimeFilterCreateService(TimeFilterPresetRepository presetRepository,
                                   OrderFollowupCheckRepository followupCheckRepository,
                                   CustomerOrderRepository orderRepository) {
        this.presetRepository = presetRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.orderRepository = orderRepository;
    }

    public TimeFilterPreset createPreset(TimeFilterPreset preset) {
        String key = preset.getPresetKey();
        if (key == null || key.trim().isEmpty()) {
            key = preset.getLabelEn() != null
                    ? preset.getLabelEn().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_")
                    : "PRESET_" + System.currentTimeMillis();
        }
        key = key.trim().toUpperCase().replaceAll("^_+|_+$", "");
        if (key.isEmpty()) {
            key = "PRESET_" + System.currentTimeMillis();
        }
        if (presetRepository.existsByPresetKey(key)) {
            key = key + "_" + System.currentTimeMillis();
        }
        preset.setPresetKey(key);

        TimeFilterPreset saved = presetRepository.save(preset);

        // Propagate newly created milestone to all existing orders
        List<CustomerOrder> allOrders = orderRepository.findAll();
        List<OrderFollowupCheck> checksToSave = new ArrayList<>(allOrders.size());
        for (CustomerOrder order : allOrders) {
            checksToSave.add(new OrderFollowupCheck(order, saved));
        }
        if (!checksToSave.isEmpty()) {
            followupCheckRepository.saveAll(checksToSave);
        }

        return saved;
    }
}

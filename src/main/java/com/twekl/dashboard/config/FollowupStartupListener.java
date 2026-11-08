package com.twekl.dashboard.config;

import com.twekl.dashboard.service.administration.FollowupService;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FollowupStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private final FollowupService followupService;
    private final AtomicBoolean executed = new AtomicBoolean(false);

    public FollowupStartupListener(FollowupService followupService) {
        this.followupService = followupService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (executed.compareAndSet(false, true)) {
            try {
                followupService.syncOverdueStatuses();
            } catch (Exception ignored) {
            }
        }
    }
}

package com.twekl.dashboard.controller.administration;

/**
 * Functional interface mapping transactional controller actions that can throw checked exceptions.
 * Enables clean execution blocks and centralized exception handling inside controller endpoints.
 */
@FunctionalInterface
public interface ControllerAction {
    void execute() throws Exception;
}

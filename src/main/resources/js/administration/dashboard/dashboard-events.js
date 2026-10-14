/**
 * ============================================================================
 * Dashboard Feature: Event Wireframes & Lifecycle Registration (dashboard-events.js)
 * ============================================================================
 * 
 * Layer:    M-V-E Layer 3 (Events)
 * Location: src/main/resources/js/administration/dashboard/dashboard-events.js
 * 
 * Rules:
 *   - Attaches event listeners and interval timers.
 *   - Strictly delegates visual updates to DashboardView.
 * ============================================================================
 */

(function (View) {
    'use strict';

    const pageLoadTime = Date.now();

    function setupInteractionListeners() {
        document.addEventListener('click', function (event) {
            // 1. Handle Dialog Target Openers
            const opener = event.target.closest('[data-dialog]');
            if (opener) {
                const daysAttr = opener.dataset.days;
                View.toggleNativeDialog(
                    opener.dataset.dialog,
                    true,
                    daysAttr !== undefined ? Number(daysAttr) : null
                );
            }

            // 2. Handle Explicit Dialog Closers
            const closeBtn = event.target.closest('[data-close-dialog]');
            if (closeBtn) {
                const parentDialog = closeBtn.closest('dialog');
                if (parentDialog) View.toggleNativeDialog(parentDialog.id, false);
            }

            // 3. Handle Registration Days Filter Buttons
            const filterBtn = event.target.closest('[data-registration-days]');
            if (filterBtn) {
                View.renderFilteredRegistrations(Number(filterBtn.dataset.registrationDays));
            }

            // 4. Close On Dialog Backdrop Click
            if (event.target.tagName === 'DIALOG') {
                const rect = event.target.getBoundingClientRect();
                const clickedOutside = (
                    event.clientY < rect.top || event.clientY > rect.bottom ||
                    event.clientX < rect.left || event.clientX > rect.right
                );
                if (clickedOutside) View.toggleNativeDialog(event.target.id, false);
            }
        });
    }

    function init() {
        setupInteractionListeners();
        View.tickCountdowns(pageLoadTime);

        // Tick countdowns every 60 seconds
        setInterval(function () {
            View.tickCountdowns(pageLoadTime);
        }, 60000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(DashboardView);

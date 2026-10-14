/**
 * ============================================================================
 * Dashboard Feature - Main Entry & Event Wireframe (dashboard-main.js)
 * ============================================================================
 * 
 * Layer:    Main Layer (Event Listeners, Page Init, Intervals)
 * Location: src/main/resources/js/administration/dashboard/dashboard-main.js
 * 
 * Rules:
 *   - Attaches event listeners and interval timers.
 *   - Strictly delegates visual updates to DashboardRenderer.
 * ============================================================================
 */

(function (Renderer) {
    'use strict';

    const pageLoadTime = Date.now();

    function setupInteractionListeners() {
        document.addEventListener('click', function (event) {
            // 1. Handle Dialog Target Openers
            const opener = event.target.closest('[data-dialog]');
            if (opener) {
                const daysAttr = opener.dataset.days;
                Renderer.toggleNativeDialog(
                    opener.dataset.dialog,
                    true,
                    daysAttr !== undefined ? Number(daysAttr) : null
                );
            }

            // 2. Handle Explicit Dialog Closers
            const closeBtn = event.target.closest('[data-close-dialog]');
            if (closeBtn) {
                const parentDialog = closeBtn.closest('dialog');
                if (parentDialog) Renderer.toggleNativeDialog(parentDialog.id, false);
            }

            // 3. Handle Registration Days Filter Buttons
            const filterBtn = event.target.closest('[data-registration-days]');
            if (filterBtn) {
                Renderer.renderFilteredRegistrations(Number(filterBtn.dataset.registrationDays));
            }

            // 4. Close On Dialog Backdrop Click
            if (event.target.tagName === 'DIALOG') {
                const rect = event.target.getBoundingClientRect();
                const clickedOutside = (
                    event.clientY < rect.top || event.clientY > rect.bottom ||
                    event.clientX < rect.left || event.clientX > rect.right
                );
                if (clickedOutside) Renderer.toggleNativeDialog(event.target.id, false);
            }
        });
    }

    function init() {
        setupInteractionListeners();
        Renderer.tickCountdowns(pageLoadTime);

        // Tick countdowns every 60 seconds
        setInterval(function () {
            Renderer.tickCountdowns(pageLoadTime);
        }, 60000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(DashboardRenderer);

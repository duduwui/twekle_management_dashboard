/**
 * ============================================================================
 * Dashboard Feature: UI Component Painters & View Controllers (dashboard-view.js)
 * ============================================================================
 * 
 * Layer:    M-V-E Layer 2 (View)
 * Location: src/main/resources/js/administration/dashboard/dashboard-view.js
 * 
 * Rules:
 *   - Performs DOM lookups and updates innerText / classes / attributes.
 *   - Opens and closes native HTML5 dialogs.
 *   - Delegates calculations to DashboardModule.
 * ============================================================================
 */

const DashboardView = (function (Module) {
    'use strict';

    /**
     * Ticks countdown tags on the priority attention table using current elapsed time.
     * 
     * @param {number} pageLoadTime - Epoch milliseconds of page load.
     */
    function tickCountdowns(pageLoadTime) {
        const elapsedMins = Math.floor((Date.now() - pageLoadTime) / 60000);

        document.querySelectorAll('.live-countdown-tag[data-minutes-left]').forEach(function (el) {
            const initialMins = parseInt(el.getAttribute('data-minutes-left'), 10);
            if (isNaN(initialMins)) return;

            const currentMins = initialMins - elapsedMins;
            el.textContent = Module.formatCountdown(currentMins);

            if (currentMins <= 0) {
                el.classList.add('tag-danger');
                el.classList.remove('tag-warning');
            } else {
                el.classList.add('tag-warning');
                el.classList.remove('tag-danger');
            }
        });
    }

    /**
     * Filters customer registration rows inside the KPI details dialog modal.
     * 
     * @param {number} days - Number of days cutoff (7, 30, or 0 for all).
     */
    function renderFilteredRegistrations(days) {
        const cutoff = days ? Date.now() - (days * 86400000) : 0;
        let visible = 0;

        document.querySelectorAll('[data-registered]').forEach(function (row) {
            const isVisible = Module.isWithinCutoff(row.dataset.registered, cutoff);
            row.hidden = !isVisible;
            if (isVisible) visible++;
        });

        const emptyEl = document.getElementById('registrationsEmpty');
        if (emptyEl) emptyEl.hidden = visible > 0;

        document.querySelectorAll('[data-registration-days]').forEach(function (btn) {
            btn.classList.toggle('btn-primary', Number(btn.dataset.registrationDays) === Number(days));
        });
    }

    /**
     * Generic controller to show or hide native HTML5 <dialog> elements.
     * 
     * @param {string} dialogId - ID of dialog element.
     * @param {boolean} shouldOpen - True to open, false to close.
     * @param {number|null} triggerDays - Optional registration days filter.
     */
    function toggleNativeDialog(dialogId, shouldOpen, triggerDays) {
        const dialog = document.getElementById(dialogId);
        if (!dialog) return;

        if (shouldOpen) {
            if (triggerDays !== undefined && triggerDays !== null) {
                renderFilteredRegistrations(triggerDays);
            }
            typeof dialog.showModal === 'function' ? dialog.showModal() : dialog.setAttribute('open', '');
        } else {
            typeof dialog.close === 'function' ? dialog.close() : dialog.removeAttribute('open');
        }
    }

    return {
        tickCountdowns: tickCountdowns,
        renderFilteredRegistrations: renderFilteredRegistrations,
        toggleNativeDialog: toggleNativeDialog
    };
})(DashboardModule);

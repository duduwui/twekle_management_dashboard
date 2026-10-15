/**
 * ============================================================================
 * Dashboard Feature (dashboard.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/dashboard/dashboard.js
 * Used by:  /WEB-INF/views/administration/dashboard/dashboard.html
 * Route:    /admin/dashboard
 * 
 * Responsibilities:
 *   - Real-time ticking and humanized countdown formatting for priority checks.
 *   - Native HTML5 dialog modal controls (open, close, backdrop click).
 *   - Filtering customer registration rows within the registrations dialog.
 * ============================================================================
 */

(function () {
    'use strict';

    const pageLoadTime = Date.now();

    /**
     * Converts minute values into humanized countdown text strings.
     * 
     * @param {number} mins - Remaining or overdue minutes.
     * @returns {string} Human-friendly string (e.g., "15m left", "2h overdue").
     */
    function formatCountdown(mins) {
        if (mins >= 0) {
            if (mins < 60) return mins + 'm left';
            const hours = Math.floor(mins / 60);
            if (hours < 24) return hours + 'h ' + (mins % 60) + 'm left';
            const days = Math.floor(hours / 24);
            const remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h left') : (days + 'd left');
        } else {
            const overdue = Math.abs(mins);
            if (overdue < 60) return overdue + 'm overdue';
            const ovH = Math.floor(overdue / 60);
            if (ovH < 24) return ovH + 'h overdue';
            const ovD = Math.floor(ovH / 24);
            const ovRH = ovH % 24;
            return ovRH > 0 ? (ovD + 'd ' + ovRH + 'h overdue') : (ovD + 'd overdue');
        }
    }

    /**
     * Ticks countdown tags on the priority attention table using current elapsed time.
     */
    function tickCountdowns() {
        const elapsedMins = Math.floor((Date.now() - pageLoadTime) / 60000);

        document.querySelectorAll('.live-countdown-tag[data-minutes-left]').forEach(function (el) {
            const initialMins = parseInt(el.getAttribute('data-minutes-left'), 10);
            if (isNaN(initialMins)) return;

            const currentMins = initialMins - elapsedMins;
            el.textContent = formatCountdown(currentMins);

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
     * Checks if a registration timestamp falls within a specific millisecond range.
     * 
     * @param {string} registeredDateStr - Date string from data-registered attribute.
     * @param {number} cutoffTimestamp - Epoch cutoff in milliseconds.
     * @returns {boolean} True if within cutoff or cutoff is zero/null.
     */
    function isWithinCutoff(registeredDateStr, cutoffTimestamp) {
        if (!cutoffTimestamp) return true;
        if (!registeredDateStr) return false;
        return Date.parse(registeredDateStr) > cutoffTimestamp;
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
            const isVisible = isWithinCutoff(row.dataset.registered, cutoff);
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

    function setupInteractionListeners() {
        document.addEventListener('click', function (event) {
            // 1. Handle Dialog Target Openers
            const opener = event.target.closest('[data-dialog]');
            if (opener) {
                const daysAttr = opener.dataset.days;
                toggleNativeDialog(
                    opener.dataset.dialog,
                    true,
                    daysAttr !== undefined ? Number(daysAttr) : null
                );
            }

            // 2. Handle Explicit Dialog Closers
            const closeBtn = event.target.closest('[data-close-dialog]');
            if (closeBtn) {
                const parentDialog = closeBtn.closest('dialog');
                if (parentDialog) toggleNativeDialog(parentDialog.id, false);
            }

            // 3. Handle Registration Days Filter Buttons
            const filterBtn = event.target.closest('[data-registration-days]');
            if (filterBtn) {
                renderFilteredRegistrations(Number(filterBtn.dataset.registrationDays));
            }

            // 4. Close On Dialog Backdrop Click
            if (event.target.tagName === 'DIALOG') {
                const rect = event.target.getBoundingClientRect();
                const clickedOutside = (
                    event.clientY < rect.top || event.clientY > rect.bottom ||
                    event.clientX < rect.left || event.clientX > rect.right
                );
                if (clickedOutside) toggleNativeDialog(event.target.id, false);
            }
        });
    }

    function init() {
        setupInteractionListeners();
        tickCountdowns();

        // Tick countdowns every 60 seconds
        setInterval(function () {
            tickCountdowns();
        }, 60000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

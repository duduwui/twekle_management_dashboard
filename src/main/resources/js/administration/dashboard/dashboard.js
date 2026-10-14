/**
 * ============================================================================
 * Dashboard Overview Interactions (dashboard.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/dashboard/dashboard.js
 * Used by:  /WEB-INF/views/administration/dashboard/dashboard.html
 * Route:    /admin/dashboard
 * 
 * Purpose:
 *   Handles client-side real-time UI interactions on the Dashboard screen:
 *   1. Live Countdown Timers: Dynamically ticks remaining minutes/hours
 *      and overdue indicators on the Priority Attention follow-up table.
 *   2. Dashboard Dialog Modals: Opens and closes quick-inspection modals
 *      (e.g., viewing urgent customer notes and feedback details).
 * ============================================================================
 */
(function () {
    'use strict';

    var pageLoadTime = Date.now();

    /**
     * Formats remaining or overdue minutes into human-readable countdown string.
     */
    function formatCountdown(mins) {
        if (mins >= 0) {
            if (mins < 60) return mins + 'm left';
            var hours = Math.floor(mins / 60);
            if (hours < 24) return hours + 'h ' + (mins % 60) + 'm left';
            var days = Math.floor(hours / 24);
            var remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h left') : (days + 'd left');
        } else {
            var overdue = Math.abs(mins);
            if (overdue < 60) return overdue + 'm overdue';
            var ovH = Math.floor(overdue / 60);
            if (ovH < 24) return ovH + 'h overdue';
            var ovD = Math.floor(ovH / 24);
            var ovRH = ovH % 24;
            return ovRH > 0 ? (ovD + 'd ' + ovRH + 'h overdue') : (ovD + 'd overdue');
        }
    }

    /**
     * Ticks live countdown tags on the priority attention table.
     */
    function updateLiveCountdowns() {
        var elapsedMins = Math.floor((Date.now() - pageLoadTime) / 60000);
        document.querySelectorAll('.live-countdown-tag[data-minutes-left]').forEach(function (el) {
            var initialMins = parseInt(el.getAttribute('data-minutes-left'), 10);
            if (isNaN(initialMins)) return;

            var currentMins = initialMins - elapsedMins;
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

    function filterRegistrations(days) {
        var cutoff = days ? Date.now() - days * 86400000 : 0;
        var visible = 0;
        document.querySelectorAll('[data-registered]').forEach(function (row) {
            row.hidden = !!cutoff && !(Date.parse(row.dataset.registered) > cutoff);
            if (!row.hidden) visible++;
        });
        var emptyEl = document.getElementById('registrationsEmpty');
        if (emptyEl) emptyEl.hidden = visible > 0;

        // Highlight active filter pill
        document.querySelectorAll('[data-registration-days]').forEach(function (btn) {
            if (Number(btn.dataset.registrationDays) === Number(days)) {
                btn.classList.add('btn-primary');
            } else {
                btn.classList.remove('btn-primary');
            }
        });
    }

    document.addEventListener('click', function (event) {
        var opener = event.target.closest('[data-dialog]');
        if (opener) {
            var dialogId = opener.dataset.dialog;
            var dialog = document.getElementById(dialogId);
            if (dialog) {
                if (opener.dataset.days !== undefined) {
                    filterRegistrations(Number(opener.dataset.days));
                }
                if (typeof dialog.showModal === 'function') {
                    dialog.showModal();
                } else {
                    dialog.setAttribute('open', '');
                }
            }
        }

        var close = event.target.closest('[data-close-dialog]');
        if (close) {
            var dialogToClose = close.closest('dialog');
            if (dialogToClose) {
                if (typeof dialogToClose.close === 'function') {
                    dialogToClose.close();
                } else {
                    dialogToClose.removeAttribute('open');
                }
            }
        }

        var filter = event.target.closest('[data-registration-days]');
        if (filter) {
            filterRegistrations(Number(filter.dataset.registrationDays));
        }

        // Close on backdrop click
        if (event.target.tagName === 'DIALOG') {
            var rect = event.target.getBoundingClientRect();
            var isInDialog = (rect.top <= event.clientY && event.clientY <= rect.top + rect.height &&
                              rect.left <= event.clientX && event.clientX <= rect.left + rect.width);
            if (!isInDialog && typeof event.target.close === 'function') {
                event.target.close();
            }
        }
    });

    // Start live countdown timer every 60 seconds
    setInterval(updateLiveCountdowns, 60000);
}());

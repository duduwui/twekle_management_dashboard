/**
 * ============================================================================
 * Dashboard Feature - Metrics & Calculation Service (dashboard-service.js)
 * ============================================================================
 * 
 * Layer:    Service Layer (Data, Algorithms, Calculations)
 * Location: src/main/resources/js/administration/dashboard/dashboard-service.js
 * 
 * Rules:
 *   - Pure math, calculations, and data formatting.
 *   - Strictly FORBIDDEN from querying or mutating the DOM.
 * ============================================================================
 */

const DashboardService = (function () {
    'use strict';

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

    return {
        formatCountdown: formatCountdown,
        isWithinCutoff: isWithinCutoff
    };
})();

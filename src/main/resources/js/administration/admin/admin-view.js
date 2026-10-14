/**
 * ============================================================================
 * Staff Administration - UI Painter & DOM Manipulator (admin-view.js)
 * ============================================================================
 * 
 * Layer:    M-V-E Layer 2 (View)
 * Location: src/main/resources/js/administration/admin/admin-view.js
 * 
 * Rules:
 *   - Performs DOM lookups (getElementById, querySelectorAll).
 *   - Manipulates CSS styles, classes, and visibility.
 *   - Delegates business and filter logic to AdminModule.
 * ============================================================================
 */

const AdminView = (function (Module) {
    'use strict';

    /**
     * Filters the administrators table rows based on input search text.
     * 
     * @param {string} query - Typed filter query.
     */
    function filterTable(query) {
        var rows = document.querySelectorAll('#adminTable tbody tr.admin-row');
        var visibleCount = 0;

        rows.forEach(function (row) {
            var text = row.innerText;
            var isVisible = Module.matchesQuery(text, query);
            row.style.display = isVisible ? '' : 'none';
            if (isVisible) visibleCount++;
        });

        // Update footer counter if present
        var infoEl = document.querySelector('.datatable-info strong');
        if (infoEl && !query) {
            infoEl.textContent = rows.length;
        } else if (infoEl) {
            infoEl.textContent = visibleCount;
        }
    }

    /**
     * Shows the Create Admin Form section and hides the Admin Table.
     */
    function showCreateSection() {
        var listSection = document.getElementById('adminListSection');
        var createSection = document.getElementById('createAdminSection');

        if (listSection) listSection.style.display = 'none';
        if (createSection) createSection.style.display = 'flex';

        var usernameInput = document.querySelector('#createAdminSection input[name="username"]');
        if (usernameInput) usernameInput.focus();
    }

    /**
     * Hides the Create Admin Form section and restores the Admin Table.
     */
    function hideCreateSection() {
        var listSection = document.getElementById('adminListSection');
        var createSection = document.getElementById('createAdminSection');

        if (createSection) createSection.style.display = 'none';
        if (listSection) listSection.style.display = 'flex';
    }

    /**
     * Toggles the "SUPER ADMIN" badge indicator.
     * 
     * @param {boolean} isSuper - Whether the Super Admin switch is enabled.
     */
    function toggleSuperAdminBadge(isSuper) {
        var badge = document.getElementById('superAdminStatusBadge');
        if (badge) {
            badge.style.display = isSuper ? 'inline-block' : 'none';
        }
    }

    return {
        filterTable: filterTable,
        showCreateSection: showCreateSection,
        hideCreateSection: hideCreateSection,
        toggleSuperAdminBadge: toggleSuperAdminBadge
    };
})(AdminModule);

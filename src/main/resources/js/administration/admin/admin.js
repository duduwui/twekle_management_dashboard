/**
 * ============================================================================
 * Staff Administration Feature (admin.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/admin/admin.js
 * Used by:  /WEB-INF/views/administration/admin/admins.html
 * Route:    /admin/admins
 * 
 * Responsibilities:
 *   - Client-side filtering of administrator table rows.
 *   - Showing and hiding the "Create Admin" form panel.
 *   - Toggling the Super Administrator badge state.
 *   - Wireframing event listeners and maintaining global function compatibility.
 * ============================================================================
 */

(function () {
    'use strict';

    /**
     * Filters the administrators table rows based on input search text.
     * 
     * @param {string} query - Filter query typed by the user.
     */
    function filterTable(query) {
        var rows = document.querySelectorAll('#adminTable tbody tr.admin-row');
        var visibleCount = 0;
        var normalizedQuery = query ? query.toLowerCase().trim() : '';

        rows.forEach(function (row) {
            var text = row.innerText.toLowerCase();
            var isVisible = !normalizedQuery || text.indexOf(normalizedQuery) > -1;
            row.style.display = isVisible ? '' : 'none';
            if (isVisible) visibleCount++;
        });

        // Update footer counter if present
        var infoEl = document.querySelector('.datatable-info strong');
        if (infoEl) {
            infoEl.textContent = normalizedQuery ? visibleCount : rows.length;
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

    function setupEventListeners() {
        // 1. Live Search Input
        var searchInput = document.getElementById('adminSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function (e) {
                filterTable(e.target.value);
            });
        }

        // 2. Open Create Admin Form Buttons
        var openButtons = document.querySelectorAll('#btnShowCreateAdmin, [data-action="show-create-admin"]');
        openButtons.forEach(function (btn) {
            btn.addEventListener('click', showCreateSection);
        });

        // 3. Close / Back Create Admin Form Buttons
        var closeButtons = document.querySelectorAll('#btnBackToAdmins, #btnCancelCreateAdmin, [data-action="hide-create-admin"]');
        closeButtons.forEach(function (btn) {
            btn.addEventListener('click', hideCreateSection);
        });

        // 4. Super Admin Checkbox Toggle
        var superAdminCheck = document.getElementById('createIsSuperAdmin');
        if (superAdminCheck) {
            superAdminCheck.addEventListener('change', function (e) {
                toggleSuperAdminBadge(e.target.checked);
            });
        }

        // 5. Global compatibility hooks (for inline handlers on admins.html)
        window.filterAdminTable = function () {
            var input = document.getElementById('adminSearchInput');
            if (input) filterTable(input.value);
        };
        window.showCreateAdminSection = showCreateSection;
        window.hideCreateAdminSection = hideCreateSection;
        window.toggleCreateSuperAdmin = toggleSuperAdminBadge;
    }

    function init() {
        setupEventListeners();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

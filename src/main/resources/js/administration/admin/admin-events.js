/**
 * ============================================================================
 * Staff Administration - Event Wireframe & Lifecycle Hooks (admin-events.js)
 * ============================================================================
 * 
 * Layer:    M-V-E Layer 3 (Events)
 * Location: src/main/resources/js/administration/admin/admin-events.js
 * 
 * Rules:
 *   - Attaches event listeners (input, click, change, submit).
 *   - Handles page initialization on DOMContentLoaded.
 *   - Strictly delegates visual actions to AdminView.
 * ============================================================================
 */

(function (View) {
    'use strict';

    function setupEventListeners() {
        // 1. Live Search Input
        var searchInput = document.getElementById('adminSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function (e) {
                View.filterTable(e.target.value);
            });
        }

        // 2. Open Create Admin Form Buttons
        var openButtons = document.querySelectorAll('#btnShowCreateAdmin, [data-action="show-create-admin"]');
        openButtons.forEach(function (btn) {
            btn.addEventListener('click', View.showCreateSection);
        });

        // 3. Close / Back Create Admin Form Buttons
        var closeButtons = document.querySelectorAll('#btnBackToAdmins, #btnCancelCreateAdmin, [data-action="hide-create-admin"]');
        closeButtons.forEach(function (btn) {
            btn.addEventListener('click', View.hideCreateSection);
        });

        // 4. Super Admin Checkbox Toggle
        var superAdminCheck = document.getElementById('createIsSuperAdmin');
        if (superAdminCheck) {
            superAdminCheck.addEventListener('change', function (e) {
                View.toggleSuperAdminBadge(e.target.checked);
            });
        }

        // 5. Global compatibility hooks (for any inline calls if present)
        window.filterAdminTable = function () {
            var input = document.getElementById('adminSearchInput');
            if (input) View.filterTable(input.value);
        };
        window.showCreateAdminSection = View.showCreateSection;
        window.hideCreateAdminSection = View.hideCreateSection;
        window.toggleCreateSuperAdmin = View.toggleSuperAdminBadge;
    }

    function init() {
        setupEventListeners();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(AdminView);

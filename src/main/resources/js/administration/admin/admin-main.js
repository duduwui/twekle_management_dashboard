/**
 * ============================================================================
 * Staff Administration - Main Entry & Event Wireframe (admin-main.js)
 * ============================================================================
 * 
 * Layer:    Main Layer (Event Listeners, Page Init, Lifecycle)
 * Location: src/main/resources/js/administration/admin/admin-main.js
 * 
 * Rules:
 *   - Attaches event listeners (input, click, change).
 *   - Initializes page on DOMContentLoaded.
 *   - Strictly delegates visual updates to AdminRenderer.
 * ============================================================================
 */

(function (Renderer) {
    'use strict';

    function setupEventListeners() {
        // 1. Live Search Input
        var searchInput = document.getElementById('adminSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function (e) {
                Renderer.filterTable(e.target.value);
            });
        }

        // 2. Open Create Admin Form Buttons
        var openButtons = document.querySelectorAll('#btnShowCreateAdmin, [data-action="show-create-admin"]');
        openButtons.forEach(function (btn) {
            btn.addEventListener('click', Renderer.showCreateSection);
        });

        // 3. Close / Back Create Admin Form Buttons
        var closeButtons = document.querySelectorAll('#btnBackToAdmins, #btnCancelCreateAdmin, [data-action="hide-create-admin"]');
        closeButtons.forEach(function (btn) {
            btn.addEventListener('click', Renderer.hideCreateSection);
        });

        // 4. Super Admin Checkbox Toggle
        var superAdminCheck = document.getElementById('createIsSuperAdmin');
        if (superAdminCheck) {
            superAdminCheck.addEventListener('change', function (e) {
                Renderer.toggleSuperAdminBadge(e.target.checked);
            });
        }

        // 5. Global compatibility hooks (for any inline calls if present)
        window.filterAdminTable = function () {
            var input = document.getElementById('adminSearchInput');
            if (input) Renderer.filterTable(input.value);
        };
        window.showCreateAdminSection = Renderer.showCreateSection;
        window.hideCreateAdminSection = Renderer.hideCreateSection;
        window.toggleCreateSuperAdmin = Renderer.toggleSuperAdminBadge;
    }

    function init() {
        setupEventListeners();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(AdminRenderer);

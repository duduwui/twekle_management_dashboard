/**
 * ============================================================================
 * Staff Administration (admin.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/admin/admin.js
 * Used by:  /WEB-INF/views/administration/admin/admins.html
 * Route:    /admin/admins
 * 
 * Purpose:
 *   Handles client-side UI interactions for the Staff Administrators screen:
 *   1. Live search filtering of the administrators table.
 *   2. View switching between the Admin Directory Table and Create Admin Form.
 *   3. Dynamic Super Admin badge toggling upon role permission changes.
 * 
 * Note:
 *   All actual data mutations (Create, Delete, Toggle Status) are handled
 *   via native Server-Side Rendered (SSR) form submissions in AdminController.
 * ============================================================================
 */

(function () {
    'use strict';

    /**
     * Filters the Server-Side Rendered administrator table in real-time.
     * Matches the typed query against usernames, phone numbers, and roles.
     */
    window.filterAdminTable = function () {
        var input = document.getElementById("adminSearchInput");
        if (!input) return;

        var filter = input.value.toLowerCase().trim();
        var rows = document.querySelectorAll("#adminTable tbody tr.admin-row");

        rows.forEach(function (row) {
            var text = row.innerText.toLowerCase();
            row.style.display = text.indexOf(filter) > -1 ? "" : "none";
        });
    };

    /**
     * Switches the view from the Administrator Table to the "Create Admin" Form.
     */
    window.showCreateAdminSection = function () {
        var list = document.getElementById('adminListSection');
        var create = document.getElementById('createAdminSection');

        if (list) list.style.display = 'none';
        if (create) create.style.display = 'flex';

        // Automatically focus the username input field
        var usernameInput = document.querySelector('#createAdminSection input[name="username"]');
        if (usernameInput) {
            usernameInput.focus();
        }
    };

    /**
     * Cancels creation and returns back to the Administrator Table.
     */
    window.hideCreateAdminSection = function () {
        var list = document.getElementById('adminListSection');
        var create = document.getElementById('createAdminSection');

        if (create) create.style.display = 'none';
        if (list) list.style.display = 'flex';
    };

    /**
     * Toggles visibility of the "SUPER ADMIN" badge based on checkbox state.
     * 
     * @param {boolean} isSuper - Whether the Super Admin switch is checked.
     */
    window.toggleCreateSuperAdmin = function (isSuper) {
        var badge = document.getElementById('superAdminStatusBadge');
        if (badge) {
            badge.style.display = isSuper ? 'inline-block' : 'none';
        }
    };

})();

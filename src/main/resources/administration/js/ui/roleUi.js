/**
 * Role & Permissions UI DOM Helpers & Matrix Handlers
 * Handles role module switch syncing, presets, and admin selection for permission matrix.
 */
var modules = ['ADMIN_MANAGEMENT', 'CUSTOMER_FOLLOWUP', 'REPORTS_INSIGHTS'];
var currentAdminId = null;

function syncModuleSwitch(mod, action) {
    var sw = document.getElementById('switch_' + mod + '_' + action);
    var badge = document.getElementById('badge_' + mod + '_' + action);
    if (sw && badge) {
        if (sw.checked) {
            badge.classList.add('active');
        } else {
            badge.classList.remove('active');
        }
    }
}

function applyModulePreset(mod, type) {
    var swC = document.getElementById('switch_' + mod + '_create');
    var swR = document.getElementById('switch_' + mod + '_read');
    var swU = document.getElementById('switch_' + mod + '_update');
    var swD = document.getElementById('switch_' + mod + '_delete');

    if (type === 'full') {
        if (swC) swC.checked = true;
        if (swR) swR.checked = true;
        if (swU) swU.checked = true;
        if (swD) swD.checked = true;
    } else if (type === 'readonly') {
        if (swC) swC.checked = false;
        if (swR) swR.checked = true;
        if (swU) swU.checked = false;
        if (swD) swD.checked = false;
    } else if (type === 'revoke') {
        if (swC) swC.checked = false;
        if (swR) swR.checked = false;
        if (swU) swU.checked = false;
        if (swD) swD.checked = false;
    }

    ['create', 'read', 'update', 'delete'].forEach(function(act) {
        syncModuleSwitch(mod, act);
    });
}

function applyGlobalPreset(type) {
    modules.forEach(function(m) {
        applyModulePreset(m, type);
    });
}

function onRoleTemplateChanged(selectEl) {
    var opt = selectEl.options[selectEl.selectedIndex];
    if (!opt || !opt.value) return;

    var canC = opt.getAttribute('data-can-create') === 'true';
    var canR = opt.getAttribute('data-can-read') === 'true';
    var canU = opt.getAttribute('data-can-update') === 'true';
    var canD = opt.getAttribute('data-can-delete') === 'true';

    modules.forEach(function(m) {
        var swC = document.getElementById('switch_' + m + '_create');
        var swR = document.getElementById('switch_' + m + '_read');
        var swU = document.getElementById('switch_' + m + '_update');
        var swD = document.getElementById('switch_' + m + '_delete');

        if (swC) swC.checked = canC;
        if (swR) swR.checked = canR;
        if (swU) swU.checked = canU;
        if (swD) swD.checked = canD;

        ['create', 'read', 'update', 'delete'].forEach(function(act) {
            syncModuleSwitch(m, act);
        });
    });
}

function closeRoleAssignmentSection() {
    var sec = document.getElementById('roleAssignmentSection');
    if (sec) sec.style.display = 'none';
    document.querySelectorAll('.admin-table-row').forEach(function(tr) {
        tr.style.backgroundColor = '';
    });
    currentAdminId = null;
}

function selectAdminFromRow(rowEl) {
    if (!rowEl) return;
    var adminId = rowEl.getAttribute('data-admin-id');
    var username = rowEl.getAttribute('data-admin-username');
    var roleBadge = rowEl.querySelector('.admin-role-badge');
    var roleId = roleBadge ? roleBadge.getAttribute('data-role-id') : null;
    var isSuper = rowEl.getAttribute('data-super-admin') === 'true';
    selectAdmin(adminId, username, roleId ? parseInt(roleId, 10) : null, isSuper);
}

function selectAdmin(adminId, username, roleId, isSuper) {
    currentAdminId = adminId;
    var formAdmin = document.getElementById('formAdminId');
    var formRole = document.getElementById('formRoleId');
    var display = document.getElementById('selectedAdminDisplay');
    var form = document.getElementById('roleAssignForm');

    if (formAdmin) formAdmin.value = adminId;
    if (formRole) formRole.value = roleId || '';
    if (display) display.textContent = username;

    var accessBadge = document.getElementById('selectedAdminAccessBadge');
    if (accessBadge) {
        accessBadge.textContent = isSuper ? 'SUPER ADMIN' : 'STAFF';
        accessBadge.className = 'comic-tag ' + (isSuper ? 'tag-primary' : 'tag-muted');
    }
    if (form) form.action = '/admin/roles/' + adminId + '/assign_user_role';

    // Show permissions section and scroll smoothly to it
    var sec = document.getElementById('roleAssignmentSection');
    if (sec) {
        sec.style.display = 'block';
        sec.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    // Highlight selected row in table
    document.querySelectorAll('.admin-table-row').forEach(function(tr) {
        tr.style.backgroundColor = '';
    });
    var activeRow = document.getElementById('admin-row-' + adminId);
    if (activeRow) {
        activeRow.style.backgroundColor = '#FEF3C7';
    }

    // Read specific admin permissions if present
    var permsContainer = document.getElementById('perms-data-' + adminId);
    if (permsContainer) {
        var permSpans = permsContainer.querySelectorAll('span');
        if (permSpans && permSpans.length > 0) {
            permSpans.forEach(function(s) {
                var mod = s.getAttribute('data-module');
                var canC = s.getAttribute('data-create') === 'true';
                var canR = s.getAttribute('data-read') === 'true';
                var canU = s.getAttribute('data-update') === 'true';
                var canD = s.getAttribute('data-delete') === 'true';

                var swC = document.getElementById('switch_' + mod + '_create');
                var swR = document.getElementById('switch_' + mod + '_read');
                var swU = document.getElementById('switch_' + mod + '_update');
                var swD = document.getElementById('switch_' + mod + '_delete');

                if (swC) swC.checked = canC;
                if (swR) swR.checked = canR;
                if (swU) swU.checked = canU;
                if (swD) swD.checked = canD;

                ['create', 'read', 'update', 'delete'].forEach(function(act) {
                    syncModuleSwitch(mod, act);
                });
            });
        }
    }
}

// Auto-select admin on load ONLY if redirected with selectedAdminId
document.addEventListener('DOMContentLoaded', function() {
    var params = new URLSearchParams(window.location.search);
    var selectedId = params.get('selectedAdminId');
    if (selectedId) {
        var row = document.getElementById('admin-row-' + selectedId);
        if (row) {
            selectAdminFromRow(row);
        }
    }
});

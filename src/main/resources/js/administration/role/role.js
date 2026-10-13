/**
 * Role Feature - Service, Controller & Interactive UI
 * Consolidated service, AngularJS controller, and UI DOM handlers for role templates and permissions assignment.
 */
angular.module('tweklApp')
.factory('RoleService', ['$http', function($http) {
    var BASE = '/admin/roles';
    function request(method, path, data) {
        return $http({ method: method, url: BASE + (path || ''), data: data });
    }
    return {
        getAll: function() { return request('GET'); },
        getById: function(id) { return request('GET', '/' + id); },
        create: function(data) { return request('POST', '/create', data); },
        update: function(id, data) { return request('POST', '/' + id + '/update', data); },
        delete: function(id) { return request('POST', '/' + id + '/delete'); }
    };
}])
.controller('RoleController', ['$scope', '$window', 'RoleService', 'ToastService',
function($scope, $window, RoleService, ToastService) {
    $scope.roles = $window.SERVER_ROLE_LIST || [];
    $scope.selectedRole = null;
    $scope.modules = [
        { key: 'ADMIN_MANAGEMENT', name: 'Administration & Staff', description: 'Manage administrator accounts, security credentials, role assignments, and system privilege matrix.' },
        { key: 'CUSTOMER_FOLLOWUP', name: 'Follow-ups & Checklists', description: 'Follow-up directory, sales order timelines, checkpoint checklists, note documentation & photo attachments.' },
        { key: 'REPORTS_INSIGHTS', name: 'Reports & Analytics', description: 'Operational milestone turnaround metrics, customer satisfaction feedback, product reviews, and period export.' }
    ];

    $scope.activePerms = {
        ADMIN_MANAGEMENT: { canCreate: false, canRead: true, canUpdate: false, canDelete: false },
        CUSTOMER_FOLLOWUP: { canCreate: false, canRead: true, canUpdate: false, canDelete: false },
        REPORTS_INSIGHTS: { canCreate: false, canRead: true, canUpdate: false, canDelete: false }
    };

    $scope.selectRole = function(role) {
        $scope.selectedRole = role;
        $scope.modules.forEach(function(m) {
            $scope.activePerms[m.key] = {
                canCreate: !!role.canCreate,
                canRead: !!role.canRead,
                canUpdate: !!role.canUpdate,
                canDelete: !!role.canDelete
            };
        });
    };

    $scope.applyCardPreset = function(modKey, preset) {
        var p = $scope.activePerms[modKey];
        if (!p) return;
        if (preset === 'full') {
            p.canCreate = true; p.canRead = true; p.canUpdate = true; p.canDelete = true;
        } else if (preset === 'readonly') {
            p.canCreate = false; p.canRead = true; p.canUpdate = false; p.canDelete = false;
        } else if (preset === 'revoke') {
            p.canCreate = false; p.canRead = false; p.canUpdate = false; p.canDelete = false;
        }
    };

    $scope.applyAllPreset = function(preset) {
        $scope.modules.forEach(function(m) {
            $scope.applyCardPreset(m.key, preset);
        });
    };
}]);

/* =========================================================
   ROLE & PERMISSIONS UI DOM HELPERS
   ========================================================= */
var roleModules = ['ADMIN_MANAGEMENT', 'CUSTOMER_FOLLOWUP', 'REPORTS_INSIGHTS'];
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
    roleModules.forEach(function(m) {
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

    roleModules.forEach(function(m) {
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

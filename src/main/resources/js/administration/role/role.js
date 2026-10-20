/**
 * ============================================================================
 * Roles & Action-Level Permission Matrix Management (role.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/role/role.js
 * Used by:  /WEB-INF/views/administration/role/roles.html
 * Route:    /admin/roles
 * 
 * Purpose:
 *   Handles granular action-level permission toggles and role assignments:
 *   1. Feature Action Matrix: Toggles individual capabilities per module:
 *      - CUSTOMER_FOLLOWUP: view list, open log, check toggle, notes/feedback, upload photos, export
 *      - REPORTS_INSIGHTS: milestones, satisfaction, products, retention, export
 *      - ADMIN_MANAGEMENT: view staff, manage staff, manage permissions, view presets, manage presets
 *   2. Module Presets: "All Actions", "View Only", "Revoke All"
 *   3. Synchronizes action keys and backward-compatible CRUD flags on form submit
 * ============================================================================
 */

var roleModules = ['CUSTOMER_FOLLOWUP', 'REPORTS_INSIGHTS', 'ADMIN_MANAGEMENT'];
var currentAdminId = null;

function onActionCheckboxChanged(moduleKey) {
    var cbs = document.querySelectorAll('.action-checkbox[data-module="' + moduleKey + '"]');
    var selectedActions = [];

    cbs.forEach(function(cb) {
        if (cb.checked) {
            var act = cb.getAttribute('data-action');
            if (act) selectedActions.push(act);
        }
    });

    var actionsInput = document.getElementById('actions_' + moduleKey);
    if (actionsInput) {
        actionsInput.value = selectedActions.join(',');
    }

    // Synchronize backward-compatible CRUD flags
    var canReadEl = document.getElementById('canRead_' + moduleKey);
    var canCreateEl = document.getElementById('canCreate_' + moduleKey);
    var canUpdateEl = document.getElementById('canUpdate_' + moduleKey);
    var canDeleteEl = document.getElementById('canDelete_' + moduleKey);

    var hasAny = selectedActions.length > 0;
    if (canReadEl) canReadEl.value = hasAny ? 'true' : 'false';

    if (moduleKey === 'CUSTOMER_FOLLOWUP') {
        var canUp = selectedActions.includes('CUSTOMER_CHECK_TOGGLE') || selectedActions.includes('CUSTOMER_ADD_FEEDBACK');
        var canCr = selectedActions.includes('CUSTOMER_UPLOAD_PHOTOS') || selectedActions.includes('CUSTOMER_EXPORT_EXCEL');
        if (canUpdateEl) canUpdateEl.value = canUp ? 'true' : 'false';
        if (canCreateEl) canCreateEl.value = canCr ? 'true' : 'false';
        if (canDeleteEl) canDeleteEl.value = 'false';
    } else if (moduleKey === 'REPORTS_INSIGHTS') {
        var canCr = selectedActions.includes('REPORT_EXPORT_EXCEL');
        if (canUpdateEl) canUpdateEl.value = 'false';
        if (canCreateEl) canCreateEl.value = canCr ? 'true' : 'false';
        if (canDeleteEl) canDeleteEl.value = 'false';
    } else if (moduleKey === 'ADMIN_MANAGEMENT') {
        var canCr = selectedActions.includes('STAFF_MANAGE') || selectedActions.includes('PRESETS_MANAGE');
        var canUp = selectedActions.includes('STAFF_PERMISSIONS');
        var canDel = selectedActions.includes('STAFF_MANAGE') || selectedActions.includes('PRESETS_MANAGE');
        if (canCreateEl) canCreateEl.value = canCr ? 'true' : 'false';
        if (canUpdateEl) canUpdateEl.value = canUp ? 'true' : 'false';
        if (canDeleteEl) canDeleteEl.value = canDel ? 'true' : 'false';
    }
}

function applyModuleActionPreset(moduleKey, type) {
    var cbs = document.querySelectorAll('.action-checkbox[data-module="' + moduleKey + '"]');

    if (type === 'all') {
        cbs.forEach(function(cb) { cb.checked = true; });
    } else if (type === 'revoke') {
        cbs.forEach(function(cb) { cb.checked = false; });
    } else if (type === 'readonly') {
        cbs.forEach(function(cb) {
            var act = cb.getAttribute('data-action');
            if (moduleKey === 'CUSTOMER_FOLLOWUP') {
                cb.checked = (act === 'CUSTOMER_VIEW_LIST' || act === 'CUSTOMER_VIEW_LOG');
            } else if (moduleKey === 'REPORTS_INSIGHTS') {
                cb.checked = (act === 'REPORT_VIEW_MILESTONES' || act === 'REPORT_VIEW_SATISFACTION' || act === 'REPORT_VIEW_PRODUCTS' || act === 'REPORT_VIEW_RETENTION');
            } else if (moduleKey === 'ADMIN_MANAGEMENT') {
                cb.checked = (act === 'STAFF_VIEW' || act === 'PRESETS_VIEW');
            }
        });
    }

    onActionCheckboxChanged(moduleKey);
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
    var superBanner = document.getElementById('superAdminNoticeBanner');

    if (formAdmin) formAdmin.value = adminId;
    if (formRole) formRole.value = roleId || '';
    if (display) display.textContent = username;

    var accessBadge = document.getElementById('selectedAdminAccessBadge');
    if (accessBadge) {
        accessBadge.textContent = isSuper ? 'SUPER ADMIN (UNRESTRICTED)' : 'STAFF';
        accessBadge.className = 'comic-tag ' + (isSuper ? 'tag-primary' : 'tag-muted');
    }
    if (superBanner) {
        superBanner.style.display = isSuper ? 'block' : 'none';
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

    // Reset all checkboxes first
    document.querySelectorAll('.action-checkbox').forEach(function(cb) {
        cb.checked = false;
    });

    if (isSuper) {
        // Super admin has all actions enabled
        document.querySelectorAll('.action-checkbox').forEach(function(cb) {
            cb.checked = true;
        });
        roleModules.forEach(function(m) { onActionCheckboxChanged(m); });
        return;
    }

    // Read stored permissions for selected admin
    var permsContainer = document.getElementById('perms-data-' + adminId);
    if (permsContainer) {
        var permSpans = permsContainer.querySelectorAll('span');
        if (permSpans && permSpans.length > 0) {
            permSpans.forEach(function(s) {
                var mod = s.getAttribute('data-module');
                var actionsStr = s.getAttribute('data-actions');
                var canC = s.getAttribute('data-create') === 'true';
                var canR = s.getAttribute('data-read') === 'true';
                var canU = s.getAttribute('data-update') === 'true';
                var canD = s.getAttribute('data-delete') === 'true';

                if (actionsStr && actionsStr.trim().length > 0) {
                    var acts = actionsStr.split(',');
                    acts.forEach(function(act) {
                        var trimmed = act.trim().toUpperCase();
                        var cb = document.querySelector('.action-checkbox[data-module="' + mod + '"][data-action="' + trimmed + '"]');
                        if (cb) cb.checked = true;
                    });
                } else {
                    // Backward-compatible fallback for legacy C/R/U/D admins
                    if (mod === 'CUSTOMER_FOLLOWUP') {
                        if (canR) {
                            checkAction(mod, 'CUSTOMER_VIEW_LIST');
                            checkAction(mod, 'CUSTOMER_VIEW_LOG');
                        }
                        if (canU) {
                            checkAction(mod, 'CUSTOMER_CHECK_TOGGLE');
                            checkAction(mod, 'CUSTOMER_ADD_FEEDBACK');
                        }
                        if (canC) {
                            checkAction(mod, 'CUSTOMER_UPLOAD_PHOTOS');
                            checkAction(mod, 'CUSTOMER_EXPORT_EXCEL');
                        }
                    } else if (mod === 'REPORTS_INSIGHTS') {
                        if (canR) {
                            checkAction(mod, 'REPORT_VIEW_MILESTONES');
                            checkAction(mod, 'REPORT_VIEW_SATISFACTION');
                            checkAction(mod, 'REPORT_VIEW_PRODUCTS');
                            checkAction(mod, 'REPORT_VIEW_RETENTION');
                        }
                        if (canC) {
                            checkAction(mod, 'REPORT_EXPORT_EXCEL');
                        }
                    } else if (mod === 'ADMIN_MANAGEMENT') {
                        if (canR) {
                            checkAction(mod, 'STAFF_VIEW');
                            checkAction(mod, 'PRESETS_VIEW');
                        }
                        if (canU) {
                            checkAction(mod, 'STAFF_PERMISSIONS');
                        }
                        if (canC || canD) {
                            checkAction(mod, 'STAFF_MANAGE');
                            checkAction(mod, 'PRESETS_MANAGE');
                        }
                    }
                }

                onActionCheckboxChanged(mod);
            });
        }
    }
}

function checkAction(moduleKey, actionKey) {
    var cb = document.querySelector('.action-checkbox[data-module="' + moduleKey + '"][data-action="' + actionKey + '"]');
    if (cb) cb.checked = true;
}

// Auto-select admin on load if redirected with selectedAdminId
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

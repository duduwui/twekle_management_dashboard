/** Local interactions for the server-rendered admin page. */
function filterAdminTable() {
    var input = document.getElementById("adminSearchInput");
    if (!input) return;
    var filter = input.value.toLowerCase();
    var rows = document.querySelectorAll("#adminTable tbody tr.admin-row");
    rows.forEach(function(row) {
        var text = row.innerText.toLowerCase();
        row.style.display = text.indexOf(filter) > -1 ? "" : "none";
    });
}

function showCreateAdminSection() {
    var list = document.getElementById('adminListSection');
    var create = document.getElementById('createAdminSection');
    if (list) list.style.display = 'none';
    if (create) create.style.display = 'flex';
}

function hideCreateAdminSection() {
    var list = document.getElementById('adminListSection');
    var create = document.getElementById('createAdminSection');
    if (create) create.style.display = 'none';
    if (list) list.style.display = 'flex';
}

function syncAdminCreateBadge(modIdx, act) {
    var sw = document.getElementById('c_sw_' + modIdx + '_' + act);
    var badge = document.getElementById('c_badge_' + modIdx + '_' + act);
    if (sw && badge) {
        if (sw.checked) badge.classList.add('active');
        else badge.classList.remove('active');
    }
}

function applyAdminCreateCardPreset(modIdx, type) {
    var swC = document.getElementById('c_sw_' + modIdx + '_create');
    var swR = document.getElementById('c_sw_' + modIdx + '_read');
    var swU = document.getElementById('c_sw_' + modIdx + '_update');
    var swD = document.getElementById('c_sw_' + modIdx + '_delete');

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

    ['create', 'read', 'update', 'delete'].forEach(function(a) {
        syncAdminCreateBadge(modIdx, a);
    });
}

function applyAdminCreateGlobalPreset(type) {
    [0, 1, 2].forEach(function(idx) {
        applyAdminCreateCardPreset(idx, type);
    });
}

function applyRoleToAdminCreate(pill) {
    var canC = pill.getAttribute('data-can-create') === 'true';
    var canR = pill.getAttribute('data-can-read') === 'true';
    var canU = pill.getAttribute('data-can-update') === 'true';
    var canD = pill.getAttribute('data-can-delete') === 'true';

    [0, 1, 2].forEach(function(idx) {
        var swC = document.getElementById('c_sw_' + idx + '_create');
        var swR = document.getElementById('c_sw_' + idx + '_read');
        var swU = document.getElementById('c_sw_' + idx + '_update');
        var swD = document.getElementById('c_sw_' + idx + '_delete');
        if (swC) swC.checked = canC;
        if (swR) swR.checked = canR;
        if (swU) swU.checked = canU;
        if (swD) swD.checked = canD;
        ['create', 'read', 'update', 'delete'].forEach(function(a) {
            syncAdminCreateBadge(idx, a);
        });
    });
}

function toggleCreateSuperAdmin(isSuper) {
    var badge = document.getElementById('superAdminStatusBadge');
    if (isSuper) {
        if (badge) badge.style.display = 'inline-block';
        applyAdminCreateGlobalPreset('full');
    } else {
        if (badge) badge.style.display = 'none';
    }
}

function openInspectAdminModal(btn) {
    var adminId = btn.getAttribute('data-admin-id');
    var username = btn.getAttribute('data-username');
    var phone = btn.getAttribute('data-phone');
    var isSuper = btn.getAttribute('data-super-admin') === 'true';

    var title = document.getElementById('inspectModalTitle');
    var subtitle = document.getElementById('inspectModalSubtitle');
    var superSw = document.getElementById('inspectSuperAdmin');
    var form = document.getElementById('inspectAdminForm');

    if (title) title.textContent = 'Inspect Administrator Permissions: ' + username;
    if (subtitle) subtitle.textContent = 'Phone: ' + (phone || 'None') + ' • Status: Configured';
    if (superSw) superSw.checked = isSuper;
    if (form) form.action = '/admin/admins/' + adminId + '/update-permissions';

    [0, 1, 2].forEach(function(idx) {
        var swC = document.getElementById('insp_sw_' + idx + '_create');
        var swR = document.getElementById('insp_sw_' + idx + '_read');
        var swU = document.getElementById('insp_sw_' + idx + '_update');
        var swD = document.getElementById('insp_sw_' + idx + '_delete');
        if (swC) swC.checked = isSuper;
        if (swR) swR.checked = true;
        if (swU) swU.checked = isSuper;
        if (swD) swD.checked = isSuper;
        ['create', 'read', 'update', 'delete'].forEach(function(a) {
            syncInspectBadge(idx, a);
        });
    });

    var modal = document.getElementById('inspectAdminModal');
    if (modal) modal.style.display = 'flex';
}

function closeInspectAdminModal() {
    var modal = document.getElementById('inspectAdminModal');
    if (modal) modal.style.display = 'none';
}

function syncInspectBadge(modIdx, act) {
    var sw = document.getElementById('insp_sw_' + modIdx + '_' + act);
    var badge = document.getElementById('insp_b_' + modIdx + '_' + act);
    if (sw && badge) {
        if (sw.checked) badge.classList.add('active');
        else badge.classList.remove('active');
    }
}

function applyRoleToAdminInspect(pill) {
    var canC = pill.getAttribute('data-can-create') === 'true';
    var canR = pill.getAttribute('data-can-read') === 'true';
    var canU = pill.getAttribute('data-can-update') === 'true';
    var canD = pill.getAttribute('data-can-delete') === 'true';

    [0, 1, 2].forEach(function(idx) {
        var swC = document.getElementById('insp_sw_' + idx + '_create');
        var swR = document.getElementById('insp_sw_' + idx + '_read');
        var swU = document.getElementById('insp_sw_' + idx + '_update');
        var swD = document.getElementById('insp_sw_' + idx + '_delete');
        if (swC) swC.checked = canC;
        if (swR) swR.checked = canR;
        if (swU) swU.checked = canU;
        if (swD) swD.checked = canD;
        ['create', 'read', 'update', 'delete'].forEach(function(a) {
            syncInspectBadge(idx, a);
        });
    });
}

function toggleInspectSuperAdmin(isSuper) {
    if (isSuper) {
        [0, 1, 2].forEach(function(idx) {
            var swC = document.getElementById('insp_sw_' + idx + '_create');
            var swR = document.getElementById('insp_sw_' + idx + '_read');
            var swU = document.getElementById('insp_sw_' + idx + '_update');
            var swD = document.getElementById('insp_sw_' + idx + '_delete');
            if (swC) swC.checked = true;
            if (swR) swR.checked = true;
            if (swU) swU.checked = true;
            if (swD) swD.checked = true;
            ['create', 'read', 'update', 'delete'].forEach(function(a) {
                syncInspectBadge(idx, a);
            });
        });
    }
}

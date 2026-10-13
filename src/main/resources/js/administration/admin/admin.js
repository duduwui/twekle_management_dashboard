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

function toggleCreateSuperAdmin(isSuper) {
    var badge = document.getElementById('superAdminStatusBadge');
    if (badge) badge.style.display = isSuper ? 'inline-block' : 'none';
}

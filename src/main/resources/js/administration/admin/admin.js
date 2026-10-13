/**
 * Admin Feature - Service, Controllers & Interactive UI
 * Consolidated service, AngularJS controllers, and UI DOM helpers for Administrator Directory, Creation, and Inspection.
 */
angular.module('tweklApp')
.factory('AdminService', ['$http', function($http) {
    var BASE = '/admin/admins';

    function request(method, path, data) {
        return $http({ method: method, url: BASE + (path || ''), data: data });
    }

    return {
        getAll: function() { return request('GET'); },
        getById: function(id) { return request('GET', '/' + id); },
        create: function(data) { return request('POST', '', data); },
        update: function(id, data) { return request('PUT', '/' + id, data); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}])
.controller('AdminController', [
    '$scope', '$window', '$timeout', 'AdminService', 'ToastService', 'FilterHelperService',
function($scope, $window, $timeout, AdminService, ToastService, FilterHelperService) {

    // View states: 'list' | 'create' | 'inspect' | 'delete'
    $scope.adminView = 'list';
    $scope.admins = [];
    $scope.selectedAdmin = null;
    $scope.adminToDelete = null;
    $scope.showDeleteAdminModal = false;

    // Table pagination & sorting state
    $scope.dt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'username',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria - Default Date From: Today, Date To: Tomorrow / Next Day
    $scope.adminFilters = {
        search: '',
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getTomorrow()
    };

    // Create form model
    $scope.newAdmin = {
        username: '',
        phoneNumber: '',
        password: '',
        superAdmin: false,
        canCreateRoles: true,
        canManageUsers: true,
        status: 'ACTIVE'
    };

    /**
     * Switch admin view mode and update URL
     */
    $scope.setAdminView = function(view, admin) {
        $scope.adminView = view;
        if (admin) $scope.selectedAdmin = admin;
        if ($window.history && $window.history.pushState) {
            if (view === 'list')    $window.history.pushState(null, '', '/admin/admins');
            else if (view === 'create')  $window.history.pushState(null, '', '/admin/admins/create');
            else if (view === 'inspect' && $scope.selectedAdmin) $window.history.pushState(null, '', '/admin/admins/' + $scope.selectedAdmin.id);
        }
    };

    /**
     * Loads all administrators from backend
     */
    $scope.loadAdmins = function() {
        AdminService.getAll().then(function(res) {
            $scope.admins = res.data || [];
        }, function(err) {
            ToastService.error('Failed to load administrators');
        });
    };

    // =========================================================
    // ADMIN TABLE HELPERS & PAGINATION
    // =========================================================
    $scope.getFilteredAdmins = function() {
        return FilterHelperService.filterTable($scope.admins, $scope.adminFilters, null);
    };

    $scope.getPagedAdmins = function() {
        return FilterHelperService.filterTable($scope.admins, $scope.adminFilters, $scope.dt);
    };

    $scope.sortBy = function(field) {
        if ($scope.dt.sortField === field) {
            $scope.dt.sortReverse = !$scope.dt.sortReverse;
        } else {
            $scope.dt.sortField = field;
            $scope.dt.sortReverse = false;
        }
    };

    $scope.getSortIndicator = function(field) {
        if ($scope.dt.sortField !== field) return '';
        return $scope.dt.sortReverse ? '▼' : '▲';
    };

    $scope.getTotalPages = function(totalItems) {
        return Math.ceil((totalItems || 1) / $scope.dt.pageSize) || 1;
    };

    $scope.getPageNumbers = function(totalItems) {
        var total = $scope.getTotalPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    $scope.setPage = function(page) {
        if (page >= 1 && page <= $scope.getTotalPages($scope.getFilteredAdmins().length)) {
            $scope.dt.currentPage = page;
        }
    };

    /**
     * Toggle active/inactive status switch
     */
    $scope.toggleAdminStatus = function(admin, $event) {
        if ($event) $event.stopPropagation();
        var newStatus = admin.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        var updatePayload = {
            phoneNumber: admin.phoneNumber,
            superAdmin: admin.superAdmin,
            canCreateRoles: admin.canCreateRoles,
            canManageUsers: admin.canManageUsers,
            status: newStatus
        };
        AdminService.update(admin.id, updatePayload).then(function() {
            admin.status = newStatus;
            ToastService.success('Admin status updated');
        }, function() {
            ToastService.error('Failed to update status');
        });
    };

    // =========================================================
    // CREATE ADMIN ACTIONS
    // =========================================================
    $scope.createAdmin = function() {
        if (!$scope.newAdmin.username || !$scope.newAdmin.password) {
            ToastService.error('Please provide username and password');
            return;
        }

        AdminService.create($scope.newAdmin).then(function(res) {
            ToastService.success('Administrator created successfully!');
            $scope.newAdmin = { username: '', phoneNumber: '', password: '', superAdmin: false, canCreateRoles: true, canManageUsers: true, status: 'ACTIVE' };
            $scope.adminView = 'list';
            $scope.loadAdmins();
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create administrator');
        });
    };

    $scope.cancelCreate = function() {
        $scope.adminView = 'list';
    };

    // =========================================================
    // INSPECT ADMIN ACTIONS
    // =========================================================
    $scope.inspectAdmin = function(admin) {
        $scope.selectedAdmin = angular.copy(admin);
        $scope.adminView = 'inspect';
    };

    $scope.closeInspect = function() {
        $scope.selectedAdmin = null;
        $scope.adminView = 'list';
    };

    // =========================================================
    // DELETE ADMIN ACTIONS
    // =========================================================
    $scope.openDeleteAdminModal = function(admin, $event) {
        if ($event) $event.stopPropagation();
        $scope.adminToDelete = admin;
        $scope.selectedAdmin = admin;
        $scope.showDeleteAdminModal = true;
    };

    $scope.closeDeleteAdminModal = function() {
        $scope.showDeleteAdminModal = false;
        $scope.adminToDelete = null;
    };

    $scope.proceedToDeleteAdminPage = function() {
        $scope.showDeleteAdminModal = false;
        $scope.adminView = 'delete';
    };

    $scope.confirmDeleteAdmin = function(admin) {
        $scope.selectedAdmin = admin;
        $scope.adminView = 'delete';
    };

    $scope.deleteAdmin = function() {
        if (!$scope.selectedAdmin || !$scope.selectedAdmin.id) return;
        AdminService.delete($scope.selectedAdmin.id).then(function() {
            ToastService.success('Administrator deleted successfully');
            $scope.selectedAdmin = null;
            $scope.adminView = 'list';
            $scope.loadAdmins();
        }, function(err) {
            ToastService.error('Failed to delete administrator');
        });
    };

    $scope.cancelDelete = function() {
        $scope.selectedAdmin = null;
        $scope.adminView = 'list';
    };

    // Auto-reload when session is authenticated
    $scope.$on('auth:loggedIn', function() {
        $scope.loadAdmins();
    });

    // Auto-reload when tab becomes active if empty
    $scope.$on('tab:changed', function(event, tab) {
        if (tab === 'admins' && (!$scope.admins || $scope.admins.length === 0)) {
            $scope.loadAdmins();
        }
    });

    // Auto-load on init
    $scope.loadAdmins();
}])
.controller('AdminFormController', ['$scope', 'AdminService', 'ToastService', function($scope, AdminService, ToastService) {

    $scope.newAdmin = {
        username: '',
        phoneNumber: '',
        password: '',
        superAdmin: false,
        canCreateRoles: true,
        canManageUsers: true,
        status: 'ACTIVE'
    };

    $scope.isSubmitting = false;

    $scope.setSuperAdminMode = function(isSuper) {
        $scope.newAdmin.superAdmin = !!isSuper;
        if (isSuper) {
            $scope.newAdmin.canCreateRoles = true;
            $scope.newAdmin.canManageUsers = true;
        }
    };

    $scope.createAdmin = function() {
        if (!$scope.newAdmin.username || !$scope.newAdmin.password) {
            ToastService.error('Please provide username and password');
            return;
        }

        $scope.isSubmitting = true;
        AdminService.create($scope.newAdmin).then(function(res) {
            $scope.isSubmitting = false;
            ToastService.success('Administrator created successfully');
            $scope.resetForm();
            if ($scope.$parent && $scope.$parent.setAdminView) {
                $scope.$parent.setAdminView('list');
            }
            if ($scope.$parent && $scope.$parent.loadAdmins) {
                $scope.$parent.loadAdmins();
            }
            $scope.$broadcast('admin:created');
        }, function(err) {
            $scope.isSubmitting = false;
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create administrator');
        });
    };

    $scope.resetForm = function() {
        $scope.newAdmin = {
            username: '',
            phoneNumber: '',
            password: '',
            superAdmin: false,
            canCreateRoles: true,
            canManageUsers: true,
            status: 'ACTIVE'
        };
    };

    $scope.cancelCreate = function() {
        $scope.resetForm();
        if ($scope.$parent && $scope.$parent.setAdminView) {
            $scope.$parent.setAdminView('list');
        }
    };

}]);

/* =========================================================
   ADMIN UI DOM HELPERS & MODAL HANDLERS
   ========================================================= */
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

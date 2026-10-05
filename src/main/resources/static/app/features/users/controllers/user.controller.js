/**
 * Users & Permissions Feature Coordinator Controller
 * Manages User Directory (2.1-2.5), Multilingual User Creation, 3-Module CRUD Matrix,
 * Role Templates Directory, and Role Application.
 */
angular.module('tweklApp')
.controller('UserController', [
    '$scope', '$window', '$timeout',
    'UserService', 'RoleService',
    'ToastService', 'FilterHelperService',
function($scope, $window, $timeout,
         UserService, RoleService,
         ToastService, FilterHelperService) {

    // Main Sub-Tab: 'users' (Users Directory) | 'roles' (Role Templates)
    $scope.userSubTab = 'users';

    // User View states: 'list' | 'create' | 'inspect' | 'update' | 'delete'
    $scope.userView = 'create';
    $scope.users = [];
    $scope.roles = [];
    $scope.selectedUser = null;
    $scope.editingUser = null;
    $scope.deletingUser = null;

    // Standard Module Definitions for 3-Module Matrix
    var DEFAULT_MODULE_DEFINITIONS = [
        { moduleKey: 'software', moduleNameEn: 'Software Management', moduleNameAr: 'إدارة البرمجيات', moduleNameKu: 'بەڕێوەبردنی سۆفتوێر', descriptionEn: 'Access to system software, configurations, and technical tools.', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
        { moduleKey: 'sales', moduleNameEn: 'Sales & Invoices', moduleNameAr: 'المبيعات والفواتير', moduleNameKu: 'فرۆشتن و پسوولەکان', descriptionEn: 'Manage customer orders, invoices, discounts, and payments.', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
        { moduleKey: 'products', moduleNameEn: 'Product Management', moduleNameAr: 'إدارة المنتجات', moduleNameKu: 'بەڕێوەبردنی کاڵاکان', descriptionEn: 'Manage inventory items, pricing categories, and barcodes.', canCreate: false, canRead: true, canUpdate: false, canDelete: false }
    ];

    $scope.userModules = angular.copy(DEFAULT_MODULE_DEFINITIONS);
    $scope.createFormModules = angular.copy(DEFAULT_MODULE_DEFINITIONS);

    // Table pagination & sorting state (Users)
    $scope.dt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'usernameEn',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria (Users) - Defaults to all, or filtered by selected date/preset
    $scope.userFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
    };

    // Table pagination & sorting state (Roles)
    $scope.roleDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'name',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria (Roles)
    $scope.roleFilters = {
        search: ''
    };

    // Role Modals state
    $scope.showRoleModal = false;
    $scope.roleModalMode = 'create'; // 'create' | 'edit'
    $scope.editingRole = { name: '', canCreate: false, canRead: true, canUpdate: false, canDelete: false };

    // New user model defaults
    $scope.newUser = {
        usernameEn: '',
        usernameAr: '',
        usernameKu: '',
        password: '',
        phoneNumber: '',
        status: 'ACTIVE'
    };

    /**
     * Switch Sub-Tab ('users' vs 'roles')
     */
    $scope.setUserSubTab = function(tab) {
        $scope.userSubTab = tab;
        if (tab === 'users') {
            $scope.loadUsers();
        } else if (tab === 'roles') {
            $scope.loadRoles();
        }
    };

    /**
     * Switch user sub-view ('list', 'create', 'inspect', 'update', 'delete')
     */
    $scope.setUserView = function(view, user) {
        $scope.userView = view;
        if (view === 'inspect' && user) {
            $scope.selectedUser = angular.copy(user);
            $scope.syncUserModules(user);
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/users/' + user.id);
        } else if (view === 'update' && user) {
            $scope.editingUser = angular.copy(user);
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/users/' + user.id + '/edit');
        } else if (view === 'delete' && user) {
            $scope.selectedUser = user;
            $scope.deletingUser = user;
        } else if (view === 'create') {
            $scope.newUser = {
                usernameEn: '',
                usernameAr: '',
                usernameKu: '',
                password: '',
                phoneNumber: '',
                status: 'ACTIVE'
            };
            $scope.createFormModules = angular.copy(DEFAULT_MODULE_DEFINITIONS);
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/users/create');
        } else if (view === 'list') {
            $scope.selectedUser = null;
            $scope.editingUser = null;
            $scope.deletingUser = null;
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/users');
        }
    };

    /**
     * Sync user permissions to 3-module cards
     */
    $scope.syncUserModules = function(user) {
        $scope.userModules = angular.copy(DEFAULT_MODULE_DEFINITIONS);
        if (user && user.permissions && user.permissions.length) {
            $scope.userModules.forEach(function(mod) {
                var found = user.permissions.find(function(p) { return p.moduleKey === mod.moduleKey; });
                if (found) {
                    mod.canCreate = !!found.canCreate;
                    mod.canRead = !!found.canRead;
                    mod.canUpdate = !!found.canUpdate;
                    mod.canDelete = !!found.canDelete;
                }
            });
        }
    };

    /**
     * Loads all users from backend
     */
    $scope.loadUsers = function() {
        UserService.getAll().then(function(res) {
            $scope.users = res.data || [];
        }, function() {
            ToastService.error('Failed to load users');
        });
    };

    /**
     * Loads all role templates from backend
     */
    $scope.loadRoles = function() {
        RoleService.getAll().then(function(res) {
            $scope.roles = res.data || [];
        }, function() {
            ToastService.error('Failed to load role templates');
        });
    };

    // =========================================================
    // USERS TABLE HELPERS & PAGINATION
    // =========================================================
    $scope.getFilteredUsers = function() {
        return FilterHelperService.filterTable($scope.users, $scope.userFilters, null);
    };

    $scope.getPagedUsers = function() {
        return FilterHelperService.filterTable($scope.users, $scope.userFilters, $scope.dt);
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
        if (page >= 1 && page <= $scope.getTotalPages($scope.getFilteredUsers().length)) {
            $scope.dt.currentPage = page;
        }
    };

    /**
     * Toggle active/inactive user status
     */
    $scope.toggleUserStatus = function(user, $event) {
        if ($event) $event.stopPropagation();
        var newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        var updatePayload = {
            usernameEn: user.usernameEn,
            usernameAr: user.usernameAr,
            usernameKu: user.usernameKu,
            phoneNumber: user.phoneNumber,
            status: newStatus,
            permissions: user.permissions || []
        };
        UserService.update(user.id, updatePayload).then(function() {
            user.status = newStatus;
            ToastService.success('User status updated to ' + newStatus);
        }, function() {
            ToastService.error('Failed to update status');
        });
    };

    // =========================================================
    // ROLE TEMPLATES ACTIONS & PAGINATION
    // =========================================================
    $scope.getFilteredRoles = function() {
        return FilterHelperService.filterTable($scope.roles, $scope.roleFilters, null);
    };

    $scope.getPagedRoles = function() {
        return FilterHelperService.filterTable($scope.roles, $scope.roleFilters, $scope.roleDt);
    };

    $scope.sortRolesBy = function(field) {
        if ($scope.roleDt.sortField === field) {
            $scope.roleDt.sortReverse = !$scope.roleDt.sortReverse;
        } else {
            $scope.roleDt.sortField = field;
            $scope.roleDt.sortReverse = false;
        }
    };

    $scope.getRoleSortIndicator = function(field) {
        if ($scope.roleDt.sortField !== field) return '';
        return $scope.roleDt.sortReverse ? '▼' : '▲';
    };

    $scope.getTotalRolePages = function(totalItems) {
        return Math.ceil((totalItems || 1) / $scope.roleDt.pageSize) || 1;
    };

    $scope.getRolePageNumbers = function(totalItems) {
        var total = $scope.getTotalRolePages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    $scope.setRolePage = function(page) {
        if (page >= 1 && page <= $scope.getTotalRolePages($scope.getFilteredRoles().length)) {
            $scope.roleDt.currentPage = page;
        }
    };

    $scope.openCreateRoleModal = function() {
        $scope.roleModalMode = 'create';
        $scope.editingRole = { name: '', canCreate: false, canRead: true, canUpdate: false, canDelete: false };
        $scope.showRoleModal = true;
    };

    $scope.openEditRoleModal = function(role) {
        $scope.roleModalMode = 'edit';
        $scope.editingRole = angular.copy(role);
        $scope.showRoleModal = true;
    };

    $scope.closeRoleModal = function() {
        $scope.showRoleModal = false;
        $scope.editingRole = { name: '', canCreate: false, canRead: true, canUpdate: false, canDelete: false };
    };

    $scope.saveRoleModal = function() {
        if (!$scope.editingRole.name || !$scope.editingRole.name.trim()) {
            ToastService.error('Role name is required');
            return;
        }

        if ($scope.roleModalMode === 'create') {
            RoleService.create($scope.editingRole).then(function() {
                ToastService.success('Role template created');
                $scope.closeRoleModal();
                $scope.loadRoles();
            }, function(err) {
                ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create role');
            });
        } else {
            RoleService.update($scope.editingRole.id, $scope.editingRole).then(function() {
                ToastService.success('Role template updated');
                $scope.closeRoleModal();
                $scope.loadRoles();
            }, function(err) {
                ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to update role');
            });
        }
    };

    $scope.deleteRole = function(role) {
        if (!confirm('Are you sure you want to delete role template "' + role.name + '"?')) return;
        RoleService.delete(role.id).then(function() {
            ToastService.success('Role template deleted');
            $scope.loadRoles();
        }, function() {
            ToastService.error('Failed to delete role');
        });
    };

    $scope.toggleRoleField = function(role, field) {
        role[field] = !role[field];
        RoleService.update(role.id, role).then(function() {
            ToastService.success('Role permissions updated');
        }, function() {
            role[field] = !role[field];
            ToastService.error('Failed to update role permissions');
        });
    };

    // =========================================================
    // INSPECT USER & PERMISSIONS MATRIX ACTIONS
    // =========================================================
    $scope.applyRoleToInspectedUser = function(role) {
        if (!$scope.selectedUser) return;
        $scope.userModules.forEach(function(mod) {
            mod.canCreate = !!role.canCreate;
            mod.canRead = !!role.canRead;
            mod.canUpdate = !!role.canUpdate;
            mod.canDelete = !!role.canDelete;
        });
        $scope.saveUserModules();
        ToastService.success('Applied role template "' + role.name + '" to ' + $scope.selectedUser.usernameEn);
    };

    $scope.toggleModuleField = function(mod, field) {
        mod[field] = !mod[field];
        $scope.saveUserModules();
    };

    $scope.setFullCrud = function(mod) {
        mod.canCreate = true;
        mod.canRead = true;
        mod.canUpdate = true;
        mod.canDelete = true;
        $scope.saveUserModules();
    };

    $scope.setReadOnly = function(mod) {
        mod.canCreate = false;
        mod.canRead = true;
        mod.canUpdate = false;
        mod.canDelete = false;
        $scope.saveUserModules();
    };

    $scope.revokeModuleAccess = function(mod) {
        mod.canCreate = false;
        mod.canRead = false;
        mod.canUpdate = false;
        mod.canDelete = false;
        $scope.saveUserModules();
    };

    $scope.saveUserModules = function() {
        if (!$scope.selectedUser || !$scope.selectedUser.id) return;
        var payload = {
            usernameEn: $scope.selectedUser.usernameEn,
            usernameAr: $scope.selectedUser.usernameAr,
            usernameKu: $scope.selectedUser.usernameKu,
            phoneNumber: $scope.selectedUser.phoneNumber,
            status: $scope.selectedUser.status,
            permissions: $scope.userModules.map(function(m) {
                return {
                    moduleKey: m.moduleKey,
                    moduleNameEn: m.moduleNameEn,
                    moduleNameAr: m.moduleNameAr,
                    moduleNameKu: m.moduleNameKu,
                    canCreate: m.canCreate,
                    canRead: m.canRead,
                    canUpdate: m.canUpdate,
                    canDelete: m.canDelete
                };
            })
        };

        UserService.update($scope.selectedUser.id, payload).then(function(res) {
            var updated = res.data && res.data.data ? res.data.data : res.data;
            if (updated) {
                $scope.selectedUser.permissions = updated.permissions;
                var idx = $scope.users.findIndex(function(u) { return u.id === updated.id; });
                if (idx !== -1) $scope.users[idx] = updated;
            }
        });
    };

    // =========================================================
    // CREATE USER ACTIONS
    // =========================================================
    $scope.applyRoleToCreateForm = function(role) {
        if (!role) return;
        $scope.createFormModules.forEach(function(mod) {
            mod.canCreate = !!role.canCreate;
            mod.canRead = !!role.canRead;
            mod.canUpdate = !!role.canUpdate;
            mod.canDelete = !!role.canDelete;
        });
        ToastService.success('Applied role template "' + role.name + '" to permissions');
    };

    $scope.toggleCreateModuleField = function(mod, field) {
        mod[field] = !mod[field];
    };

    $scope.setCreateFormFullCrud = function(mod) {
        mod.canCreate = true;
        mod.canRead = true;
        mod.canUpdate = true;
        mod.canDelete = true;
    };

    $scope.setCreateFormReadOnly = function(mod) {
        mod.canCreate = false;
        mod.canRead = true;
        mod.canUpdate = false;
        mod.canDelete = false;
    };

    $scope.setCreateFormRevoke = function(mod) {
        mod.canCreate = false;
        mod.canRead = false;
        mod.canUpdate = false;
        mod.canDelete = false;
    };

    $scope.createUser = function() {
        if (!$scope.newUser.usernameEn || !$scope.newUser.password) {
            ToastService.error('Please provide at least English username and password');
            return;
        }

        $scope.newUser.permissions = $scope.createFormModules.map(function(m) {
            return {
                moduleKey: m.moduleKey,
                moduleNameEn: m.moduleNameEn,
                moduleNameAr: m.moduleNameAr,
                moduleNameKu: m.moduleNameKu,
                canCreate: m.canCreate,
                canRead: m.canRead,
                canUpdate: m.canUpdate,
                canDelete: m.canDelete
            };
        });

        UserService.create($scope.newUser).then(function() {
            ToastService.success('User created successfully!');
            $scope.userView = 'list';
            $scope.loadUsers();
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create user');
        });
    };

    // =========================================================
    // UPDATE USER ACTIONS
    // =========================================================
    $scope.updateUser = function() {
        if (!$scope.editingUser || !$scope.editingUser.id) return;
        UserService.update($scope.editingUser.id, $scope.editingUser).then(function() {
            ToastService.success('User updated successfully');
            $scope.editingUser = null;
            $scope.userView = 'list';
            $scope.loadUsers();
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to update user');
        });
    };

    // =========================================================
    // DELETE USER ACTIONS
    // =========================================================
    $scope.performDeleteUser = function(user) {
        if (!user || !user.id) return;
        UserService.delete(user.id).then(function() {
            ToastService.success('User deleted successfully');
            $scope.selectedUser = null;
            $scope.deletingUser = null;
            $scope.userView = 'list';
            $scope.loadUsers();
        }, function() {
            ToastService.error('Failed to delete user');
        });
    };

    $scope.getFilteredDeleteUsers = function() {
        return FilterHelperService.filterTable($scope.users, $scope.userFilters, null);
    };

    $scope.getPagedDeleteUsers = function() {
        return FilterHelperService.filterTable($scope.users, $scope.userFilters, $scope.dt);
    };

    // Auto-reload when session is authenticated
    $scope.$on('auth:loggedIn', function() {
        $scope.loadUsers();
        $scope.loadRoles();
    });

    // Auto-reload when tab becomes active if empty
    $scope.$on('tab:changed', function(event, tab) {
        if (tab === 'users' && (!$scope.users || $scope.users.length === 0)) {
            $scope.loadUsers();
            $scope.loadRoles();
        }
    });

    // Listen for preset change from LayoutController
    $scope.$on('filter:userTimePresetChanged', function(event, preset) {
        if (preset) {
            var range = FilterHelperService.getPresetDateRange(preset);
            $scope.userFilters.dateFrom = range.dateFrom;
            $scope.userFilters.dateTo = range.dateTo;
        } else {
            $scope.userFilters.dateFrom = null;
            $scope.userFilters.dateTo = null;
        }
        $scope.dt.currentPage = 1;
    });

    // Auto-load on init
    $scope.loadUsers();
    $scope.loadRoles();
}]);

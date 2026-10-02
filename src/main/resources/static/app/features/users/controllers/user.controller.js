/**
 * Users Feature Coordinator Controller
 * Manages User Directory, Multilingual User Creation, 3-Module CRUD Matrix, Updates, and Deletion.
 */
angular.module('tweklApp')
.controller('UserController', [
    '$scope', '$timeout', 'UserGetAllService', 'UserCreateService', 'UserUpdateService', 'UserDeleteService', 'ToastService', 'FilterHelperService',
function($scope, $timeout, UserGetAllService, UserCreateService, UserUpdateService, UserDeleteService, ToastService, FilterHelperService) {

    // View states: 'list' | 'create' | 'inspect' | 'update' | 'delete'
    $scope.userView = 'list';
    $scope.users = [];
    $scope.selectedUser = null;
    $scope.editingUser = null;
    $scope.deletingUser = null;

    // Table pagination & sorting state
    $scope.dt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'usernameEn',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria
    $scope.userFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
    };

    // New user model with 3-Module Matrix defaults
    $scope.newUser = {
        usernameEn: '',
        usernameAr: '',
        usernameKu: '',
        password: '',
        phoneNumber: '',
        status: 'ACTIVE',
        permissions: [
            { moduleKey: 'software', moduleNameEn: 'Software Management', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
            { moduleKey: 'sales', moduleNameEn: 'Sales & Invoices', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
            { moduleKey: 'products', moduleNameEn: 'Product Management', canCreate: false, canRead: true, canUpdate: false, canDelete: false }
        ]
    };

    /**
     * Switch user sub-view
     */
    $scope.setUserView = function(view, user) {
        $scope.userView = view;
        if (view === 'inspect' && user) {
            $scope.selectedUser = angular.copy(user);
        } else if (view === 'update' && user) {
            $scope.editingUser = angular.copy(user);
        } else if (view === 'delete' && user) {
            $scope.deletingUser = user;
        } else if (view === 'list') {
            $scope.selectedUser = null;
            $scope.editingUser = null;
            $scope.deletingUser = null;
        }
    };

    /**
     * Loads all users from backend
     */
    $scope.loadUsers = function() {
        UserGetAllService.execute().then(function(res) {
            $scope.users = res.data || [];
        }, function(err) {
            ToastService.error('Failed to load users');
        });
    };

    // =========================================================
    // USER TABLE HELPERS & PAGINATION
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
     * Toggle active/inactive status switch
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
        UserUpdateService.execute(user.id, updatePayload).then(function() {
            user.status = newStatus;
            ToastService.success('User status updated');
        }, function() {
            ToastService.error('Failed to update status');
        });
    };

    // =========================================================
    // 3-MODULE PERMISSION MATRIX PRESETS
    // =========================================================
    $scope.applyFullCrud = function(perm) {
        perm.canCreate = true;
        perm.canRead = true;
        perm.canUpdate = true;
        perm.canDelete = true;
    };

    $scope.applyReadOnly = function(perm) {
        perm.canCreate = false;
        perm.canRead = true;
        perm.canUpdate = false;
        perm.canDelete = false;
    };

    $scope.applyRevoke = function(perm) {
        perm.canCreate = false;
        perm.canRead = false;
        perm.canUpdate = false;
        perm.canDelete = false;
    };

    // =========================================================
    // CREATE USER ACTIONS
    // =========================================================
    $scope.createUser = function() {
        if (!$scope.newUser.usernameEn || !$scope.newUser.password) {
            ToastService.error('Please provide at least English username and password');
            return;
        }

        UserCreateService.execute($scope.newUser).then(function(res) {
            ToastService.success('User created successfully!');
            $scope.newUser = {
                usernameEn: '', usernameAr: '', usernameKu: '', password: '', phoneNumber: '', status: 'ACTIVE',
                permissions: [
                    { moduleKey: 'software', moduleNameEn: 'Software Management', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
                    { moduleKey: 'sales', moduleNameEn: 'Sales & Invoices', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
                    { moduleKey: 'products', moduleNameEn: 'Product Management', canCreate: false, canRead: true, canUpdate: false, canDelete: false }
                ]
            };
            $scope.userView = 'list';
            $scope.loadUsers();
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create user');
        });
    };

    $scope.cancelCreate = function() {
        $scope.userView = 'list';
    };

    // =========================================================
    // INSPECT USER ACTIONS
    // =========================================================
    $scope.inspectUser = function(user) {
        $scope.selectedUser = angular.copy(user);
        $scope.userView = 'inspect';
    };

    $scope.closeInspect = function() {
        $scope.selectedUser = null;
        $scope.userView = 'list';
    };

    // =========================================================
    // UPDATE USER ACTIONS
    // =========================================================
    $scope.editUser = function(user) {
        $scope.editingUser = angular.copy(user);
        $scope.userView = 'update';
    };

    $scope.updateUser = function() {
        if (!$scope.editingUser || !$scope.editingUser.id) return;
        UserUpdateService.execute($scope.editingUser.id, $scope.editingUser).then(function() {
            ToastService.success('User updated successfully');
            $scope.editingUser = null;
            $scope.userView = 'list';
            $scope.loadUsers();
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to update user');
        });
    };

    $scope.cancelUpdate = function() {
        $scope.editingUser = null;
        $scope.userView = 'list';
    };

    // =========================================================
    // DELETE USER ACTIONS
    // =========================================================
    $scope.confirmDeleteUser = function(user) {
        $scope.deletingUser = user;
        $scope.userView = 'delete';
    };

    $scope.deleteUser = function() {
        if (!$scope.deletingUser || !$scope.deletingUser.id) return;
        UserDeleteService.execute($scope.deletingUser.id).then(function() {
            ToastService.success('User deleted successfully');
            $scope.deletingUser = null;
            $scope.userView = 'list';
            $scope.loadUsers();
        }, function(err) {
            ToastService.error('Failed to delete user');
        });
    };

    $scope.cancelDelete = function() {
        $scope.deletingUser = null;
        $scope.userView = 'list';
    };

    // Auto-load on init
    $scope.loadUsers();
}]);

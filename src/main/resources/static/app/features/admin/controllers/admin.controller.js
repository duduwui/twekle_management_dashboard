/**
 * Admin Feature Coordinator Controller
 * Manages Admin Directory, Creation, Permissions, Status Toggling, and Deletion.
 */
angular.module('tweklApp')
.controller('AdminController', [
    '$scope', '$timeout', 'AdminGetAllService', 'AdminCreateService', 'AdminUpdateService', 'AdminDeleteService', 'ToastService', 'FilterHelperService',
function($scope, $timeout, AdminGetAllService, AdminCreateService, AdminUpdateService, AdminDeleteService, ToastService, FilterHelperService) {

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

    // Filter criteria
    $scope.adminFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
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
     * Switch admin view mode
     */
    $scope.setAdminView = function(view) {
        $scope.adminView = view;
    };

    /**
     * Loads all administrators from backend
     */
    $scope.loadAdmins = function() {
        AdminGetAllService.execute().then(function(res) {
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
        AdminUpdateService.execute(admin.id, updatePayload).then(function() {
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

        AdminCreateService.execute($scope.newAdmin).then(function(res) {
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
        AdminDeleteService.execute($scope.selectedAdmin.id).then(function() {
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

    // Auto-load on init
    $scope.loadAdmins();
}]);

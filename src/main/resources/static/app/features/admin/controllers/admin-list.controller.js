/**
 * Admin Action Controller: Admin List & DataTable
 * Handles fetching, search filtering, date range sorting, and pagination for administrators.
 */
angular.module('tweklApp')
.controller('AdminListController', ['$scope', 'AdminGetAllService', 'ToastService', 'FilterHelperService',
function($scope, AdminGetAllService, ToastService, FilterHelperService) {

    // Table pagination and sorting state
    $scope.adminDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria
    $scope.adminFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
    };

    /**
     * Loads full list of administrators from the backend
     */
    $scope.loadAdmins = function() {
        AdminGetAllService.execute().then(function(res) {
            $scope.admins = res.data || [];
        }, function(err) {
            ToastService.error('Failed to load administrators');
        });
    };

    /**
     * Returns filtered and paginated admins array
     */
    $scope.getFilteredAdmins = function() {
        return FilterHelperService.filterTable($scope.admins, $scope.adminFilters, $scope.adminDt);
    };

    // Auto-load on view init
    if (!$scope.admins || !$scope.admins.length) {
        $scope.loadAdmins();
    }
}]);

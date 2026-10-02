/**
 * User Action Controller: User List & DataTable
 * Handles listing, search filtering, and pagination for AppUsers.
 */
angular.module('tweklApp')
.controller('UserListController', ['$scope', 'UserGetAllService', 'ToastService', 'FilterHelperService',
function($scope, UserGetAllService, ToastService, FilterHelperService) {

    // Table pagination and sorting state
    $scope.userDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria
    $scope.userFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
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

    /**
     * Returns filtered and sorted user list
     */
    $scope.getFilteredUsers = function() {
        return FilterHelperService.filterTable($scope.users, $scope.userFilters, $scope.userDt);
    };

    // Auto-load on init
    if (!$scope.users || !$scope.users.length) {
        $scope.loadUsers();
    }
}]);

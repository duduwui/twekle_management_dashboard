/**
 * Follow-up Action Controller: Follow-up List & Customers Directory
 * Handles customer table, time filtering, search, and milestone badge clicks.
 */
angular.module('tweklApp')
.controller('FollowupListController', ['$scope', 'CustomerGetAllService', 'ToastService', 'FilterHelperService',
function($scope, CustomerGetAllService, ToastService, FilterHelperService) {

    // Table pagination and sorting
    $scope.customerDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria
    $scope.customerFilters = {
        search: '',
        dateFrom: null,
        dateTo: null,
        timePreset: null
    };

    /**
     * Loads all customers with current time preset filter
     */
    $scope.loadCustomers = function() {
        CustomerGetAllService.execute($scope.customerFilters.timePreset).then(function(res) {
            $scope.customers = res.data || [];
        }, function(err) {
            ToastService.error('Failed to load customers');
        });
    };

    /**
     * Filtered customers for DataTable
     */
    $scope.getFilteredCustomers = function() {
        return FilterHelperService.filterTable($scope.customers, $scope.customerFilters, $scope.customerDt);
    };

    // Auto-load on init
    if (!$scope.customers || !$scope.customers.length) {
        $scope.loadCustomers();
    }
}]);

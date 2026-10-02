/**
 * Report Feature Controller
 * Manages loading analytics, KPIs, and report breakdowns across time periods.
 */
angular.module('tweklApp')
.controller('ReportController', ['$scope', 'ReportGetService', 'ToastService',
function($scope, ReportGetService, ToastService) {

    $scope.activePeriod = 'monthly'; // 'daily' | 'weekly' | 'monthly' | 'all'
    $scope.reportData = null;
    $scope.loading = false;

    /**
     * Fetch report analytics data from backend
     */
    $scope.loadReport = function(period) {
        $scope.activePeriod = period || $scope.activePeriod;
        $scope.loading = true;

        ReportGetService.execute($scope.activePeriod).then(function(res) {
            $scope.reportData = res.data || {};
            $scope.loading = false;
        }, function(err) {
            $scope.loading = false;
            ToastService.error('Failed to load report analytics');
        });
    };

    // Auto-load on view init
    $scope.loadReport('monthly');
}]);

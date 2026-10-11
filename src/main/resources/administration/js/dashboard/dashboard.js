/**
 * Dashboard Overview Feature: Service & Controller Combined
 */
angular.module('tweklApp')
.factory('DashboardStatsService', ['$http', function($http) {
    return {
        getSummary: function() {
            return $http.get('/admin/api/stats/summary').catch(function() {
                return $http.get('/internal/stats/summary');
            });
        },
        getCustomers: function() {
            return $http.get('/admin/api/customers').catch(function() {
                return $http.get('/internal/customers');
            });
        },
        getSatisfactionDetails: function() {
            return $http.get('/admin/api/reports/satisfaction?period=ALL').catch(function() {
                return $http.get('/internal/reports/satisfaction?period=ALL');
            });
        },
        getReports: function() {
            return $http.get('/admin/api/reports?period=ALL').catch(function() {
                return $http.get('/internal/reports?period=ALL');
            });
        }
    };
}])
.controller('DashboardController', [
    '$scope', '$rootScope', '$interval', 'DashboardStatsService', 'ToastService',
function($scope, $rootScope, $interval, DashboardStatsService, ToastService) {

    $scope.stats = {
        totalCustomers: 0,
        customersToday: 0,
        customersThisWeek: 0,
        customersThisMonth: 0,
        totalOrders: 0,
        pendingFollowups: 0,
        completedFollowups: 0,
        totalFollowups: 0,
        alertFollowups: 0,
        idleFollowups: 0,
        doneFollowups: 0,
        unsatisfiedCount: 0,
        completionPercentage: 0,
        completedPercentage: 0,
        overduePercentage: 0,
        inQueuePercentage: 0,
        inQueueCount: 0
    };

    $scope.loading = false;
    $scope.urgentCustomers = [];
    $scope.allCustomers = [];
    $scope.unsatisfiedList = [];

    $scope.loadDashboardData = function() {
        $scope.loading = true;
        DashboardStatsService.getSummary().then(function(res) {
            var data = (res && res.data && res.data.data !== undefined) ? res.data.data : (res ? res.data : {});
            if (data) {
                angular.extend($scope.stats, data);
            }
        }).finally(function() {
            $scope.loading = false;
        });

        DashboardStatsService.getCustomers().then(function(res) {
            var list = (res && res.data && res.data.data !== undefined) ? res.data.data : (res ? res.data : []);
            $scope.allCustomers = list || [];
            $scope.urgentCustomers = ($scope.allCustomers || []).filter(function(c) {
                return !c.allFollowupsCompleted && (c.followupStatus === 'ALERT' || c.followupStatus === 'IDLE');
            });
        });

        DashboardStatsService.getSatisfactionDetails().then(function(res) {
            var list = (res && res.data && res.data.data !== undefined) ? res.data.data : (res ? res.data : []);
            $scope.unsatisfiedList = (list || []).filter(function(item) {
                return (item.rating && item.rating <= 2) || (item.satisfaction && item.satisfaction.toUpperCase() === 'UNSATISFIED');
            });
            $scope.stats.unsatisfiedCount = $scope.unsatisfiedList.length;
        });
    };

    $scope.filterAlert = function() {
        $rootScope.$broadcast('filter:followupStatusChanged', 'ALERT');
        if ($scope.setTab) $scope.setTab('customers');
    };

    $scope.filterPendingQueue = function() {
        $rootScope.$broadcast('filter:followupStatusChanged', 'IDLE');
        if ($scope.setTab) $scope.setTab('customers');
    };

    $scope.filterDone = function() {
        $rootScope.$broadcast('filter:followupStatusChanged', 'DONE');
        if ($scope.setTab) $scope.setTab('customers');
    };

    $scope.goToFollowups = function() {
        if ($scope.setTab) $scope.setTab('customers');
    };

    $scope.goToSatisfactionReports = function() {
        $scope.closeUnsatisfiedModal();
        if ($scope.setTab) $scope.setTab('reports');
        if ($scope.setReportSubTab) $scope.setReportSubTab('satisfaction');
    };

    $scope.showUnsatisfiedModal = false;
    $scope.openUnsatisfiedModal = function() {
        $scope.showUnsatisfiedModal = true;
    };
    $scope.closeUnsatisfiedModal = function() {
        $scope.showUnsatisfiedModal = false;
    };

    $scope.showProgressModal = false;
    $scope.openProgressModal = function() {
        $scope.showProgressModal = true;
    };
    $scope.closeProgressModal = function() {
        $scope.showProgressModal = false;
    };

    $scope.isOverdue = function(cust) {
        if (!cust) return false;
        return cust.followupStatus === 'ALERT' || (cust.nextPendingMinutesLeft !== null && cust.nextPendingMinutesLeft <= 0);
    };

    $scope.getLiveCountdown = function(cust) {
        if (!cust) return '-';
        var mins = cust.nextPendingMinutesLeft;
        if (mins === null || mins === undefined) return 'Scheduled';
        if (mins <= 0) {
            var over = Math.abs(mins);
            if (over < 60) return over + 'm overdue';
            var h = Math.floor(over / 60);
            var m = over % 60;
            return h + 'h ' + m + 'm overdue';
        }
        if (mins < 60) return mins + 'm left';
        var hours = Math.floor(mins / 60);
        var remMins = mins % 60;
        return hours + 'h ' + remMins + 'm left';
    };

    $scope.openCustomer = function(cust) {
        if (!cust) return;
        $rootScope.$broadcast('customer:openDetail', cust.id);
        if ($scope.setTab) $scope.setTab('customers');
    };

    // Hydrate from server-injected state
    if (window.SERVER_STATS) {
        angular.extend($scope.stats, window.SERVER_STATS);
    }
    if (window.SERVER_URGENT_CUSTOMERS) {
        $scope.urgentCustomers = window.SERVER_URGENT_CUSTOMERS;
    }
    if (window.SERVER_ALL_CUSTOMERS) {
        $scope.allCustomers = window.SERVER_ALL_CUSTOMERS;
    }

    $scope.loadDashboardData();

    $scope.$on('auth:loggedIn', function() {
        $scope.loadDashboardData();
    });
}]);

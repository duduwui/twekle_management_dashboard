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
                var total = $scope.stats.totalFollowups || (($scope.stats.completedFollowups || 0) + ($scope.stats.pendingFollowups || 0)) || 0;
                var completed = $scope.stats.completedFollowups || 0;
                var overdue = $scope.stats.overdueFollowups != null ? $scope.stats.overdueFollowups : ($scope.stats.alertFollowups || 0);
                var inQueue = $scope.stats.inQueueFollowups != null ? $scope.stats.inQueueFollowups : Math.max(0, total - completed - overdue);

                $scope.stats.completedPercentage = total > 0 ? Math.round((completed / total) * 100) : 0;
                $scope.stats.overduePercentage = total > 0 ? Math.round((overdue / total) * 100) : 0;
                $scope.stats.inQueuePercentage = total > 0 ? Math.max(0, 100 - $scope.stats.completedPercentage - $scope.stats.overduePercentage) : 0;
                $scope.stats.completionPercentage = $scope.stats.completedPercentage;
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
        if ($scope.setTab) {
            $rootScope.$broadcast('filter:followupStatusChanged', 'ALERT');
            $scope.setTab('customers');
        } else {
            window.location.href = '/admin/followups?status=ALERT';
        }
    };

    $scope.filterPendingQueue = function() {
        if ($scope.setTab) {
            $rootScope.$broadcast('filter:followupStatusChanged', 'IDLE');
            $scope.setTab('customers');
        } else {
            window.location.href = '/admin/followups?status=IDLE';
        }
    };

    $scope.filterDone = function() {
        if ($scope.setTab) {
            $rootScope.$broadcast('filter:followupStatusChanged', 'DONE');
            $scope.setTab('customers');
        } else {
            window.location.href = '/admin/followups?status=DONE';
        }
    };

    $scope.goToFollowups = function() {
        if ($scope.setTab) {
            $scope.setTab('customers');
        } else {
            window.location.href = '/admin/followups';
        }
    };

    $scope.goToSatisfactionReports = function() {
        $scope.closeUnsatisfiedModal();
        if ($scope.setTab) {
            $scope.setTab('reports');
            if ($scope.setReportSubTab) $scope.setReportSubTab('satisfaction');
        } else {
            window.location.href = '/admin/reports?tab=satisfaction';
        }
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
        if (mins >= 0) {
            if (mins < 60) return mins + 'm left';
            var hours = Math.floor(mins / 60);
            if (hours < 24) return hours + 'h ' + (mins % 60) + 'm left';
            var days = Math.floor(hours / 24);
            var remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h left') : (days + 'd left');
        } else {
            var overdue = Math.abs(mins);
            if (overdue < 60) return overdue + 'm overdue';
            var ovH = Math.floor(overdue / 60);
            if (ovH < 24) return ovH + 'h overdue';
            var ovD = Math.floor(ovH / 24);
            var ovRH = ovH % 24;
            return ovRH > 0 ? (ovD + 'd ' + ovRH + 'h overdue') : (ovD + 'd overdue');
        }
    };

    $scope.openCustomer = function(cust) {
        if (!cust) return;
        if ($scope.setTab) {
            $rootScope.$broadcast('customer:openDetail', cust.id);
            $scope.setTab('customers');
        } else {
            window.location.href = '/admin/followups/orders/' + cust.id;
        }
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

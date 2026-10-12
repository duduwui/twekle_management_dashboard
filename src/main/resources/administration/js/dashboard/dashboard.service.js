/**
 * Dashboard Stats Service
 * Retrieves KPI summaries, follow-up queues, and satisfaction metrics.
 */
angular.module('tweklApp')
.factory('DashboardStatsService', ['$http', function($http) {
    return {
        getSummary: function() {
            return $http.get('/admin/api/stats/summary');
        },
        getCustomers: function() {
            return $http.get('/admin/api/customers');
        },
        getSatisfactionDetails: function() {
            return $http.get('/admin/api/reports/satisfaction?period=ALL');
        },
        getReports: function() {
            return $http.get('/admin/api/reports?period=ALL');
        }
    };
}]);

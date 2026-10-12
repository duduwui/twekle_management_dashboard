/**
 * Dashboard Statistics Service
 * Retrieves KPI summaries, customer activity, and satisfaction details.
 */
angular.module('tweklApp')
.factory('DashboardStatsService', ['$http', function($http) {
    return {
        getSummary: function() {
            return $http.get('/admin/stats/summary');
        },
        getCustomers: function() {
            return $http.get('/admin/customers');
        },
        getSatisfactionDetails: function() {
            return $http.get('/admin/reports/satisfaction?period=ALL');
        },
        getReports: function() {
            return $http.get('/admin/reports?period=ALL');
        }
    };
}]);

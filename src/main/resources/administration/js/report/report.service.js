/**
 * Report Service
 * Manages dashboard analytics, turnaround times, and satisfaction reports.
 */
angular.module('tweklApp')
.factory('ReportService', ['$http', function($http) {
    var BASE = '/internal/reports';

    function get(period, params) {
        var config = { params: params || {} };
        if (period) config.params.period = period;
        return $http.get(BASE, config);
    }

    return {
        getReports: function(period, params) { return get(period, params); },
        get: function(period, params) { return get(period, params); }
    };
}]);

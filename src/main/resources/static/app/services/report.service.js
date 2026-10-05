/**
 * Report Service
 * Manages dashboard analytics, turnaround times, and satisfaction reports.
 * Endpoints: /internal/reports (fallback: /api/reports)
 */
angular.module('tweklApp')
.factory('ReportService', ['$http', function($http) {
    var BASE = '/internal/reports';
    var FALLBACK = '/api/reports';

    function get(period, params) {
        var config = { params: params || {} };
        if (period) config.params.period = period;

        return $http.get(BASE, config).catch(function() {
            return $http.get(FALLBACK, config);
        });
    }

    return {
        getReports: function(period, params) { return get(period, params); },
        get: function(period, params) { return get(period, params); }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('ReportGetService', ['ReportService', function(ReportService) {
    return { execute: function(period, params) { return ReportService.get(period, params); } };
}]);

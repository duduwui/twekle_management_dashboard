/**
 * Report Action Service: Get Reports
 * Fetches dashboard analytics and report summaries for a given period.
 * Endpoint: GET /internal/reports
 */
app.factory('ReportGetService', ['$http', function($http) {
    return {
        /**
         * @param {string} [period] - 'daily', 'weekly', 'monthly', 'all'
         * @param {Object} [params] - Additional filter query parameters
         * @returns {Promise} Resolves with analytics data
         */
        execute: function(period, params) {
            var config = { params: params || {} };
            if (period) config.params.period = period;
            return $http.get('/internal/reports', config).catch(function() {
                return $http.get('/api/reports', config);
            });
        }
    };
}]);

/**
 * Reporting & Analytics Internal Data Service
 */
app.factory('ReportService', ['$http', function($http) {
    return {
        get: function(period, params) {
            var config = { params: params || {} };
            if (period) config.params.period = period;
            return $http.get('/internal/reports', config).catch(function() {
                return $http.get('/api/reports', config);
            });
        }
    };
}]);

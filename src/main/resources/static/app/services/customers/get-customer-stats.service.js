/**
 * Customer Action Service: Get Customer Stats
 * Fetches aggregate dashboard metrics (total customers, total revenue, pending follow-ups).
 * Endpoint: GET /internal/customers/stats
 */
app.factory('CustomerGetStatsService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves with customer statistics object
         */
        execute: function() {
            return $http.get('/internal/customers/stats').catch(function() {
                return $http.get('/api/customers/stats');
            });
        }
    };
}]);

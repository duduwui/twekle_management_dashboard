/**
 * Customer Action Service: Get All Customers
 * Fetches all customers with optional time filtering parameters.
 * Endpoint: GET /internal/customers
 */
app.factory('CustomerGetAllService', ['$http', function($http) {
    return {
        /**
         * @param {string} [timeFilter] - Optional time filter preset key
         * @returns {Promise} Resolves with array of Customer objects
         */
        execute: function(timeFilter) {
            var params = timeFilter ? { timeFilter: timeFilter } : {};
            return $http.get('/internal/customers', { params: params }).catch(function() {
                return $http.get('/api/customers', { params: params });
            });
        }
    };
}]);

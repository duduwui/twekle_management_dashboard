/**
 * Customer Action Service: Get Customer By ID
 * Fetches single customer profile details.
 * Endpoint: GET /internal/customers/{id}
 */
app.factory('CustomerGetByIdService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Customer ID
         * @returns {Promise} Resolves with Customer object
         */
        execute: function(id) {
            return $http.get('/internal/customers/' + id).catch(function() {
                return $http.get('/api/customers/' + id);
            });
        }
    };
}]);

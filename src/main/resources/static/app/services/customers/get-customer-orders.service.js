/**
 * Customer Action Service: Get Customer Orders
 * Fetches all orders and products purchased by a specific customer.
 * Endpoint: GET /internal/customers/{id}/orders
 */
app.factory('CustomerGetOrdersService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} customerId - Customer ID
         * @returns {Promise} Resolves with array of CustomerOrder objects
         */
        execute: function(customerId) {
            return $http.get('/internal/customers/' + customerId + '/orders').catch(function() {
                return $http.get('/api/customers/' + customerId + '/orders');
            });
        }
    };
}]);

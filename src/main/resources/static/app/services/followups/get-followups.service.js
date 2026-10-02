/**
 * Follow-up Action Service: Get Follow-ups
 * Fetches all checkpoints and timeline records for a specific order.
 * Endpoint: GET /internal/orders/{orderId}/followups
 */
app.factory('FollowupGetService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} orderId - Customer Order ID
         * @returns {Promise} Resolves with array of OrderFollowupCheck objects
         */
        execute: function(orderId) {
            return $http.get('/internal/orders/' + orderId + '/followups').catch(function() {
                return $http.get('/api/orders/' + orderId + '/followups');
            });
        }
    };
}]);

/**
 * Follow-up Action Service: Create Follow-up
 * Adds a new milestone checkpoint to an order.
 * Endpoint: POST /internal/orders/{orderId}/followups
 */
app.factory('FollowupCreateService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} orderId - Customer Order ID
         * @param {Object} data - Followup checkpoint payload
         * @returns {Promise} Resolves with created checkpoint
         */
        execute: function(orderId, data) {
            return $http.post('/internal/orders/' + orderId + '/followups', data).catch(function() {
                return $http.post('/api/orders/' + orderId + '/followups', data);
            });
        }
    };
}]);

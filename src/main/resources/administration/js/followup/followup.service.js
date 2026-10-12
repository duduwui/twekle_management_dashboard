/**
 * Customer Service
 * Retrieves customers, their orders, and summary stats.
 */
angular.module('tweklApp')
.factory('CustomerService', ['$http', function($http) {
    var BASE = '/admin/followups';
    var CUSTOMERS = '/internal/customers';
    return {
        getAll: function(timeFilter) {
            return $http.get(CUSTOMERS, { params: timeFilter ? { timeFilter: timeFilter } : {} });
        },
        getById: function(id) {
            return $http.get(CUSTOMERS + '/' + id);
        },
        getOrders: function(customerId) {
            return $http.get(CUSTOMERS + '/' + customerId + '/orders');
        },
        getStats: function() {
            return $http.get(CUSTOMERS + '/stats');
        }
    };
}]);

/**
 * Follow-up Service
 * Manages follow-up checkpoints, milestone toggles, and note/photo updates.
 */
angular.module('tweklApp')
.factory('FollowupService', ['$http', function($http) {
    var BASE = '/admin/followups';
    var ORDERS = '/internal/orders';
    return {
        getFollowups: function(orderId) {
            return $http.get(ORDERS + '/' + orderId + '/followups');
        },
        updateFollowup: function(id, data) {
            return $http.put(BASE + '/' + id, data);
        },
        toggleStatus: function(id) {
            return $http.patch(BASE + '/' + id + '/status', {});
        }
    };
}]);

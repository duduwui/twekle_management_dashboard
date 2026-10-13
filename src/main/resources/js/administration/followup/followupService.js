/**
 * Follow-up Service
 * Unified service for customer follow-up inquiries, checkpoints, orders, and stats.
 */
angular.module('tweklApp')
.factory('FollowupService', ['$http', function($http) {
    var BASE = '/admin/followups';
    var CUSTOMERS = '/admin/customers';
    var ORDERS = '/admin/orders';

    var service = {
        // Customer directory & stats
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
        },

        // Follow-up checklists & milestone updates
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

    return service;
}])
.factory('CustomerService', ['FollowupService', function(FollowupService) {
    // Backward-compatible alias for existing controllers
    return FollowupService;
}]);

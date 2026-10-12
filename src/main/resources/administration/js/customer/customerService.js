/**
 * Customer Service
 * Retrieves customers, their orders, and summary stats.
 */
angular.module('tweklApp')
.factory('CustomerService', ['$http', function($http) {
    var BASE = '/admin/followups';
    var CUSTOMERS = '/admin/customers';
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

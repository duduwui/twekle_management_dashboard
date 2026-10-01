/**
 * Customer Lifecycle Internal Data Service
 */
app.factory('CustomerService', ['$http', function($http) {
    var BASE = '/internal/customers';
    var FALLBACK = '/api/customers';

    return {
        getAll: function(timeFilter) {
            var params = timeFilter ? { timeFilter: timeFilter } : {};
            return $http.get(BASE, { params: params }).catch(function() {
                return $http.get(FALLBACK, { params: params });
            });
        },
        getById: function(id) {
            return $http.get(BASE + '/' + id).catch(function() {
                return $http.get(FALLBACK + '/' + id);
            });
        },
        getOrders: function(customerId) {
            return $http.get(BASE + '/' + customerId + '/orders').catch(function() {
                return $http.get(FALLBACK + '/' + customerId + '/orders');
            });
        },
        getStats: function() {
            return $http.get(BASE + '/stats').catch(function() {
                return $http.get(FALLBACK + '/stats');
            });
        }
    };
}]);

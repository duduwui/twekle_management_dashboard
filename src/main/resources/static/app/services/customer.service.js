/**
 * Customer Service
 * Handles customer retrieval, stats, and related orders.
 * Endpoints: /internal/customers (fallback: /api/customers)
 */
angular.module('tweklApp')
.factory('CustomerService', ['$http', function($http) {
    var BASE = '/internal/customers';
    var FALLBACK = '/api/customers';

    function request(method, path, config) {
        config = config || {};
        config.method = method;
        config.url = BASE + (path || '');
        var fallbackUrl = FALLBACK + (path || '');

        return $http(config).catch(function() {
            var fallbackConfig = angular.copy(config);
            fallbackConfig.url = fallbackUrl;
            return $http(fallbackConfig);
        });
    }

    return {
        getAll: function(timeFilter) {
            var params = timeFilter ? { timeFilter: timeFilter } : {};
            return request('GET', '', { params: params });
        },
        getById: function(id) {
            return request('GET', '/' + id);
        },
        getOrders: function(customerId) {
            return request('GET', '/' + customerId + '/orders');
        },
        getStats: function() {
            return request('GET', '/stats');
        }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('CustomerGetAllService', ['CustomerService', function(CustomerService) {
    return { execute: function(timeFilter) { return CustomerService.getAll(timeFilter); } };
}])
.factory('CustomerGetByIdService', ['CustomerService', function(CustomerService) {
    return { execute: function(id) { return CustomerService.getById(id); } };
}])
.factory('CustomerGetOrdersService', ['CustomerService', function(CustomerService) {
    return { execute: function(customerId) { return CustomerService.getOrders(customerId); } };
}])
.factory('CustomerGetStatsService', ['CustomerService', function(CustomerService) {
    return { execute: function() { return CustomerService.getStats(); } };
}]);

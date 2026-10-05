/**
 * Time Filter Service
 * Manages dynamic time filter presets and status toggling.
 * Endpoints: /internal/time-filters (fallback: /api/time-filters)
 */
angular.module('tweklApp')
.factory('TimeFilterService', ['$http', function($http) {
    var BASE = '/internal/time-filters';
    var FALLBACK = '/api/time-filters';

    function request(method, path, data) {
        var url = BASE + (path || '');
        var fallbackUrl = FALLBACK + (path || '');
        var config = { method: method, url: url, data: data };
        return $http(config).catch(function() {
            return $http({ method: method, url: fallbackUrl, data: data });
        });
    }

    return {
        getAll: function() { return request('GET'); },
        create: function(data) { return request('POST', '', data); },
        toggle: function(id) { return request('PATCH', '/' + id + '/toggle'); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('TimeFilterGetAllService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function() { return TimeFilterService.getAll(); } };
}])
.factory('TimeFilterCreateService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function(data) { return TimeFilterService.create(data); } };
}])
.factory('TimeFilterToggleService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function(id) { return TimeFilterService.toggle(id); } };
}])
.factory('TimeFilterDeleteService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function(id) { return TimeFilterService.delete(id); } };
}]);

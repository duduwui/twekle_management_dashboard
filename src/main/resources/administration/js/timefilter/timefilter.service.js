/**
 * Time Filter Service
 * Manages dynamic time filter presets and status toggling.
 */
angular.module('tweklApp')
.factory('TimeFilterService', ['$http', function($http) {
    var BASE = '/internal/time-filters';

    function request(method, path, data) {
        return $http({ method: method, url: BASE + (path || ''), data: data });
    }

    return {
        getAll: function() { return request('GET'); },
        create: function(data) { return request('POST', '', data); },
        toggle: function(id) { return request('PATCH', '/' + id + '/toggle'); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}]);

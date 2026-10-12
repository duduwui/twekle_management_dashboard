/**
 * Admin Service
 * Handles administrator API operations with the backend.
 */
angular.module('tweklApp')
.factory('AdminService', ['$http', function($http) {
    var BASE = '/admin/admins';

    function request(method, path, data) {
        return $http({ method: method, url: BASE + (path || ''), data: data });
    }

    return {
        getAll: function() { return request('GET'); },
        getById: function(id) { return request('GET', '/' + id); },
        create: function(data) { return request('POST', '', data); },
        update: function(id, data) { return request('PUT', '/' + id, data); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}]);

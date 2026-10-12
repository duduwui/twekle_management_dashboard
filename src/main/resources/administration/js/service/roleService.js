/**
 * Role Service
 * Handles role template definitions, queries, creation, updates, and deletion.
 */
angular.module('tweklApp')
.factory('RoleService', ['$http', function($http) {
    var BASE = '/admin/roles';
    function request(method, path, data) {
        return $http({ method: method, url: BASE + (path || ''), data: data });
    }
    return {
        getAll: function() { return request('GET'); },
        getById: function(id) { return request('GET', '/' + id); },
        create: function(data) { return request('POST', '/create', data); },
        update: function(id, data) { return request('POST', '/' + id + '/update', data); },
        delete: function(id) { return request('POST', '/' + id + '/delete'); }
    };
}]);

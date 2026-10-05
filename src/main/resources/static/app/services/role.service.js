/**
 * Role Service
 * Handles role template definitions and permissions management.
 * Endpoints: /internal/roles (fallback: /api/roles)
 */
angular.module('tweklApp')
.factory('RoleService', ['$http', function($http) {
    var BASE = '/internal/roles';
    var FALLBACK = '/api/roles';

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
        getById: function(id) { return request('GET', '/' + id); },
        create: function(data) { return request('POST', '', data); },
        update: function(id, data) { return request('PUT', '/' + id, data); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('RoleGetAllService', ['RoleService', function(RoleService) {
    return { execute: function() { return RoleService.getAll(); } };
}])
.factory('RoleGetByIdService', ['RoleService', function(RoleService) {
    return { execute: function(id) { return RoleService.getById(id); } };
}])
.factory('RoleCreateService', ['RoleService', function(RoleService) {
    return { execute: function(data) { return RoleService.create(data); } };
}])
.factory('RoleUpdateService', ['RoleService', function(RoleService) {
    return { execute: function(id, data) { return RoleService.update(id, data); } };
}])
.factory('RoleDeleteService', ['RoleService', function(RoleService) {
    return { execute: function(id) { return RoleService.delete(id); } };
}]);

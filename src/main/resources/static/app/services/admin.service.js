/**
 * Admin Service
 * Handles all administrator API operations with the backend.
 * Endpoints: /internal/admins (fallback: /api/admins)
 */
angular.module('tweklApp')
.factory('AdminService', ['$http', function($http) {
    var BASE = '/internal/admins';
    var FALLBACK = '/api/admins';

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
.factory('AdminGetAllService', ['AdminService', function(AdminService) {
    return { execute: function() { return AdminService.getAll(); } };
}])
.factory('AdminGetByIdService', ['AdminService', function(AdminService) {
    return { execute: function(id) { return AdminService.getById(id); } };
}])
.factory('AdminCreateService', ['AdminService', function(AdminService) {
    return { execute: function(data) { return AdminService.create(data); } };
}])
.factory('AdminUpdateService', ['AdminService', function(AdminService) {
    return { execute: function(id, data) { return AdminService.update(id, data); } };
}])
.factory('AdminDeleteService', ['AdminService', function(AdminService) {
    return { execute: function(id) { return AdminService.delete(id); } };
}]);

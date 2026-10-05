/**
 * User Service
 * Handles user directory and profile management with the backend.
 * Endpoints: /internal/users (fallback: /api/users)
 */
angular.module('tweklApp')
.factory('UserService', ['$http', function($http) {
    var BASE = '/internal/users';
    var FALLBACK = '/api/users';

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
.factory('UserGetAllService', ['UserService', function(UserService) {
    return { execute: function() { return UserService.getAll(); } };
}])
.factory('UserGetByIdService', ['UserService', function(UserService) {
    return { execute: function(id) { return UserService.getById(id); } };
}])
.factory('UserCreateService', ['UserService', function(UserService) {
    return { execute: function(data) { return UserService.create(data); } };
}])
.factory('UserUpdateService', ['UserService', function(UserService) {
    return { execute: function(id, data) { return UserService.update(id, data); } };
}])
.factory('UserDeleteService', ['UserService', function(UserService) {
    return { execute: function(id) { return UserService.delete(id); } };
}]);

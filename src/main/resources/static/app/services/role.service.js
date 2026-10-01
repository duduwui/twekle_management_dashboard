/**
 * Role Template Internal Data Service
 */
app.factory('RoleService', ['$http', function($http) {
    var BASE = '/internal/roles';
    var FALLBACK = '/api/roles';

    function request(method, url, data) {
        return $http({ method: method, url: BASE + url, data: data }).catch(function() {
            return $http({ method: method, url: FALLBACK + url, data: data });
        });
    }

    return {
        getAll: function() { return request('GET', ''); },
        getById: function(id) { return request('GET', '/' + id); },
        create: function(data) { return request('POST', '', data); },
        update: function(id, data) { return request('PUT', '/' + id, data); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}]);

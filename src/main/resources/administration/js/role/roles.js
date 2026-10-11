/**
 * Role Service & Controller
 * Handles role template definitions and CRUD permissions management.
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
        create: function(data) { return request('POST', '/create-role', data); },
        update: function(id, data) { return request('POST', '/' + id + '/update-role', data); },
        delete: function(id) { return request('POST', '/' + id + '/delete-role'); }
    };
}])
.controller('RoleController', ['$scope', '$window', 'RoleService', 'ToastService',
function($scope, $window, RoleService, ToastService) {
    $scope.roles = $window.SERVER_ROLE_LIST || [];
    $scope.newRole = { canCreate: false, canRead: true, canUpdate: false, canDelete: false };
}]);

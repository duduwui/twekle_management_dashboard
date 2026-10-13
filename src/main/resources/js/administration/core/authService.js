/**
 * Auth Service
 * Manages user authentication, sessions, and current user profile retrieval.
 */
angular.module('tweklApp')
.factory('AuthService', ['$http', function($http) {
    var BASE = '/admin/auth';
    return {
        getMe: function() {
            return $http.get(BASE + '/me');
        },
        login: function(credentials) {
            return $http.post(BASE + '/login', credentials);
        },
        logout: function() {
            return $http.post(BASE + '/logout', {});
        }
    };
}]);


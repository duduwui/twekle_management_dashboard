/**
 * Auth Service
 * Manages user authentication, sessions, and current user profile retrieval.
 * Endpoints: /internal/auth/* (fallback: /api/auth/*)
 */
angular.module('tweklApp')
.factory('AuthService', ['$http', function($http) {
    return {
        getMe: function() {
            return $http.get('/internal/auth/me').catch(function() {
                return $http.get('/api/auth/me');
            });
        },
        login: function(credentials) {
            return $http.post('/internal/auth/login', credentials).catch(function() {
                return $http.post('/api/auth/login', credentials);
            });
        },
        logout: function() {
            return $http.post('/internal/auth/logout', {}).catch(function() {
                return $http.post('/api/auth/logout', {});
            });
        }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('AuthGetMeService', ['AuthService', function(AuthService) {
    return { execute: function() { return AuthService.getMe(); } };
}])
.factory('AuthLoginService', ['AuthService', function(AuthService) {
    return { execute: function(credentials) { return AuthService.login(credentials); } };
}])
.factory('AuthLogoutService', ['AuthService', function(AuthService) {
    return { execute: function() { return AuthService.logout(); } };
}]);

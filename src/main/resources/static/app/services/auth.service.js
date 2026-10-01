/**
 * Authentication & Session Management Service
 */
app.factory('AuthService', ['$http', function($http) {
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
}]);

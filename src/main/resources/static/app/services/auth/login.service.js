/**
 * Auth Action Service: Login
 * Authenticates user credentials and establishes an HTTP Session with JSESSIONID.
 * Endpoint: POST /internal/auth/login
 */
app.factory('AuthLoginService', ['$http', function($http) {
    return {
        /**
         * @param {Object} credentials - {username, password}
         * @returns {Promise} Resolves with session profile
         */
        execute: function(credentials) {
            return $http.post('/internal/auth/login', credentials).catch(function() {
                return $http.post('/api/auth/login', credentials);
            });
        }
    };
}]);

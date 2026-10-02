/**
 * Auth Action Service: Logout
 * Terminates the active HTTP Session and invalidates the JSESSIONID.
 * Endpoint: POST /internal/auth/logout
 */
app.factory('AuthLogoutService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves upon logout completion
         */
        execute: function() {
            return $http.post('/internal/auth/logout', {}).catch(function() {
                return $http.post('/api/auth/logout', {});
            });
        }
    };
}]);

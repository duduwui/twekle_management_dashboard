/**
 * Auth Action Service: Get Me
 * Retrieves the currently authenticated administrator/user session.
 * Endpoint: GET /internal/auth/me
 */
app.factory('AuthGetMeService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves with current user details
         */
        execute: function() {
            return $http.get('/internal/auth/me').catch(function() {
                return $http.get('/api/auth/me');
            });
        }
    };
}]);

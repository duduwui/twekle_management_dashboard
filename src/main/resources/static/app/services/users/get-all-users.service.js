/**
 * User Action Service: Get All Users
 * Fetches all AppUser accounts with multilingual names and permission matrix.
 * Endpoint: GET /internal/users
 */
app.factory('UserGetAllService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves with array of AppUser objects
         */
        execute: function() {
            return $http.get('/internal/users').catch(function() {
                return $http.get('/api/users');
            });
        }
    };
}]);

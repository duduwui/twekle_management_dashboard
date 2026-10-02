/**
 * User Action Service: Get User By ID
 * Fetches a single user and their 3-module CRUD permissions.
 * Endpoint: GET /internal/users/{id}
 */
app.factory('UserGetByIdService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - User ID
         * @returns {Promise} Resolves with User object
         */
        execute: function(id) {
            return $http.get('/internal/users/' + id).catch(function() {
                return $http.get('/api/users/' + id);
            });
        }
    };
}]);

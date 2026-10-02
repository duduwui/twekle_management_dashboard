/**
 * User Action Service: Delete User
 * Permanently removes a user account and associated permission records.
 * Endpoint: DELETE /internal/users/{id}
 */
app.factory('UserDeleteService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - User ID
         * @returns {Promise} Resolves on successful deletion
         */
        execute: function(id) {
            return $http.delete('/internal/users/' + id).catch(function() {
                return $http.delete('/api/users/' + id);
            });
        }
    };
}]);

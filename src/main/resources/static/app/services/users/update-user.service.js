/**
 * User Action Service: Update User
 * Modifies an existing AppUser's details or module permissions.
 * Endpoint: PUT /internal/users/{id}
 */
app.factory('UserUpdateService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - User ID
         * @param {Object} data - UpdateUserDto payload
         * @returns {Promise} Resolves with updated User object
         */
        execute: function(id, data) {
            return $http.put('/internal/users/' + id, data).catch(function() {
                return $http.put('/api/users/' + id, data);
            });
        }
    };
}]);

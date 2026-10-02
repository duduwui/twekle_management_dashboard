/**
 * Role Action Service: Update Role
 * Updates an existing role template's name or permissions.
 * Endpoint: PUT /internal/roles/{id}
 */
app.factory('RoleUpdateService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Role ID
         * @param {Object} data - Updated role fields
         * @returns {Promise} Resolves with updated Role
         */
        execute: function(id, data) {
            return $http.put('/internal/roles/' + id, data).catch(function() {
                return $http.put('/api/roles/' + id, data);
            });
        }
    };
}]);

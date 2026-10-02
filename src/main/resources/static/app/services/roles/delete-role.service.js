/**
 * Role Action Service: Delete Role
 * Deletes a role template.
 * Endpoint: DELETE /internal/roles/{id}
 */
app.factory('RoleDeleteService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Role ID
         * @returns {Promise} Resolves on successful deletion
         */
        execute: function(id) {
            return $http.delete('/internal/roles/' + id).catch(function() {
                return $http.delete('/api/roles/' + id);
            });
        }
    };
}]);

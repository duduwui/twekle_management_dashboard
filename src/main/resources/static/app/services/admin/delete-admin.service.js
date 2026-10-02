/**
 * Admin Action Service: Delete Admin
 * Permanently removes an administrator from the system.
 * Endpoint: DELETE /internal/admins/{id}
 */
app.factory('AdminDeleteService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Administrator ID
         * @returns {Promise} Resolves on successful deletion
         */
        execute: function(id) {
            return $http.delete('/internal/admins/' + id).catch(function() {
                return $http.delete('/api/admins/' + id);
            });
        }
    };
}]);

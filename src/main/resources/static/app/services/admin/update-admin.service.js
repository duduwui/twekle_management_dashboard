/**
 * Admin Action Service: Update Admin
 * Modifies an existing administrator's details, permissions, or status.
 * Endpoint: PUT /internal/admins/{id}
 */
app.factory('AdminUpdateService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Administrator ID
         * @param {Object} data - Updated admin fields
         * @returns {Promise} Resolves with updated Admin object
         */
        execute: function(id, data) {
            return $http.put('/internal/admins/' + id, data).catch(function() {
                return $http.put('/api/admins/' + id, data);
            });
        }
    };
}]);

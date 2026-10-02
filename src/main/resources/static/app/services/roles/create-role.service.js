/**
 * Role Action Service: Create Role
 * Creates a new role template with CRUD flags.
 * Endpoint: POST /internal/roles
 */
app.factory('RoleCreateService', ['$http', function($http) {
    return {
        /**
         * @param {Object} data - Role payload {name, canCreate, canRead, canUpdate, canDelete}
         * @returns {Promise} Resolves with created Role
         */
        execute: function(data) {
            return $http.post('/internal/roles', data).catch(function() {
                return $http.post('/api/roles', data);
            });
        }
    };
}]);

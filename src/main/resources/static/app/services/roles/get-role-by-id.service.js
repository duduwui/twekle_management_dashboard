/**
 * Role Action Service: Get Role By ID
 * Fetches a single role template.
 * Endpoint: GET /internal/roles/{id}
 */
app.factory('RoleGetByIdService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Role ID
         * @returns {Promise} Resolves with Role object
         */
        execute: function(id) {
            return $http.get('/internal/roles/' + id).catch(function() {
                return $http.get('/api/roles/' + id);
            });
        }
    };
}]);

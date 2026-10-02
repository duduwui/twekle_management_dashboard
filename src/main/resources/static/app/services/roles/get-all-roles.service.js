/**
 * Role Action Service: Get All Roles
 * Fetches all role template definitions.
 * Endpoint: GET /internal/roles
 */
app.factory('RoleGetAllService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves with array of Role objects
         */
        execute: function() {
            return $http.get('/internal/roles').catch(function() {
                return $http.get('/api/roles');
            });
        }
    };
}]);

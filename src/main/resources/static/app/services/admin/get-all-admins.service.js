/**
 * Admin Action Service: Get All Admins
 * Fetches complete list of administrators from the backend.
 * Endpoint: GET /internal/admins
 */
app.factory('AdminGetAllService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves with array of Admin objects
         */
        execute: function() {
            return $http.get('/internal/admins').catch(function() {
                return $http.get('/api/admins');
            });
        }
    };
}]);

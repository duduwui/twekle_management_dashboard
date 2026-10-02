/**
 * Admin Action Service: Get Admin By ID
 * Fetches a single administrator's profile by their unique ID.
 * Endpoint: GET /internal/admins/{id}
 */
app.factory('AdminGetByIdService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Administrator ID
         * @returns {Promise} Resolves with Admin object
         */
        execute: function(id) {
            return $http.get('/internal/admins/' + id).catch(function() {
                return $http.get('/api/admins/' + id);
            });
        }
    };
}]);

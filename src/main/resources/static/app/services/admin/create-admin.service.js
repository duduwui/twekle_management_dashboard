/**
 * Admin Action Service: Create Admin
 * Creates a new administrator account with username, password, phone, and permissions.
 * Endpoint: POST /internal/admins
 */
app.factory('AdminCreateService', ['$http', function($http) {
    return {
        /**
         * @param {Object} data - Admin payload {username, password, phoneNumber, isSuperAdmin, status}
         * @returns {Promise} Resolves with created Admin object
         */
        execute: function(data) {
            return $http.post('/internal/admins', data).catch(function() {
                return $http.post('/api/admins', data);
            });
        }
    };
}]);

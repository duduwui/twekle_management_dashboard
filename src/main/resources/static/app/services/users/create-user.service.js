/**
 * User Action Service: Create User
 * Creates an AppUser with multilingual names (EN, AR, KU) and 3-module permissions.
 * Endpoint: POST /internal/users
 */
app.factory('UserCreateService', ['$http', function($http) {
    return {
        /**
         * @param {Object} data - CreateUserDto payload
         * @returns {Promise} Resolves with created User object
         */
        execute: function(data) {
            return $http.post('/internal/users', data).catch(function() {
                return $http.post('/api/users', data);
            });
        }
    };
}]);

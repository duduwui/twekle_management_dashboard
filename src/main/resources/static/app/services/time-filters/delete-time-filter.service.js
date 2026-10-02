/**
 * Time Filter Action Service: Delete Time Filter
 * Deletes a dynamic time filter preset.
 * Endpoint: DELETE /internal/time-filters/{id}
 */
app.factory('TimeFilterDeleteService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Time Filter Preset ID
         * @returns {Promise} Resolves on successful deletion
         */
        execute: function(id) {
            return $http.delete('/internal/time-filters/' + id).catch(function() {
                return $http.delete('/api/time-filters/' + id);
            });
        }
    };
}]);

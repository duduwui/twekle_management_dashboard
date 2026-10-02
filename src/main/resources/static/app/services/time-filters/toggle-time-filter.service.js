/**
 * Time Filter Action Service: Toggle Time Filter
 * Toggles a preset between enabled and disabled status.
 * Endpoint: PATCH /internal/time-filters/{id}/toggle
 */
app.factory('TimeFilterToggleService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Time Filter Preset ID
         * @returns {Promise} Resolves with updated preset
         */
        execute: function(id) {
            return $http.patch('/internal/time-filters/' + id + '/toggle', {}).catch(function() {
                return $http.patch('/api/time-filters/' + id + '/toggle', {});
            });
        }
    };
}]);
